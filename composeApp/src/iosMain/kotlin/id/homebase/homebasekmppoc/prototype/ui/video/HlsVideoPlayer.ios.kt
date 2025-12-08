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
import id.homebase.homebasekmppoc.prototype.lib.video.LocalVideoServer
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readBytes
import io.ktor.http.encodeURLParameter
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.runBlocking
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL

/**
 * iOS HLS video player using AVPlayer with LocalVideoServer proxy for authentication
 * Supports HLS streaming with DY0810 header injection for backend requests
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun HlsVideoPlayer(
    manifestUrl: String,
    clientAuthToken: String?,
    modifier: Modifier
) {
    Logger.i("HlsVideoPlayer.iOS") { "Initializing HLS player for URL: $manifestUrl" }
    Logger.i("HlsVideoPlayer.iOS") { "Client auth token present: ${clientAuthToken != null}" }
    if (clientAuthToken != null) {
        Logger.d("HlsVideoPlayer.iOS") { "Auth token length: ${clientAuthToken.length}" }
    }

    val playerViewController = remember(manifestUrl, clientAuthToken) {
        Logger.d("HlsVideoPlayer.iOS") { "Creating new AVPlayerViewController instance" }

        runBlocking {
            // Start local video server with auth token
            val localServer = LocalVideoServer(clientAuthToken)
            val serverUrl = localServer.start()
            Logger.i("HlsVideoPlayer.iOS") { "Local video server started at: $serverUrl" }

            // Fetch the manifest (it's already local, no auth needed)
            val client = HttpClient()
            Logger.d("HlsVideoPlayer.iOS") { "Fetching manifest from: $manifestUrl" }
            val manifestResponse = try {
                client.get(manifestUrl)
            } catch (e: Exception) {
                Logger.e("HlsVideoPlayer.iOS") { "Exception fetching manifest: ${e.message}" }
                client.close()
                return@runBlocking null
            }

            Logger.d("HlsVideoPlayer.iOS") { "Manifest response status: ${manifestResponse.status}" }
            val manifestBytes = manifestResponse.readBytes()
            val manifestText = manifestBytes.decodeToString()
            Logger.d("HlsVideoPlayer.iOS") { "Fetched manifest (${manifestBytes.size} bytes): ${manifestText.take(300)}..." }

            // Modify the manifest to proxy remote URLs through local server
            val modifiedManifest = manifestText.lines().joinToString("\n") { line ->
                if (line.startsWith("https://")) {
                    val encodedUrl = line.encodeURLParameter()
                    "$serverUrl/proxy?url=$encodedUrl"
                } else {
                    line
                }
            }
            Logger.d("HlsVideoPlayer.iOS") { "Modified manifest: ${modifiedManifest.take(200)}..." }

            // Register the modified manifest
            localServer.registerContent("manifest", modifiedManifest.encodeToByteArray(), "application/vnd.apple.mpegurl")
            Logger.i("HlsVideoPlayer.iOS") { "Registered modified manifest (${modifiedManifest.length} chars)" }

            val localManifestUrl = localServer.getContentUrl("manifest")
            Logger.i("HlsVideoPlayer.iOS") { "Local manifest URL: $localManifestUrl" }

            val url = NSURL.URLWithString(localManifestUrl) ?: run {
                Logger.e("HlsVideoPlayer.iOS") { "Invalid local manifest URL: $localManifestUrl" }
                client.close()
                return@runBlocking null
            }

            val player = AVPlayer.playerWithURL(url)
            val playerViewController = AVPlayerViewController()
            playerViewController.player = player
            playerViewController.showsPlaybackControls = true

            Logger.i("HlsVideoPlayer.iOS") { "AVPlayerViewController created and configured" }

            client.close()
            playerViewController
        }
    }

    DisposableEffect(playerViewController) {
        Logger.d("HlsVideoPlayer.iOS") { "Starting video playback" }
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
                    Logger.d("HlsVideoPlayer.iOS") { "Returning AVPlayerViewController view" }
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
