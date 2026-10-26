package com.advisely.call

// Module 9 : le réducteur, une fonction pure. Pas de coroutine, pas de plateforme, pas de réseau.
fun CallState.reduce(event: CallEvent): CallState = when (event) {
    is CallEvent.ParticipantJoined -> copy(participants = participants + Participant(event.id))
    is CallEvent.ParticipantLeft   -> copy(participants = participants.filterNot { it.id == event.id })
    is CallEvent.Error             -> copy(status = Status.Error)
    CallEvent.CallEnded            -> copy(status = Status.Ended)
    is CallEvent.ChatMessageReceived ->                        // bonus, avant la branche else
        copy(messages = messages + ChatMessage(event.from, event.text))
    else                           -> this
}
