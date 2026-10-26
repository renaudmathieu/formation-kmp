package com.advisely.call

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ReducerTest {
    @Test
    fun ajouteUnParticipant() {
        val next = CallState().reduce(CallEvent.ParticipantJoined("alice"))
        assertEquals(1, next.participants.size)
    }

    @Test
    fun retireUnParticipant() {
        val state = CallState(participants = listOf(Participant("alice"), Participant("bob")))
        assertEquals(listOf("bob"), state.reduce(CallEvent.ParticipantLeft("alice")).participants.map { it.id })
    }

    @Test
    fun ignoreLeDepartDUnAbsent() {   // bonus de l'atelier
        val state = CallState(participants = listOf(Participant("alice")))
        assertEquals(state, state.reduce(CallEvent.ParticipantLeft("zoe")))
    }

    @Test
    fun uneErreurPasseLAppelEnErreur() {
        assertEquals(Status.Error, CallState().reduce(CallEvent.Error("socket fermée")).status)
    }

    @Test
    fun unEvenementSansEffetRenvoieLeMemeEtat() {
        val state = CallState()
        assertSame(state, state.reduce(CallEvent.RemoteTrackAdded("alice")))
    }

    @Test
    fun ajouteUnMessageDeChat() {   // bonus
        val next = CallState().reduce(CallEvent.ChatMessageReceived("alice", "bonjour"))
        assertEquals(listOf(ChatMessage("alice", "bonjour")), next.messages)
    }
}
