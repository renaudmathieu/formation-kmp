package com.advisely.call

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.mapNotNull

// La source des messages : un faux ici (FakeSignalingSource, AtelierCoroutines.kt),
// le vrai SignalingClient au module 6, qui implémentera cette interface.
interface SignalingSource {
    fun incoming(room: String): Flow<SignalMessage>
}

fun SignalMessage.toCallEvent(): CallEvent? = when (type) {
    "join"  -> CallEvent.ParticipantJoined(from)
    "leave" -> CallEvent.ParticipantLeft(from)
    else    -> null                 // offer, answer, candidate : traités au module 11
}

class CallRepository(private val signaling: SignalingSource) {
    fun events(room: String): Flow<CallEvent> =
        signaling.incoming(room)
            .mapNotNull { it.toCallEvent() }   // ne garde que les messages qui ont un sens métier
            .catch { e -> emit(CallEvent.Error(e.message)) }

    suspend fun hangUp() { /* ferme la connexion et libère les pistes : repris par CallEngine au module 11 */ }
}
