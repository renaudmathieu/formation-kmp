package com.advisely.signaling

import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.post
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets

class ServerTest {
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun DefaultClientWebSocketSession.receiveMessage(): SignalMessage = withTimeout(5_000) {
        val frame = incoming.receive() as Frame.Text
        json.decodeFromString(SignalMessage.serializer(), frame.readText())
    }

    private suspend fun DefaultClientWebSocketSession.sendMessage(message: SignalMessage) =
        send(Frame.Text(json.encodeToString(SignalMessage.serializer(), message)))

    @Test
    fun handshakeRenvoieTokenParticipantEtServeursIce() = testApplication {
        application { signalingModule(SignalingConfig(listOf(IceServerDto(listOf("stun:stun.example.com"))))) }
        val client = createClient { install(ClientContentNegotiation) { json() } }
        val join = client.post("/rooms/r1/join").body<JoinResponse>()
        assertEquals(listOf("stun:stun.example.com"), join.iceServers.single().urls)
        assert(join.participantId.isNotBlank() && join.token.isNotBlank())
    }

    @Test
    fun joinDiffuseRoutageParToEtLeaveALaFermeture() = testApplication {
        application { signalingModule(SignalingConfig(emptyList())) }
        val client = createClient {
            install(ClientContentNegotiation) { json() }
            install(ClientWebSockets)
        }
        val alice = client.post("/rooms/r1/join").body<JoinResponse>()
        val bob = client.post("/rooms/r1/join").body<JoinResponse>()

        client.webSocket("/rooms/r1?token=${alice.token}") {
            val bobSession = launch {
                client.webSocket("/rooms/r1?token=${bob.token}") {
                    // Bob, nouvel arrivant, ne reçoit pas de join : il attend l'offre d'Alice
                    val offer = receiveMessage()
                    assertEquals("offer", offer.type)
                    assertEquals(alice.participantId, offer.from)   // from imposé par le serveur
                    assertEquals("v=0", offer.sdp)
                }
            }
            // Alice, déjà présente, reçoit le join de Bob et lui envoie une offre ciblée
            val join = receiveMessage()
            assertEquals("join", join.type)
            assertEquals(bob.participantId, join.from)
            assertNull(join.to)
            sendMessage(SignalMessage(type = "offer", from = "usurpation", to = bob.participantId, sdp = "v=0"))

            bobSession.join()                    // Bob ferme sa socket
            val leave = receiveMessage()
            assertEquals("leave", leave.type)
            assertEquals(bob.participantId, leave.from)
        }
    }
}
