package id.homebase.homebasekmppoc.prototype.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVKit.AVPlayerViewController
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun HlsVideoPlayer(manifestUrl: String, modifier: Modifier) {
    Logger.i("HlsVideoPlayer.iOS") { "Configuring for HLS: $manifestUrl" }

    // 1. Audio Session Configuration
    // Best Practice: Ensure video audio plays even if hardware Silent Switch is ON.
    LaunchedEffect(Unit) {
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, error = null)
            session.setActive(true, error = null)
        } catch (e: Exception) {
            Logger.e("HlsVideoPlayer.iOS") { "Failed to set audio session: ${e.message}" }
        }
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (manifestUrl.isEmpty()) {
            Text("No URL", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Box
        }

        // 2. Controller & Player Creation
        // We remember the pair to ensure they stay strictly linked
        val playerStack = remember(manifestUrl) {
            val url = NSURL.URLWithString(manifestUrl)
            if (url == null) {
                Logger.e("HlsVideoPlayer.iOS") { "Invalid URL" }
                null
            } else {
                val player = AVPlayer.playerWithURL(url)
                val vc = AVPlayerViewController().apply {
                    this.player = player
                    this.showsPlaybackControls = true
                }
                Pair(player, vc)
            }
        }

        // 3. Lifecycle Management
        DisposableEffect(playerStack) {
            val player = playerStack?.first
            val vc = playerStack?.second

            player?.play()

            onDispose {
                Logger.d("HlsVideoPlayer.iOS") { "Stopping playback and cleaning up" }
                player?.pause()
                // Important: Break the retention cycle between VC and Player
                vc?.player = null
            }
        }

        // 4. Render
        if (playerStack != null) {
            UIKitView(
                factory = {
                    playerStack.second.view
                },
                modifier = Modifier.fillMaxSize(),
                // Properties typically don't change dynamically for HLS in this setup,
                // but if you needed to update resize modes, you'd do it here.
                update = { }
            )
        } else {
            Text("Invalid URL", color = MaterialTheme.colorScheme.error)
        }
    }
}
