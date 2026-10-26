package com.advisely.call

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CallRepositoryTest {
    private val repo = CallRepository(FakeSignalingSource())

    @Test
    fun emetUnEvenementDeConnexion() = runTest {
        repo.events("room-1").test {
            assertIs<CallEvent.ParticipantJoined>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun reconnecteApresUneCoupure() = runTest {
        FlakySignalingSource().incomingWithRetry("room-1").test {
            // la première collecte échoue, retryWhen relance après 1 s de temps virtuel
            assertEquals("alice", awaitItem().from)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
