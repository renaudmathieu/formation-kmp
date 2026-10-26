package com.advisely.call

import com.shepeliev.webrtckmp.PeerConnection
import com.shepeliev.webrtckmp.RtcConfiguration

// Module 12 : passer à plusieurs, c'est une PeerConnection par pair (mesh).
class CallSession(private val config: RtcConfiguration) {
    private val peers = mutableMapOf<String, PeerConnection>()   // une connexion par participantId

    operator fun get(id: String): PeerConnection? = peers[id]

    fun addParticipant(id: String): PeerConnection =
        PeerConnection(config).also { peers[id] = it }

    fun removeParticipant(id: String) {
        peers.remove(id)?.close()
    }

    fun closeAll() {
        peers.values.forEach { it.close() }
        peers.clear()
    }
}
