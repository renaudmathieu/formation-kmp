package com.advisely.call

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaType
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.requestAccessForMediaType
import kotlin.coroutines.resume

// iOS : les clés de l'Info.plist ne demandent rien, c'est AVFoundation qui pose la question
actual class PermissionController actual constructor() {
    actual suspend fun ensureCameraAndMic(): Boolean =
        request(AVMediaTypeVideo) && request(AVMediaTypeAudio)

    // Les constantes AVMediaType* arrivent nullables depuis Objective-C : d'où le paramètre nullable.
    private suspend fun request(type: AVMediaType?): Boolean = suspendCancellableCoroutine { cont ->
        AVCaptureDevice.requestAccessForMediaType(checkNotNull(type)) { granted -> cont.resume(granted) }
    }
}
