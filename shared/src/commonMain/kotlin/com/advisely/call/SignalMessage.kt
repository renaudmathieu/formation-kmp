package com.advisely.call

import kotlinx.serialization.Serializable

@Serializable
data class SignalMessage(
    val type: String,          // "join", "leave", "offer", "answer", "candidate"
    val from: String = "",     // participantId de l'émetteur (utilisé dès le module 4)
    val sdp: String? = null,
)
// Version de départ : le module 6 la complète (to, champs des candidats ICE).
// Le plugin génère le code de (dé)sérialisation à la compilation (classe SignalMessage$$serializer).

// Module 2 : K2 suit le flux de nullabilité jusqu'au bout
fun describe(msg: SignalMessage?) {
    if (msg?.sdp == null) return
    // ici msg et msg.sdp sont smart-cast en non-null, sans variable intermédiaire
    println(msg.sdp.length)
}
