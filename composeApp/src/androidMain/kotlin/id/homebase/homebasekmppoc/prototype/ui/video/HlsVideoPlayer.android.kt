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
 * Supports HLS streaming with proper parsing and adaptive streaming
 */
@OptIn(UnstableApi::class)
@Composable
actual fun HlsVideoPlayer(
    manifestUrl: String,
    clientAuthToken: String?,
    modifier: Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (manifestUrl.isNotEmpty()) {
            Logger.i("HlsVideoPlayer") { "Creating ExoPlayer for HLS URL: $manifestUrl" }
            Logger.i("HlsVideoPlayer") { "clientAuthToken present: ${clientAuthToken != null}" }
            if (clientAuthToken != null) {
                Logger.i("HlsVideoPlayer") { "clientAuthToken value: $clientAuthToken" }
                Logger.i("HlsVideoPlayer") { "clientAuthToken length: ${clientAuthToken.length}" }
            }

            val exoPlayer = remember(manifestUrl, clientAuthToken) {
                ExoPlayer.Builder(context).build().apply {
                    // Create a composite DataSource that supports both HTTP and data URIs
                    val dataSourceFactory = object : DataSource.Factory {
                        private val httpDataSourceFactory = DefaultHttpDataSource.Factory().apply {
                            // Add authentication cookie if token is provided
                            if (clientAuthToken != null) {
                                // setDefaultRequestProperties(mapOf("Cookie" to "DY0810=$clientAuthToken"))
                                // Logger.i("HlsVideoPlayer") { "Set Cookie header: DY0810=$clientAuthToken" }
                                setDefaultRequestProperties(mapOf("DY0810" to clientAuthToken))
                            }
                        }

                        override fun createDataSource(): DataSource {
                            val httpDataSource = httpDataSourceFactory.createDataSource()
                            val dataSchemeDataSource = DataSchemeDataSource()

                            // Return a DataSource that tries data: scheme first, then falls back to HTTP
                            return object : DataSource {
                                private var currentDataSource: DataSource? = null

                                override fun addTransferListener(transferListener: androidx.media3.datasource.TransferListener) {
                                    httpDataSource.addTransferListener(transferListener)
                                }

                                override fun open(dataSpec: androidx.media3.datasource.DataSpec): Long {
                                    val scheme = dataSpec.uri.scheme
                                    currentDataSource = when (scheme) {
                                        "data" -> {
                                            Logger.d("HlsVideoPlayer") { "Using DataSchemeDataSource for: ${dataSpec.uri}" }
                                            dataSchemeDataSource
                                        }

                                        else -> {
                                            Logger.d("HlsVideoPlayer") { "Using HTTP DataSource for: ${dataSpec.uri}" }
                                            httpDataSource
                                        }
                                    }
                                    return currentDataSource!!.open(dataSpec)
                                }

                                override fun read(
                                    buffer: ByteArray,
                                    offset: Int,
                                    length: Int
                                ): Int {
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
                                Player.STATE_IDLE -> Logger.d("HlsVideoPlayer") { "Player state: IDLE" }
                                Player.STATE_BUFFERING -> Logger.d("HlsVideoPlayer") { "Player state: BUFFERING" }
                                Player.STATE_READY -> Logger.i("HlsVideoPlayer") { "Player state: READY - video is ready to play" }
                                Player.STATE_ENDED -> Logger.i("HlsVideoPlayer") { "Player state: ENDED" }
                            }
                        }

                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            Logger.e("HlsVideoPlayer") { "ExoPlayer error: ${error.message}" }
                            Logger.e("HlsVideoPlayer") { "Error code: ${error.errorCode}, cause: ${error.cause}" }
                        }
                    })
                }
            }

            DisposableEffect(Unit) {
                Logger.d("HlsVideoPlayer") { "ExoPlayer composed" }
                onDispose {
                    Logger.d("HlsVideoPlayer") { "Releasing ExoPlayer" }
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
