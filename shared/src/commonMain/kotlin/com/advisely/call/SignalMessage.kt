package com.advisely.call

// Version de départ. Au module 2, on l'annote @Serializable (après avoir ajouté le plugin de sérialisation) ;
// le module 6 la complète (to, champs des candidats ICE).
data class SignalMessage(
    val type: String,          // "join", "leave", "offer", "answer", "candidate"
    val from: String = "",     // participantId de l'émetteur (utilisé dès le module 4)
    val sdp: String? = null,
)
