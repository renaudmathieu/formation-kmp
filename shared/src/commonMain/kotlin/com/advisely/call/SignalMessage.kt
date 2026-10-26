package com.advisely.call

import kotlinx.serialization.Serializable

// Module 6 : le protocole complet. join et leave annoncent les participants (diffusés par le serveur),
// offer, answer et candidate portent la négociation WebRTC (module 11).
@Serializable
data class SignalMessage(
    val type: String,                // "join", "leave", "offer", "answer", "candidate"
    val from: String = "",           // participantId de l'émetteur, toujours renseigné (valeur par défaut : tolérance au décodage)
    val to: String? = null,          // destinataire ; null = toute la room (join, leave)
    val sdp: String? = null,         // offer et answer
    val candidate: String? = null,   // candidate : les trois champs d'un IceCandidate
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null,
)

// Module 2 : K2 suit le flux de nullabilité jusqu'au bout
fun describe(msg: SignalMessage?) {
    if (msg?.sdp == null) return
    // ici msg et msg.sdp sont smart-cast en non-null, sans variable intermédiaire
    println(msg.sdp.length)
}
