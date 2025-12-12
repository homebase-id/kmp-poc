import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import id.homebase.homebasekmppoc.prototype.lib.drives.PayloadDescriptor
import id.homebase.homebasekmppoc.prototype.lib.http.AppOrOwner
import id.homebase.homebasekmppoc.prototype.lib.http.PayloadWrapper
import id.homebase.homebasekmppoc.prototype.lib.http.cookieNameFrom
import id.homebase.homebasekmppoc.prototype.lib.video.LocalVideoServer
import id.homebase.homebasekmppoc.prototype.lib.video.VideoMetaData
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

sealed class VideoPlaybackPreparationResult {
    data class Success(val url: String, val contentId: String) : VideoPlaybackPreparationResult()
    data class Error(val message: String) : VideoPlaybackPreparationResult()
}

//

suspend fun prepareVideoContentForPlayback(
    appOrOwner: AppOrOwner,
    videoServer: LocalVideoServer,
    videoPayload: PayloadWrapper
): VideoPlaybackPreparationResult = withContext(Dispatchers.Default) {
    try {
        var videoMetaData = videoPayload.getVideoMetaData(appOrOwner)

        //
        // HLS
        //
        val isHls = videoMetaData.isSegmented && (
                (videoMetaData.hlsPlaylist != null && videoMetaData.key == null) ||
                (videoMetaData.hlsPlaylist == null && videoMetaData.key != null))

        if (isHls) {

            // For HLS, when hlsPlaylist is null, we need to get a new VideoMetaData from a different payload
            if (videoMetaData.hlsPlaylist == null) {
                val videoMetaDataPayload = videoPayload.headerWrapper.getPayloadWrapper(videoMetaData.key!!)
                val json = videoMetaDataPayload.getPayloadBytes(appOrOwner).decodeToString()
                videoMetaData = OdinSystemSerializer.deserialize<VideoMetaData>(json)
            }

            Logger.i ("VideoPreparer") { "Preparing HLS video playback" }

            val hlsPlayList = createHlsPlaylist(
                appOrOwner,
                videoPayload,
                videoMetaData)

            val serverUrl = videoServer.getServerUrl()
            val contentId = "video-manifest-${videoPayload.getCompositeKey()}.m3u8"

            // Rewrite the playlist
            val proxiedPlayList = hlsPlayList.lines().joinToString("\n") { line ->
                if (line.startsWith("https://")) {
                    val encodedUrl = line.encodeURLParameter()
                    "$serverUrl/proxy?url=$encodedUrl&manifestId=$contentId"
                } else {
                    line
                }
            }

            videoServer.registerContent(
                id = contentId,
                data = proxiedPlayList.encodeToByteArray(),
                contentType = "application/vnd.apple.mpegurl",
                authTokenHeaderName = cookieNameFrom(appOrOwner),
                authToken = videoPayload.authenticated.clientAuthToken
            )

            return@withContext VideoPlaybackPreparationResult.Success(
                url = videoServer.getContentUrl(contentId),
                contentId = contentId
            )
        }

        // --- MP4 Logic ---
        else if (!videoMetaData.isSegmented) {
            // WARNING: getPayloadBytes puts entire file in RAM.
            val videoBytes = videoPayload.getPayloadBytes(AppOrOwner.Owner)
            val contentId = "video-${Uuid.random()}"

            videoServer.registerContent(
                id = contentId,
                data = videoBytes,
                contentType = "video/mp4",
                authTokenHeaderName = cookieNameFrom(appOrOwner),
                authToken = videoPayload.authenticated.clientAuthToken
            )

            return@withContext VideoPlaybackPreparationResult.Success(
                url = videoServer.getContentUrl(contentId),
                contentId = contentId
            )
        }

        return@withContext VideoPlaybackPreparationResult.Error("Segmented MP4/Unknown format not supported")

    } catch (e: Exception) {
        Logger.e("VideoPreparer", e) { "Failed to prepare video" }
        return@withContext VideoPlaybackPreparationResult.Error(e.message ?: "Unknown error")
    }
}

//

fun unprepareVideoContent(contentId: String, videoServer: LocalVideoServer) {
    videoServer.unregisterContent(contentId)
}

//

private suspend fun createHlsPlaylist(
    appOrOwner: AppOrOwner,
    videoPayload: PayloadWrapper,
    videoMetaData: VideoMetaData): String {

    if (!videoMetaData.isSegmented) {
        throw Exception("Video is not segmented; HLS playlist cannot be created")
    }

    if (videoMetaData.hlsPlaylist == null) {
        throw Exception("Insufficient data to create HLS playlist")
    }

    val hlsPlaylist = videoMetaData.hlsPlaylist

    val lines = hlsPlaylist.lines()
    if (lines.isEmpty() || !lines[0].startsWith("#EXTM3U")) {
        throw Exception("Invalid HLS playlist content")
    }

    val modifiedLines = ArrayList<String>(lines.size) // Pre-allocate size

    val aesKey = videoPayload.decryptKeyHeader()?.aesKey?.Base64Encode()
    for (line in lines) {
        when {
            // Case 1: Encryption Key
            line.startsWith("#EXT-X-KEY:METHOD=AES-128") -> {
                if (aesKey == null) {
                    throw Exception("AES key is null but playlist requires encryption key")
                }
                val uriRegex = Regex("""URI="([^"]+)"""")
                val match = uriRegex.find(line)
                if (match != null && match.groupValues.size > 1) {
                    val originalKeyUri = match.groupValues[1]
                    val newKeyUri = "data:application/octet-stream;base64,$aesKey"
                    modifiedLines.add(line.replace(originalKeyUri, newKeyUri))
                }
            }
            // Case 2: Segment URL (Not a comment/tag and not empty)
            !line.startsWith("#") && line.isNotBlank() -> {
                val newUrl = videoPayload.getEncryptedPayloadUri(appOrOwner)
                modifiedLines.add(newUrl)
            }
            // Case 3: Metadata / Comments / Empty lines
            else -> {
                modifiedLines.add(line)
            }
        }
    }
    return modifiedLines.joinToString("\n")
}

//

