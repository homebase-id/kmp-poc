package id.homebase.homebasekmppoc.media

actual object FFmpegUtils {
    actual fun getUniqueId(filePath: String): String {
        return "stub_id"
    }

    actual suspend fun grabThumbnail(inputPath: String): String? {
        return null // Not implemented
    }

    actual suspend fun getRotationFromFile(filePath: String): Int {
        return 0 // Not implemented
    }

    actual suspend fun compressVideo(inputPath: String, onProgress: ((Float) -> Unit)?): String? {
        return null // Not implemented
    }

    actual suspend fun segmentVideo(inputPath: String): Pair<String, String>? {
        return null // Not implemented
    }

    actual suspend fun cacheInputVideo(fileName: String, data: ByteArray): String {
        return "stub_path"
    }
}
