package com.advisely.call

// Commencé au module 3 (deux cas), complété au module 4.
sealed interface CallEvent {
    data class ParticipantJoined(val id: String) : CallEvent
    data class ParticipantLeft(val id: String) : CallEvent
    data class RemoteTrackAdded(val participantId: String) : CallEvent
    data object CallEnded : CallEvent
    data class Error(val message: String?) : CallEvent
    data class ChatMessageReceived(val from: String, val text: String) : CallEvent   // bonus
}
