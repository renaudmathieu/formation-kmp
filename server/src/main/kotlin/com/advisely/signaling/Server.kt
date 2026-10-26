package com.advisely.signaling

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// Protocole du module 6, à l'identique côté client (shared/.../SignalMessage.kt et JoinResponse).
@Serializable
data class IceServerDto(val urls: List<String>, val username: String? = null, val credential: String? = null)

@Serializable
data class JoinResponse(val token: String, val participantId: String, val iceServers: List<IceServerDto>)

@Serializable
data class SignalMessage(
    val type: String,
    val from: String = "",
    val to: String? = null,
    val sdp: String? = null,
    val candidate: String? = null,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null,
)

data class SignalingConfig(val iceServers: List<IceServerDto>) {
    companion object {
        /**
         * STUN public par défaut. TURN de secours (plan B réseau) par variables d'environnement :
         * TURN_URLS="turn:turn.example.com:3478?transport=udp,turns:turn.example.com:5349"
         * TURN_USERNAME=…  TURN_CREDENTIAL=…   (sans identifiants, TURN refuse la connexion)
         */
        fun fromEnv(): SignalingConfig {
            val stun = IceServerDto(urls = (System.getenv("STUN_URLS") ?: "stun:stun.l.google.com:19302").split(","))
            val turnUrls = System.getenv("TURN_URLS")?.split(",")?.filter { it.isNotBlank() }.orEmpty()
            val turn = if (turnUrls.isEmpty()) null else IceServerDto(
                urls = turnUrls,
                username = System.getenv("TURN_USERNAME"),
                credential = System.getenv("TURN_CREDENTIAL"),
            )
            return SignalingConfig(listOfNotNull(stun, turn))
        }
    }
}

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

private data class Ticket(val room: String, val participantId: String)

/** Les participants connectés, par room puis par participantId. */
private class Rooms {
    private val rooms = ConcurrentHashMap<String, ConcurrentHashMap<String, DefaultWebSocketServerSession>>()

    fun enter(room: String, id: String, session: DefaultWebSocketServerSession) {
        rooms.getOrPut(room) { ConcurrentHashMap() }[id] = session
    }

    fun exit(room: String, id: String, session: DefaultWebSocketServerSession) {
        rooms[room]?.remove(id, session)   // ne retire que cette session-là (une reconnexion a pu la remplacer)
    }

    suspend fun sendTo(room: String, id: String, message: SignalMessage) {
        val session = rooms[room]?.get(id) ?: return
        runCatching { session.send(Frame.Text(json.encodeToString(SignalMessage.serializer(), message))) }
    }

    suspend fun broadcast(room: String, except: String, message: SignalMessage) {
        rooms[room]?.keys?.filter { it != except }?.forEach { sendTo(room, it, message) }
    }
}

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    println("Serveur de signaling sur le port $port")
    embeddedServer(CIO, port = port, host = "0.0.0.0") { signalingModule() }.start(wait = true)
}

fun Application.signalingModule(config: SignalingConfig = SignalingConfig.fromEnv()) {
    install(ContentNegotiation) { json(json) }
    install(WebSockets)

    val tickets = ConcurrentHashMap<String, Ticket>()
    val rooms = Rooms()

    routing {
        get("/health") { call.respondText("ok") }

        // 1. Handshake REST : token, participantId, serveurs ICE
        post("/rooms/{room}/join") {
            val room = call.parameters["room"]!!
            val participantId = "p-" + UUID.randomUUID().toString().take(8)
            val token = UUID.randomUUID().toString()
            tickets[token] = Ticket(room, participantId)
            call.respond(HttpStatusCode.OK, JoinResponse(token, participantId, config.iceServers))
        }

        // 2. Canal temps réel. join diffusé aux autres à l'ouverture, leave à la fermeture,
        //    routage par `to`, `from` toujours renseigné par le serveur.
        webSocket("/rooms/{room}") {
            val room = call.parameters["room"]!!
            val ticket = call.request.queryParameters["token"]?.let { tickets[it] }
            if (ticket == null || ticket.room != room) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "token absent ou invalide"))
                return@webSocket
            }
            val me = ticket.participantId
            rooms.enter(room, me, this)
            try {
                rooms.broadcast(room, except = me, SignalMessage(type = "join", from = me))
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val message = runCatching { json.decodeFromString(SignalMessage.serializer(), frame.readText()) }
                        .getOrNull() ?: continue
                    val outgoing = message.copy(from = me)
                    val to = outgoing.to
                    if (to != null) rooms.sendTo(room, to, outgoing) else rooms.broadcast(room, except = me, outgoing)
                }
            } finally {
                rooms.exit(room, me, this)
                rooms.broadcast(room, except = me, SignalMessage(type = "leave", from = me))
            }
        }
    }
}
