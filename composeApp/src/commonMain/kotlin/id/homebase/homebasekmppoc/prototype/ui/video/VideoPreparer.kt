import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.prototype.lib.http.AppOrOwner
import id.homebase.homebasekmppoc.prototype.lib.http.PayloadWrapper
import id.homebase.homebasekmppoc.prototype.lib.http.cookieNameFrom
import id.homebase.homebasekmppoc.prototype.lib.video.LocalVideoServer
import id.homebase.homebasekmppoc.prototype.lib.video.VideoMetaData
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

// VideoPreparer.kt

sealed class VideoPreparationResult {
    data class Success(val url: String, val contentId: String) : VideoPreparationResult()
    data class Error(val message: String) : VideoPreparationResult()
}

//

suspend fun prepareVideoContentForPlayback(
    appOrOwner: AppOrOwner,
    videoServer: LocalVideoServer,
    videoPayload: PayloadWrapper
): VideoPreparationResult = withContext(Dispatchers.Default) {
    try {
        val videoMetaData = videoPayload.getVideoMetaData(appOrOwner)

        // --- HLS Logic ---
        if (videoMetaData.hlsPlaylist != null) {
            val hlsPlayList = createHlsPlaylist(videoPayload, appOrOwner, videoMetaData)
            val serverUrl = videoServer.getServerUrl()
            val contentId = "video-manifest-${videoPayload.compositeKey}.m3u8"

            // Pure logic: Rewrite the playlist
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

            return@withContext VideoPreparationResult.Success(
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
                data = videoBytes, // Ideally this should be a File Path, not ByteArray
                contentType = "video/mp4",
                authTokenHeaderName = cookieNameFrom(appOrOwner),
                authToken = videoPayload.authenticated.clientAuthToken
            )

            return@withContext VideoPreparationResult.Success(
                url = videoServer.getContentUrl(contentId),
                contentId = contentId
            )
        }

        return@withContext VideoPreparationResult.Error("Segmented MP4/Unknown format not supported")

    } catch (e: Exception) {
        Logger.e("VideoPreparer", e) { "Failed to prepare video" }
        return@withContext VideoPreparationResult.Error(e.message ?: "Unknown error")
    }
}

//

fun unprepareVideoContent(contentId: String, videoServer: LocalVideoServer) {
    videoServer.unregisterContent(contentId)
}

//

private suspend fun createHlsPlaylist(
    payloadWrapper: PayloadWrapper,
    appOrOwner: AppOrOwner,
    videoMetaData: VideoMetaData): String {

    val aesKey = payloadWrapper.decryptKeyHeader()?.aesKey?.Base64Encode()

    val lines = videoMetaData.hlsPlaylist?.lines() ?: throw Exception("No HLS playlist content found")
    if (lines.isEmpty() || !lines[0].startsWith("#EXTM3U")) {
        throw Exception("Invalid HLS playlist content")
    }

    val modifiedLines = ArrayList<String>(lines.size) // Pre-allocate size

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
                val newUrl = payloadWrapper.getEncryptedPayloadUri(appOrOwner)
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

