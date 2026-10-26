package com.advisely.call

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.shepeliev.webrtckmp.VideoTrack
import com.shepeliev.webrtckmp.WebRtc
import org.webrtc.SurfaceViewRenderer

@Composable
actual fun VideoRenderer(track: VideoTrack, modifier: Modifier, mirror: Boolean) {
    AndroidView(
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                init(WebRtc.rootEglBase.eglBaseContext, null)   // contexte EGL partagé par webrtc-kmp
                setMirror(mirror)                                // vue locale en miroir, comme une webcam
                track.addSink(this)
            }
        },
        modifier = modifier,
        onRelease = { view ->                  // la tuile disparaît : on débranche et on libère
            track.removeSink(view)
            view.release()
        },
    )
}
