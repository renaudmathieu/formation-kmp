package com.advisely.call

import com.shepeliev.webrtckmp.DataChannel
import com.shepeliev.webrtckmp.IceCandidate
import com.shepeliev.webrtckmp.IceServer
import com.shepeliev.webrtckmp.MediaStream
import com.shepeliev.webrtckmp.OfferAnswerOptions
import com.shepeliev.webrtckmp.PeerConnection
import com.shepeliev.webrtckmp.RtcConfiguration
import com.shepeliev.webrtckmp.SessionDescription
import com.shepeliev.webrtckmp.SessionDescriptionType
import com.shepeliev.webrtckmp.VideoTrack
import com.shepeliev.webrtckmp.onDataChannel
import com.shepeliev.webrtckmp.onIceCandidate
import com.shepeliev.webrtckmp.onTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

// Module 11 : le moteur d'appel, une classe qui porte la connexion (appel à deux)
class CallEngine(
    private val signaling: SignalingClient,          // module 6
    private val scope: CoroutineScope,               // viewModelScope du CallViewModel
    private val state: MutableStateFlow<CallState>,  // l'état du module 9
) {
    private lateinit var me: String
    private var peerId: String? = null               // le pair en face (appel à deux)
    private lateinit var peerConnection: PeerConnection
    private val pendingCandidates = mutableListOf<IceCandidate>()
    private var chat: DataChannel? = null   // bonus : le data channel du chat

    suspend fun start(room: String, localStream: MediaStream) {
        // 1. Handshake : token, participantId, serveurs ICE (module 6)
        val join = signaling.join(room)
        me = join.participantId
        peerConnection = PeerConnection(RtcConfiguration(iceServers = join.iceServers.map {
            IceServer(urls = it.urls, username = it.username.orEmpty(), password = it.credential.orEmpty())
        }))

        // 2. Pistes locales (vidéo ET audio) AVANT toute offre ou réponse,
        //    sinon le média ne part que dans un sens
        localStream.tracks.forEach { peerConnection.addTrack(it, localStream) }

        // 3. Collectes AVANT toute description : ces Flow ne rejouent pas les événements passés
        peerConnection.onIceCandidate
            .onEach { c ->
                val to = peerId ?: return@onEach
                signaling.send(SignalMessage(
                    type = "candidate", from = me, to = to,
                    candidate = c.candidate, sdpMid = c.sdpMid, sdpMLineIndex = c.sdpMLineIndex,
                ))
            }
            .launchIn(scope)

        peerConnection.onTrack
            .mapNotNull { event -> event.track as? VideoTrack }   // track est nullable et générique
            .onEach { track ->
                val from = peerId ?: return@onEach
                state.update { it.copy(remoteVideos = it.remoteVideos + (from to track), status = Status.InCall) }
            }
            .launchIn(scope)

        // Bonus, côté appelé : le canal arrive par onDataChannel, avec les autres collectes, avant toute description
        peerConnection.onDataChannel.onEach { listenChat(it) }.launchIn(scope)

        // 4. Une seule collecte du signaling, ici et nulle part ailleurs :
        //    incoming() est froid, une seconde collecte ouvrirait une seconde socket.
        //    Le moteur nourrit aussi le réducteur du module 9 avec join et leave.
        signaling.incoming(room)
            .filter { it.from != me && (it.to == null || it.to == me) }   // ni nos échos, ni les messages d'un autre pair
            .onEach { msg ->
                msg.toCallEvent()?.let { event -> state.update { it.reduce(event) } }   // module 4 et module 9
                handleSignal(msg)
            }
            .catch { state.update { it.copy(status = Status.Error) } }   // socket tombée : pas de plantage
            .launchIn(scope)
    }

    fun stop() {
        if (::peerConnection.isInitialized) peerConnection.close()   // stop() avant start() : rien à fermer
    }

    // Qui appelle ? Le présent, à réception du join du nouvel arrivant. Le nouvel arrivant attend l'offre.
    private suspend fun startCall() {
        // Bonus, côté appelant : le canal se crée avant createOffer (DataChannel? : null si la création échoue)
        peerConnection.createDataChannel(label = "chat")?.let { listenChat(it) }
        val offer = peerConnection.createOffer(OfferAnswerOptions())
        peerConnection.setLocalDescription(offer)
        signaling.send(SignalMessage(type = "offer", from = me, to = peerId, sdp = offer.sdp))
    }

    private suspend fun handleSignal(msg: SignalMessage) {
        when (msg.type) {
            "join" -> {                          // un nouvel arrivant : le présent appelle
                peerId = msg.from
                startCall()
            }
            "offer" -> {
                peerId = msg.from                // l'appelé apprend qui l'appelle
                peerConnection.setRemoteDescription(SessionDescription(SessionDescriptionType.Offer, msg.sdp!!))
                val answer = peerConnection.createAnswer(OfferAnswerOptions())
                peerConnection.setLocalDescription(answer)
                signaling.send(SignalMessage(type = "answer", from = me, to = msg.from, sdp = answer.sdp))
                flushPendingCandidates()
            }
            "answer" -> {
                peerConnection.setRemoteDescription(SessionDescription(SessionDescriptionType.Answer, msg.sdp!!))
                flushPendingCandidates()
            }
            "candidate" -> {
                val candidate = IceCandidate(msg.sdpMid!!, msg.sdpMLineIndex!!, msg.candidate!!)
                // un candidat peut arriver avant la description distante : on le met en file
                if (peerConnection.remoteDescription == null) pendingCandidates += candidate
                else peerConnection.addIceCandidate(candidate)
            }
            "leave" -> {
                state.update { it.copy(remoteVideos = it.remoteVideos - msg.from) }
                peerConnection.close()           // appel à deux : on raccroche
            }
        }
    }

    // Bonus : le data channel, troisième type de flux, sur la même connexion négociée
    private fun listenChat(channel: DataChannel) {
        chat = channel
        channel.onMessage                                  // Flow<ByteArray>
            .map { it.decodeToString() }
            .onEach { text ->
                val from = peerId ?: return@onEach
                state.update { it.reduce(CallEvent.ChatMessageReceived(from, text)) }
            }
            .launchIn(scope)
    }

    // Envoi, appelé par le ViewModel ; l'émetteur voit aussi son propre message
    fun sendChat(text: String) {
        if (!::me.isInitialized) return                    // pas encore dans l'appel
        chat?.send(text.encodeToByteArray())
        state.update { it.reduce(CallEvent.ChatMessageReceived(me, text)) }
    }

    private suspend fun flushPendingCandidates() {
        pendingCandidates.forEach { peerConnection.addIceCandidate(it) }
        pendingCandidates.clear()
    }
}
