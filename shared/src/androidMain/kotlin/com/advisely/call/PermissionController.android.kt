package com.advisely.call

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// Android : il faut une Activity pour demander une permission
actual class PermissionController actual constructor() {
    companion object {
        // Raccourci de la formation (une seule Activity) : renseigné par MainActivity.onCreate,
        // remis à null dans onDestroy. En production : mécanisme lié au cycle de vie, ou moko-permissions.
        var activity: ComponentActivity? = null
    }

    actual suspend fun ensureCameraAndMic(): Boolean = suspendCancellableCoroutine { cont ->
        val activity = checkNotNull(activity) { "MainActivity n'a pas renseigné PermissionController.activity" }
        lateinit var launcher: ActivityResultLauncher<Array<String>>
        launcher = activity.activityResultRegistry.register(
            "camera-mic", ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            launcher.unregister()
            // carte vide = demande annulée ou Activity recréée : ce n'est pas un accord
            cont.resume(result.isNotEmpty() && result.values.all { it })
        }
        cont.invokeOnCancellation { launcher.unregister() }
        launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }
}
