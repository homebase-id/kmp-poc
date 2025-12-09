package id.homebase.homebasekmppoc.prototype.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL

/**
 * iOS HLS video player using AVPlayer
 * Receives a local manifest URL with proxied segment URLs
 * No LocalVideoServer inside - server is managed externally
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun HlsVideoPlayer(
    manifestUrl: String,
    clientAuthToken: String?,
    modifier: Modifier
) {
    Logger.i("HlsVideoPlayer.iOS") { "Creating HLS player for URL: $manifestUrl" }

    val playerViewController = remember(manifestUrl) {
        val url = NSURL.URLWithString(manifestUrl)
        if (url == null) {
            Logger.e("HlsVideoPlayer.iOS") { "Invalid manifest URL: $manifestUrl" }
            null
        } else {
            val player = AVPlayer.playerWithURL(url)
            val playerViewController = AVPlayerViewController()
            playerViewController.player = player
            playerViewController.showsPlaybackControls = true
            Logger.i("HlsVideoPlayer.iOS") { "AVPlayerViewController created" }
            playerViewController
        }
    }

    DisposableEffect(playerViewController) {
        playerViewController?.player?.play()
        onDispose {
            Logger.d("HlsVideoPlayer.iOS") { "Disposing AVPlayerViewController" }
            playerViewController?.player?.pause()
        }
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (playerViewController != null) {
            UIKitView(
                factory = {
                    playerViewController.view
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = "Failed to create video player",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
