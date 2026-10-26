package com.advisely.call

/**
 * Adresse du serveur de signaling de la formation, fournie par le formateur.
 * Instance locale de secours (server/README.md) : l'adresse IP du poste formateur, par exemple
 * "http://192.168.1.20:8080". Jamais "localhost" : sur un téléphone, localhost désigne le téléphone.
 */
object SignalingConfig {
    const val BASE_URL = "https://signaling.example.com"

    /** Le WebSocket suit le schéma du handshake : https donne wss, http donne ws. */
    fun webSocketUrl(baseUrl: String): String = baseUrl.replaceFirst("http", "ws")
}
