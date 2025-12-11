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
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.play
import platform.AVFoundation.pause
import platform.AVKit.AVPlayerViewController
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun VideoPlayer(
    videoData: ByteArray,
    modifier: Modifier
) {
    var videoUrl by remember { mutableStateOf<NSURL?>(null) }

    // 1. Write File in Background (Off Main Thread)
    LaunchedEffect(videoData) {
        withContext(Dispatchers.Default) {
            try {
                val tempDir = NSTemporaryDirectory()
                val fileName = "video_${NSDate().timeIntervalSince1970}.mp4"
                val filePath = "${tempDir}${fileName}"
                val url = NSURL.fileURLWithPath(filePath)

                // Efficiently write data
                val success = videoData.usePinned { pinned ->
                    val nsData = NSData.create(
                        bytesNoCopy = pinned.addressOf(0),
                        length = videoData.size.toULong(),
                        freeWhenDone = false
                    )
                    nsData.writeToURL(url, atomically = true)
                }

                if (success) {
                    videoUrl = url
                } else {
                    println("Failed to write video file on iOS")
                }
            } catch (e: Exception) {
                println("Error writing iOS video file: ${e.message}")
            }
        }
    }

    // 2. File Cleanup
    DisposableEffect(videoUrl) {
        val urlToDelete = videoUrl
        onDispose {
            urlToDelete?.path?.let { path ->
                val fileManager = NSFileManager.defaultManager
                if (fileManager.fileExistsAtPath(path)) {
                    fileManager.removeItemAtPath(path, null)
                }
            }
        }
    }

    // 3. UI and Player Lifecycle
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val currentUrl = videoUrl

        if (currentUrl != null) {
            // Keep track of player to pause it later
            val player = remember(currentUrl) { AVPlayer.playerWithURL(currentUrl) }
            val playerViewController = remember(currentUrl) {
                AVPlayerViewController().apply {
                    this.player = player
                    this.showsPlaybackControls = true
                }
            }

            // Lifecycle: Pause on Dispose
            DisposableEffect(Unit) {
                onDispose {
                    player.pause()
                    // Explicitly clearing ref helps generic KMP cleanup sometimes
                    playerViewController.player = null
                }
            }

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
            // Loading Indicator while writing file
            CircularProgressIndicator()
        }
    }
}



