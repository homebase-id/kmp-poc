package id.homebase.homebasekmppoc.prototype.ui.video

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import co.touchlab.kermit.Logger

@OptIn(UnstableApi::class)
@Composable
actual fun HlsVideoPlayer(manifestUrl: String, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (manifestUrl.isEmpty()) {
            Text(
                text = "No video URL provided",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Box
        }

        // 1. Initialize Player ONCE (Do not dependent on manifestUrl here to avoid leaks)
        val exoPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                repeatMode = ExoPlayer.REPEAT_MODE_OFF
                playWhenReady = true

                // Add logging listener
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            Logger.i("HlsVideoPlayer") { "Player Ready" }
                        }
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Logger.e("HlsVideoPlayer", error) { "Playback Error" }
                    }
                })
            }
        }

        // 2. Handle URL Changes safely
        LaunchedEffect(manifestUrl) {
            Logger.i("HlsVideoPlayer") { "Loading HLS Manifest: $manifestUrl" }

            // DefaultDataSource handles HTTP, FILE, DATA, etc. automatically
            val dataSourceFactory = DefaultDataSource.Factory(context)

            val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(manifestUrl))

            exoPlayer.setMediaSource(hlsMediaSource)
            exoPlayer.prepare()
        }

        // 3. Lifecycle Management (Pause/Resume download/playback)
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                    Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        // 4. Cleanup
        DisposableEffect(Unit) {
            onDispose {
                Logger.d("HlsVideoPlayer") { "Releasing ExoPlayer" }
                exoPlayer.release()
            }
        }

        // 5. Render View
        AndroidView(
            factory = { ctx ->
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

        // Optional: Show spinner when buffering
        // You would need to add a mutableStateOf to track 'isPlaying' via the Listener above
    }
}

