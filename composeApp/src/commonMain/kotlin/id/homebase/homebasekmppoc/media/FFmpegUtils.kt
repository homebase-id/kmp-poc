package id.homebase.homebasekmppoc.media

expect object FFmpegUtils {
    fun getUniqueId(filePath: String): String

    suspend fun grabThumbnail(inputPath: String): String?

    suspend fun getRotationFromFile(filePath: String): Int

    suspend fun compressVideo(inputPath: String, onProgress: ((Float) -> Unit)? = null): String?

    suspend fun segmentVideo(inputPath: String): Pair<String, String>?
}
