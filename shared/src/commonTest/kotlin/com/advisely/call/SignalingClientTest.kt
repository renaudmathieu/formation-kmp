package com.advisely.call

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SignalingClientTest {
    @Test
    fun decodeLeHandshake() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"token":"t","participantId":"alice","iceServers":[{"urls":["stun:stun.example.com"]}]}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),   // sans lui, body() échoue
            )
        }
        val join = SignalingClient(buildHttpClient(engine)).join("room-1")
        assertEquals("alice", join.participantId)
    }

    @Test
    fun decodeUnMessageOffer() {
        // MockEngine ne sait pas simuler une session WebSocket : le décodage d'un message se teste directement
        val msg = Json.decodeFromString<SignalMessage>("""{"type":"offer","from":"alice","to":"bob","sdp":"v=0..."}""")
        assertEquals("bob", msg.to)
    }
}
