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
import id.homebase.homebasekmppoc.prototype.lib.video.LocalVideoServer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL
import kotlin.random.Random

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoPlayer(
    videoData: ByteArray,
    localVideoServer: LocalVideoServer,
    modifier: Modifier
) {
    var videoUrl by remember { mutableStateOf<String?>(null) }

    // 1. Register video content with LocalVideoServer
    LaunchedEffect(videoData) {
        try {
            // Generate a unique content ID using random numbers
            val contentId = "video-${Random.nextLong()}"
            localVideoServer.registerContent(
                id = contentId,
                data = videoData,
                contentType = "video/mp4"
            )
            videoUrl = localVideoServer.getContentUrl(contentId)
            Logger.d("VideoPlayer.iOS") { "Registered video content: $contentId at $videoUrl" }
        } catch (e: Exception) {
            Logger.e("VideoPlayer.iOS", e) { "Failed to register video content" }
        }
    }

    // 2. UI and Player Lifecycle
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val currentUrl = videoUrl

        if (currentUrl != null) {
            // Keep track of player to pause it later
            val nsUrl = NSURL.URLWithString(currentUrl)
            val player = remember(currentUrl) {
                if (nsUrl != null) {
                    AVPlayer.playerWithURL(nsUrl)
                } else {
                    Logger.e("VideoPlayer.iOS") { "Invalid URL: $currentUrl" }
                    null
                }
            }
            val playerViewController = remember(currentUrl) {
                if (player != null) {
                    AVPlayerViewController().apply {
                        this.player = player
                        this.showsPlaybackControls = true
                    }
                } else {
                    null
                }
            }

            // Lifecycle: Pause on Dispose
            DisposableEffect(Unit) {
                onDispose {
                    player?.pause()
                    // Explicitly clearing ref helps generic KMP cleanup sometimes
                    playerViewController?.player = null
                }
            }

            if (player != null && playerViewController != null) {
                UIKitView(
                    factory = {
                        // Start Playback
                        player.play()
                        playerViewController.view
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



