package id.homebase.homebasekmppoc.prototype.ui.video

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.prototype.lib.video.LocalVideoServer
import java.util.UUID

@OptIn(UnstableApi::class)
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
            val contentId = "video-${UUID.randomUUID()}"
            localVideoServer.registerContent(
                id = contentId,
                data = videoData,
                contentType = "video/mp4"
            )
            videoUrl = localVideoServer.getContentUrl(contentId)
            Logger.d("VideoPlayer.Android") { "Registered video content: $contentId at $videoUrl" }
        } catch (e: Exception) {
            Logger.e("VideoPlayer.Android", e) { "Failed to register video content" }
        }
    }

    // 2. Render
    Box(modifier = modifier) {
        val currentUrl = videoUrl
        if (currentUrl != null) {
            ExoPlayerContainer(url = currentUrl)
        } else {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ExoPlayerContainer(url: String) {
    val context = LocalContext.current

    // Initialize ExoPlayer (Best Practice Standard)
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_OFF
            playWhenReady = true
        }
    }

    // Load Media
    LaunchedEffect(url) {
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
    }

    // Release Resources
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // 3. The View Implementation
    AndroidView(
        factory = { ctx ->
            // Create the view ONCE
            PlayerView(ctx).apply {
                useController = true
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { playerView ->
            // CRITICAL FIX: SurfaceView vs Compose Timing
            // ExoPlayer uses SurfaceView, which renders to a distinct hardware surface behind the window.
            // It requires a valid Window Token to "punch the hole" through the UI.
            // During the Compose `update` pass, the View exists but might not yet be fully attached
            // to the Window Manager. Assigning the player immediately causes the decoder to
            // push frames to an invalid Surface, resulting in audio-only playback (Black Screen).
            //
            // .post() pushes the assignment to the end of the Main Thread Message Queue,
            // ensuring the View is fully attached and the Surface is ready before the player finds it.
            playerView.post {
                playerView.player = exoPlayer
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}





