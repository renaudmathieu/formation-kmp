package com.advisely.call

// Types fournis en avance du module 8 (écran d'appel). Le module 9 les complète.

data class Participant(val id: String, val displayName: String = id)

data class CallState(
    val participants: List<Participant> = emptyList(),
    val micEnabled: Boolean = true,
)

sealed interface CallIntent {
    data object ToggleMic : CallIntent
    data object HangUp : CallIntent
}
