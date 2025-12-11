package id.homebase.homebasekmppoc.prototype.ui.video

import android.net.Uri
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@OptIn(UnstableApi::class)
@Composable
actual fun VideoPlayer(
    videoData: ByteArray,
    modifier: Modifier
) {
    val context = LocalContext.current
    var videoUri by remember { mutableStateOf<Uri?>(null) }

    // 1. IO Operation (Correct Best Practice: Off Main Thread)
    LaunchedEffect(videoData) {
        withContext(Dispatchers.IO) {
            try {
                val fileName = "temp_video_${UUID.randomUUID()}.mp4"
                val file = File(context.cacheDir, fileName)
                FileOutputStream(file).use { it.write(videoData) }
                videoUri = Uri.fromFile(file)
            } catch (e: Exception) {
                Logger.e("VideoPlayer", e) { "Failed to write video" }
            }
        }
    }

    // 2. Cleanup
    DisposableEffect(videoUri) {
        val fileToDelete = videoUri
        onDispose {
            fileToDelete?.path?.let { File(it).delete() }
        }
    }

    // 3. Render
    Box(modifier = modifier) {
        val currentUri = videoUri
        if (currentUri != null) {
            ExoPlayerContainer(uri = currentUri)
        } else {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ExoPlayerContainer(uri: Uri) {
    val context = LocalContext.current

    // Initialize ExoPlayer (Best Practice Standard)
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_OFF
            playWhenReady = true
        }
    }

    // Load Media
    LaunchedEffect(uri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
    }

    // Release Resources
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // 4. The View Implementation
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





