package com.advisely.call

import com.shepeliev.webrtckmp.MediaStream

// Module 10 : l'ordre est aussi celui des erreurs courantes. Permission, puis capture, puis publication dans l'état.
suspend fun startLocalMedia(
    permissions: PermissionController,   // module 7
    camera: CameraController,            // module 7
): MediaStream? {
    if (!permissions.ensureCameraAndMic()) return null
    return camera.start()                // MediaStream : une piste vidéo, une piste audio
}
