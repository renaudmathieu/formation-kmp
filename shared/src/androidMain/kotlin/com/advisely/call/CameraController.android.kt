package com.advisely.call

import com.shepeliev.webrtckmp.MediaDevices
import com.shepeliev.webrtckmp.MediaStream

// Identique à l'actual iOS : quand la bibliothèque offre déjà une API commune, expect/actual ne sert à rien.
actual class CameraController actual constructor() {
    private var stream: MediaStream? = null

    actual suspend fun start(): MediaStream =
        MediaDevices.getUserMedia(audio = true, video = true)   // suspend, webrtc-kmp
            .also { stream = it }

    actual fun stop() {
        stream?.tracks?.forEach { it.stop() }   // libère caméra et micro
        stream = null
    }
}
