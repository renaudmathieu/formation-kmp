package com.advisely.call

import com.shepeliev.webrtckmp.VideoTrack

// Module 9 : un état, des intentions, des effets.

enum class Status { Connecting, InCall, Error, Ended }

data class Participant(val id: String, val displayName: String = id)

data class CallState(
    val participants: List<Participant> = emptyList(),
    val micEnabled: Boolean = true,
    val status: Status = Status.Connecting,
    val localVideo: VideoTrack? = null,                      // rempli au module 10
    val remoteVideos: Map<String, VideoTrack> = emptyMap(),  // rempli au module 11, par participantId
    val messages: List<ChatMessage> = emptyList(),           // bonus : le chat
)

data class ChatMessage(val from: String, val text: String)

sealed interface CallIntent {
    data object ToggleMic : CallIntent
    data object HangUp : CallIntent
    data class SendChatMessage(val text: String) : CallIntent   // bonus
}

sealed interface CallEffect {           // événements ponctuels (non rejoués)
    data class ShowError(val message: String) : CallEffect
    data object NavigateBack : CallEffect
}
