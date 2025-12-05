package id.homebase.homebasekmppoc.prototype.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.play
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL

/**
 * iOS HLS video player using AVPlayer
 * Supports HLS streaming from URLs
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun HlsVideoPlayer(
    manifestUrl: String,
    clientAuthToken: String?,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (manifestUrl.isNotEmpty()) {
            val videoUrl = remember(manifestUrl) {
                Logger.i("HlsVideoPlayer") { "Creating NSURL for: $manifestUrl" }
                NSURL.URLWithString(manifestUrl)
            }

            if (videoUrl != null) {
                DisposableEffect(videoUrl) {
                    Logger.d("HlsVideoPlayer") { "Setting up AVPlayer" }
                    onDispose {
                        Logger.d("HlsVideoPlayer") { "Disposing AVPlayer" }
                    }
                }

                UIKitView(
                    factory = {
                        Logger.i("HlsVideoPlayer") { "Creating AVPlayerViewController with URL: $videoUrl" }
                        val player = AVPlayer.playerWithURL(videoUrl)

                        val playerViewController = AVPlayerViewController()
                        playerViewController.player = player
                        playerViewController.showsPlaybackControls = true

                        // Start playback
                        player.play()

                        playerViewController.view
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = "Invalid video URL",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = "No video URL provided",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
