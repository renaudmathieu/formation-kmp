package com.advisely.call

import com.shepeliev.webrtckmp.MediaDevices
import com.shepeliev.webrtckmp.MediaStream

// Module 7, atelier 3 : la même chose en interface plus DI, pour comparer avec CameraController.
// (nom différent pour l'exercice : deux CameraController dans le même package ne compilent pas)
interface CameraCapture {
    suspend fun start(): MediaStream
    fun stop()
}

// Une seule implémentation commune suffit ; un faux s'injecte en test via Koin.
class WebRtcCameraCapture : CameraCapture {
    private var stream: MediaStream? = null

    override suspend fun start(): MediaStream =
        MediaDevices.getUserMedia(audio = true, video = true).also { stream = it }

    override fun stop() {
        stream?.tracks?.forEach { it.stop() }
        stream = null
    }
}
