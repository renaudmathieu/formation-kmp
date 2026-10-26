package com.advisely.call

import com.shepeliev.webrtckmp.MediaStream

// Module 7 : un contrat commun, une implémentation par plateforme
expect class CameraController() {
    suspend fun start(): MediaStream   // une piste vidéo et une piste audio
    fun stop()
}
