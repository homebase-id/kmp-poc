package id.homebase.homebasekmppoc.prototype.ui.video

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.prototype.lib.http.AppOrOwner
import id.homebase.homebasekmppoc.prototype.lib.http.PayloadWrapper
import id.homebase.homebasekmppoc.prototype.lib.http.cookieNameFrom
import id.homebase.homebasekmppoc.prototype.lib.video.LocalVideoServer
import io.ktor.http.encodeURLParameter
import kotlin.uuid.Uuid

/**
 * Page for playing HLS videos
 *
 */
@Composable
fun VideoPlayerPage(
    appOrOwner:  AppOrOwner,
    localVideoServer: LocalVideoServer,
    videoPayload: PayloadWrapper,
    videoTitle: String = "Video Player",
    onBack: () -> Unit
) {
    val hlsInfo = "Some info here..."

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Header
        Text(
            text = videoTitle,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Video player card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            VideoPlayerContainer(appOrOwner, localVideoServer, videoPayload)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Video info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "HLS Manifest URL:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = hlsInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Back button
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Video List")
        }
    }
}

//

@Composable
private fun VideoPlayerContainer(
    appOrOwner: AppOrOwner,
    videoServer: LocalVideoServer,
    videoPayload: PayloadWrapper
) {
    var isLoading by remember { mutableStateOf(true) }
    var hlsManifestUrl by remember { mutableStateOf<String?>(null) }
    var videoUrl by remember { mutableStateOf<String?>(null) }
    var videoServerContentId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val videoMetaData = videoPayload.getVideoMetaData(appOrOwner)

        //
        // HLS
        //
        if (videoMetaData.hlsPlaylist != null) {
            val hlsPlayList = createHlsPlaylist(videoPayload, appOrOwner, videoMetaData)
            Logger.d("VideoPlayer") { "Original hlsPlayList: $hlsPlayList" }

            // Get the local server URL
            val serverUrl = videoServer.getServerUrl()
            Logger.d("VideoPlayer") { "Video server running at: $serverUrl" }

            val contentId = "video-manifest-${videoPayload.compositeKey}"

            // Modify the manifest to proxy remote segment URLs through local server
            val proxiedPlayList = hlsPlayList.lines()
                .joinToString("\n") { line ->
                    if (line.startsWith("https://")) {
                        val encodedUrl = line.encodeURLParameter()
                        "$serverUrl/proxy?url=$encodedUrl&manifestId=$contentId"
                    } else {
                        line
                    }
                }

            Logger.d("VideoPlayer") { "Proxied hlsPlayList: $proxiedPlayList" }

            // Register the manifest ID on Video Server
            videoServer.registerContent(
                id = contentId,
                data = proxiedPlayList.encodeToByteArray(),
                contentType = "application/vnd.apple.mpegurl",
                authTokenHeaderName = cookieNameFrom(appOrOwner),
                authToken = videoPayload.authenticated.clientAuthToken
            )

            hlsManifestUrl = videoServer.getContentUrl(contentId)
            Logger.d("VideoPlayer") { "Manifest URL: $hlsManifestUrl" }

            videoServerContentId = contentId
        }

        //
        // Segmented MP4
        //
        else if (videoMetaData.isSegmented) {
            // Not implemented yet
            throw Exception("Segmented MP4 not supported yet")
        }

        //
        // Non-segmented MP4 file
        //
        else {
            val videoBytes = videoPayload.getPayloadBytes(AppOrOwner.Owner)

            val contentId = "video-${Uuid.random()}"
            videoServer.registerContent(
                id = contentId,
                data = videoBytes,
                contentType = "video/mp4",
                authTokenHeaderName = cookieNameFrom(appOrOwner),
                authToken = videoPayload.authenticated.clientAuthToken
            )
            videoUrl = videoServer.getContentUrl(contentId)
            Logger.d("VideoPlayer") { "Video URL: $hlsManifestUrl" }

            videoServerContentId = contentId
        }

        isLoading = false
    }

    if (isLoading) {
        return
    }

    // HLS video?
    if (hlsManifestUrl != null) {
        Logger.i("VideoPlayer") { "Creating HlsVideoPlayer" }
        HlsVideoPlayer(
            manifestUrl = hlsManifestUrl!!,
            modifier = Modifier.fillMaxWidth().height(400.dp)
        )
    } else if (videoUrl != null) {
        Logger.i("VideoPlayer") { "Creating VideoPlayer" }
        VideoPlayer(
            videoUrl,
            modifier = Modifier.fillMaxWidth().height(400.dp)
        )
    } else {
        throw Exception("Video not supported")
    }

    //

    DisposableEffect(Unit) {
        onDispose {
            if (videoServerContentId != null) {
                videoServer.unregisterContent(videoServerContentId!!)
                Logger.d("VideoPlayer") { "Unregistered video content: $videoServerContentId" }
            }
        }
    }

    //

}
