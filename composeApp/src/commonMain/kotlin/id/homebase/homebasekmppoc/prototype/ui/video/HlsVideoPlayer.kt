package id.homebase.homebasekmppoc.prototype.ui.video

import id.homebase.homebasekmppoc.prototype.lib.http.AppOrOwner
import id.homebase.homebasekmppoc.prototype.lib.http.PayloadWrapper
import id.homebase.homebasekmppoc.prototype.lib.video.VideoMetaData

/**
 * HLS video utility functions
 */

//

// m3u8 playlist manipulation for HLS streaming
suspend fun createHlsPlaylist(
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

