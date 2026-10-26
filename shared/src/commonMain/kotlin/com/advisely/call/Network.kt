package com.advisely.call

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable

// Module 6 : la construction du client est commune, le moteur (OkHttp, Darwin) est injecté par la plateforme.
fun buildHttpClient(engine: HttpClientEngine) = HttpClient(engine) {
    install(ContentNegotiation) { json() }
    install(WebSockets)
    install(Logging) { level = LogLevel.INFO }   // INFO trace l'URL : notre token y est. Jamais au-delà en production.
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 10_000
    }
}

@Serializable
data class IceServerDto(
    val urls: List<String>,
    val username: String? = null,     // obligatoire pour un serveur TURN
    val credential: String? = null,
)

@Serializable
data class JoinResponse(
    val token: String,
    val participantId: String,        // notre identifiant dans la room, utilisé dans from et to
    val iceServers: List<IceServerDto>,
)
