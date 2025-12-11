package id.homebase.homebasekmppoc.prototype.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
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
actual fun VideoPlayer(
    videoUrl: String?,
    modifier: Modifier
) {
    // 1. Audio Session Configuration
    // Best Practice: Ensure video audio plays even if hardware Silent Switch is ON.
    LaunchedEffect(Unit) {
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, error = null)
            session.setActive(true, error = null)
        } catch (e: Exception) {
            Logger.e("VideoPlayer.iOS") { "Failed to set audio session: ${e.message}" }
        }
    }

    // 3. UI and Player Lifecycle
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val currentUrl = videoUrl

        if (currentUrl != null) {
            // Create player and controller together to ensure they stay linked
            val playerStack = remember(currentUrl) {
                val nsUrl = NSURL.URLWithString(currentUrl)
                if (nsUrl != null) {
                    val player = AVPlayer.playerWithURL(nsUrl)
                    val vc = AVPlayerViewController().apply {
                        this.player = player
                        this.showsPlaybackControls = true
                    }
                    Pair(player, vc)
                } else {
                    Logger.e("VideoPlayer.iOS") { "Invalid URL: $currentUrl" }
                    null
                }
            }

            // Lifecycle: Start playback and cleanup on dispose
            DisposableEffect(playerStack) {
                val player = playerStack?.first
                val vc = playerStack?.second

                // Start playback after view is ready
                player?.play()

                onDispose {
                    Logger.d("VideoPlayer.iOS") { "Stopping playback and cleaning up" }
                    player?.pause()
                    // Break the retention cycle between VC and Player
                    vc?.player = null
                }
            }

            if (playerStack != null) {
                UIKitView(
                    factory = {
                        playerStack.second.view
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        // No-op creates stable view.
                        // If you needed to update layout constraints, do it here.
                    }
                )
            } else {
                CircularProgressIndicator()
            }
        } else {
            // Loading Indicator while registering content
            CircularProgressIndicator()
        }
    }
}



