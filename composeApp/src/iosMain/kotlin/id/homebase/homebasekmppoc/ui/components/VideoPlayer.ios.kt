package id.homebase.homebasekmppoc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerLayer
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoPlayer(
    videoData: ByteArray,
    modifier: Modifier
) {
    // Create a temporary file URL from the byte array
    val videoUrl = remember(videoData) {
        try {
            val tempDir = NSFileManager.defaultManager.temporaryDirectory
            val fileUrl = tempDir.URLByAppendingPathComponent("video_${System.currentTimeMillis()}.mp4")

            val nsData = NSData.create(
                bytes = videoData.toUByteArray().refTo(0),
                length = videoData.size.toULong()
            )
            nsData.writeToURL(fileUrl!!, atomically = true)
            fileUrl
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(videoUrl) {
        onDispose {
            videoUrl?.let { url ->
                try {
                    NSFileManager.defaultManager.removeItemAtURL(url, null)
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (videoUrl != null) {
            UIKitView(
                factory = {
                    val player = AVPlayer(uRL = videoUrl)
                    val playerLayer = AVPlayerLayer()
                    playerLayer.player = player

                    val view = UIView()
                    view.layer.addSublayer(playerLayer)

                    player.play()

                    view
                },
                modifier = Modifier.matchParentSize(),
                update = { view ->
                    CATransaction.begin()
                    CATransaction.setValue(true, kCATransactionDisableActions)
                    view.layer.sublayers?.firstOrNull()?.let { layer ->
                        layer.frame = view.layer.bounds
                    }
                    CATransaction.commit()
                }
            )
        }
    }
}
