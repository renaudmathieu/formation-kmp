// Serveur de signaling de la formation (côté formateur) : handshake REST puis WebSocket, protocole du module 6.
// Lancement local : ./gradlew :server:run   (port 8080 par défaut, voir server/README.md)
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("com.advisely.signaling.ServerKt")
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
}

tasks.test { useJUnitPlatform() }
