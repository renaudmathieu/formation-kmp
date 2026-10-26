package com.advisely.call

import com.shepeliev.webrtckmp.IceCandidate
import com.shepeliev.webrtckmp.IceServer
import com.shepeliev.webrtckmp.MediaStream
import com.shepeliev.webrtckmp.OfferAnswerOptions
import com.shepeliev.webrtckmp.PeerConnection
import com.shepeliev.webrtckmp.RtcConfiguration
import com.shepeliev.webrtckmp.SessionDescription
import com.shepeliev.webrtckmp.SessionDescriptionType
import com.shepeliev.webrtckmp.VideoTrack
import com.shepeliev.webrtckmp.onIceCandidate
import com.shepeliev.webrtckmp.onTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/**
 * Module 12 : le moteur du module 11 généralisé à N pairs, pour la démo de l'appel à trois.
 * Même protocole : chaque présent reçoit le join du nouvel arrivant et lui envoie une offre ;
 * le nouvel arrivant répond à chaque offre. Les champs from et to font le routage.
 * Le mesh est pédagogique : chaque pair envoie N-1 flux. En production, on passe par un SFU.
 */
class MeshCallEngine(
    private val signaling: SignalingClient,
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<CallState>,
) {
    private lateinit var me: String
    private lateinit var localStream: MediaStream
    private lateinit var session: CallSession
    private val peerScopes = mutableMapOf<String, CoroutineScope>()               // collectes propres à chaque pair
    private val pendingCandidates = mutableMapOf<String, MutableList<IceCandidate>>()

    suspend fun start(room: String, localStream: MediaStream) {
        val join = signaling.join(room)
        me = join.participantId
        this.localStream = localStream
        session = CallSession(RtcConfiguration(iceServers = join.iceServers.map {
            IceServer(urls = it.urls, username = it.username.orEmpty(), password = it.credential.orEmpty())
        }))

        signaling.incoming(room)
            .filter { it.from != me && (it.to == null || it.to == me) }
            .onEach { msg ->
                msg.toCallEvent()?.let { event -> state.update { it.reduce(event) } }
                handleSignal(msg)
            }
            .catch { state.update { it.copy(status = Status.Error) } }
            .launchIn(scope)
    }

    fun stop() {
        if (!::session.isInitialized) return
        peerScopes.values.forEach { it.cancel() }
        peerScopes.clear()
        session.closeAll()
    }

    /** Une connexion par pair, avec ses pistes locales et ses collectes, posées avant toute description. */
    private fun connect(peerId: String): PeerConnection {
        session[peerId]?.let { return it }
        val pc = session.addParticipant(peerId)
        localStream.tracks.forEach { pc.addTrack(it, localStream) }

        val peerScope = CoroutineScope(scope.coroutineContext + Job(scope.coroutineContext[Job]))
        peerScopes[peerId] = peerScope
        pc.onIceCandidate
            .onEach { c ->
                signaling.send(SignalMessage(
                    type = "candidate", from = me, to = peerId,
                    candidate = c.candidate, sdpMid = c.sdpMid, sdpMLineIndex = c.sdpMLineIndex,
                ))
            }
            .launchIn(peerScope)
        pc.onTrack
            .mapNotNull { it.track as? VideoTrack }
            .onEach { track -> state.update { it.copy(remoteVideos = it.remoteVideos + (peerId to track), status = Status.InCall) } }
            .launchIn(peerScope)
        return pc
    }

    private fun disconnect(peerId: String) {
        peerScopes.remove(peerId)?.cancel()
        pendingCandidates.remove(peerId)
        session.removeParticipant(peerId)
        state.update { it.copy(remoteVideos = it.remoteVideos - peerId) }
    }

    private suspend fun handleSignal(msg: SignalMessage) {
        val peerId = msg.from
        when (msg.type) {
            "join" -> {                                  // un nouvel arrivant : chaque présent l'appelle
                val pc = connect(peerId)
                val offer = pc.createOffer(OfferAnswerOptions())
                pc.setLocalDescription(offer)
                signaling.send(SignalMessage(type = "offer", from = me, to = peerId, sdp = offer.sdp))
            }
            "offer" -> {
                val pc = connect(peerId)
                pc.setRemoteDescription(SessionDescription(SessionDescriptionType.Offer, msg.sdp!!))
                val answer = pc.createAnswer(OfferAnswerOptions())
                pc.setLocalDescription(answer)
                signaling.send(SignalMessage(type = "answer", from = me, to = peerId, sdp = answer.sdp))
                flushPendingCandidates(peerId, pc)
            }
            "answer" -> {
                val pc = session[peerId] ?: return
                pc.setRemoteDescription(SessionDescription(SessionDescriptionType.Answer, msg.sdp!!))
                flushPendingCandidates(peerId, pc)
            }
            "candidate" -> {
                val candidate = IceCandidate(msg.sdpMid!!, msg.sdpMLineIndex!!, msg.candidate!!)
                val pc = session[peerId]
                if (pc == null || pc.remoteDescription == null) pendingCandidates.getOrPut(peerId) { mutableListOf() } += candidate
                else pc.addIceCandidate(candidate)
            }
            "leave" -> disconnect(peerId)              // les autres connexions continuent
        }
    }

    private suspend fun flushPendingCandidates(peerId: String, pc: PeerConnection) {
        pendingCandidates.remove(peerId)?.forEach { pc.addIceCandidate(it) }
    }
}
