package id.homebase.homebasekmppoc.prototype.ui.video

import androidx.annotation.OptIn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSchemeDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import co.touchlab.kermit.Logger

/**
 * Android HLS video player using ExoPlayer (Media3)
 * Receives a local manifest URL with proxied segment URLs
 * No LocalVideoServer inside - server is managed externally
 */
@OptIn(UnstableApi::class)
@Composable
actual fun HlsVideoPlayer(
    manifestUrl: String,
    clientAuthToken: String?,
    modifier: Modifier
) {
    val context = LocalContext.current

    Logger.i("HlsVideoPlayer.Android") { "Creating HLS player for URL: $manifestUrl" }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (manifestUrl.isNotEmpty()) {
            val exoPlayer = remember(manifestUrl) {
                ExoPlayer.Builder(context).build().apply {
                    // Create a composite DataSource that supports both HTTP and data URIs
                    val dataSourceFactory = object : DataSource.Factory {
                        private val httpDataSourceFactory = DefaultHttpDataSource.Factory()

                        override fun createDataSource(): DataSource {
                            val httpDataSource = httpDataSourceFactory.createDataSource()
                            val dataSchemeDataSource = DataSchemeDataSource()

                            return object : DataSource {
                                private var currentDataSource: DataSource? = null

                                override fun addTransferListener(transferListener: androidx.media3.datasource.TransferListener) {
                                    httpDataSource.addTransferListener(transferListener)
                                }

                                override fun open(dataSpec: androidx.media3.datasource.DataSpec): Long {
                                    val scheme = dataSpec.uri.scheme
                                    currentDataSource = when (scheme) {
                                        "data" -> dataSchemeDataSource
                                        else -> httpDataSource
                                    }
                                    return currentDataSource!!.open(dataSpec)
                                }

                                override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                                    return currentDataSource?.read(buffer, offset, length) ?: -1
                                }

                                override fun getUri() = currentDataSource?.uri

                                override fun getResponseHeaders() =
                                    currentDataSource?.responseHeaders ?: emptyMap()

                                override fun close() {
                                    currentDataSource?.close()
                                }
                            }
                        }
                    }

                    val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(manifestUrl))

                    setMediaSource(hlsMediaSource)
                    prepare()
                    playWhenReady = true

                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_IDLE -> Logger.d("HlsVideoPlayer.Android") { "Player state: IDLE" }
                                Player.STATE_BUFFERING -> Logger.d("HlsVideoPlayer.Android") { "Player state: BUFFERING" }
                                Player.STATE_READY -> Logger.i("HlsVideoPlayer.Android") { "Player state: READY" }
                                Player.STATE_ENDED -> Logger.i("HlsVideoPlayer.Android") { "Player state: ENDED" }
                            }
                        }

                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            Logger.e("HlsVideoPlayer.Android") { "ExoPlayer error: ${error.message}" }
                        }
                    })
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    Logger.d("HlsVideoPlayer.Android") { "Releasing ExoPlayer" }
                    exoPlayer.release()
                }
            }

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = "No video URL provided",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
