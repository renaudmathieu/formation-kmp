package com.advisely.call

import WebRTC.RTCMTLVideoView                       // le SDK WebRTC iOS (package SPM WebRTC)
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.shepeliev.webrtckmp.VideoTrack
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.UIKit.UIViewContentMode

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoRenderer(track: VideoTrack, modifier: Modifier, mirror: Boolean) {
    UIKitView(
        factory = {
            val view = RTCMTLVideoView()
            view.videoContentMode = UIViewContentMode.UIViewContentModeScaleAspectFill
            if (mirror) view.transform = CGAffineTransformMakeScale(-1.0, 1.0)   // miroir horizontal
            track.addRenderer(view)
            view
        },
        modifier = modifier,
        onRelease = { view -> track.removeRenderer(view) },
    )
}
