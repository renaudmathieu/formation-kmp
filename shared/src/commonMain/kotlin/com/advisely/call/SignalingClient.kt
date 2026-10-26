package com.advisely.call

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.post
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Module 6 : l'API que consomment les modules 4, 9, 11 et 12.
class SignalingClient(
    private val client: HttpClient,
    private val baseUrl: String = SignalingConfig.BASE_URL,
) : SignalingSource {   // interface du module 4

    private var joined: JoinResponse? = null
    private var session: DefaultClientWebSocketSession? = null

    // Handshake REST : à appeler avant incoming()
    suspend fun join(room: String): JoinResponse =
        client.post("$baseUrl/rooms/$room/join").body<JoinResponse>()
            .also { joined = it }

    // Flux des messages entrants. Flux FROID : chaque collecte ouvre sa propre socket.
    override fun incoming(room: String): Flow<SignalMessage> = callbackFlow {
        val token = checkNotNull(joined) { "Appeler join() avant incoming()" }.token
        try {
            client.webSocket("${SignalingConfig.webSocketUrl(baseUrl)}/rooms/$room?token=$token") {
                session = this
                this.incoming.consumeAsFlow()
                    .filterIsInstance<Frame.Text>()
                    .collect { frame -> trySend(Json.decodeFromString<SignalMessage>(frame.readText())) }
            }
        } finally {
            session = null         // même si le collecteur est annulé pendant la session
        }
        close()                    // le serveur a fermé la socket : le flux se termine (retryWhen peut relancer)
        awaitClose()
    }

    // Atelier, étape 4 : appelé avant la collecte de incoming(), send() ne fait rien et ne dit rien.
    // Solutions possibles : voir NOTES.md.
    suspend fun send(message: SignalMessage) {
        session?.send(Frame.Text(Json.encodeToString(message)))
    }
}
