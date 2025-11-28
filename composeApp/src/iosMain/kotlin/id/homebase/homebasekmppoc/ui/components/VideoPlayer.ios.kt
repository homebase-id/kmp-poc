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
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.play
import platform.AVKit.AVPlayerViewController
import platform.CoreGraphics.CGRectMake
import platform.Foundation.*
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
@Composable
actual fun VideoPlayer(
    videoData: ByteArray,
    modifier: Modifier
) {
    // Create a temporary file URL from the byte array
    val videoUrl = remember(videoData) {
        try {
            val tempDir = NSTemporaryDirectory()
            val timestamp = platform.Foundation.NSDate().timeIntervalSince1970.toLong()
            val fileName = "video_${timestamp}.mp4"
            val filePath = "${tempDir}${fileName}"
            val fileUrl = NSURL.fileURLWithPath(filePath)

            // Create NSData from ByteArray
            val success = videoData.usePinned { pinned ->
                val nsData = NSData.create(
                    bytes = pinned.addressOf(0),
                    length = videoData.size.toULong()
                )

                // Write to file using NSFileManager
                NSFileManager.defaultManager.createFileAtPath(
                    path = filePath,
                    contents = nsData,
                    attributes = null
                )
            }

            println("Video file created at: $filePath, success: $success, size: ${videoData.size}")
            if (success) fileUrl else null
        } catch (e: Exception) {
            println("Error creating video file: ${e.message}")
            null
        }
    }

    DisposableEffect(videoUrl) {
        onDispose {
            videoUrl?.let { url ->
                try {
                    val path = url.path
                    if (path != null) {
                        NSFileManager.defaultManager.removeItemAtPath(path, null)
                    }
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
                    println("Creating AVPlayerViewController with URL: $videoUrl")
                    val player = AVPlayer.playerWithURL(videoUrl)

                    val playerViewController = AVPlayerViewController()
                    playerViewController.player = player
                    playerViewController.showsPlaybackControls = true

                    // Start playback
                    player.play()

                    playerViewController.view
                },
                modifier = Modifier.matchParentSize()
            )
        }
    }
}
