package id.homebase.homebasekmppoc.media

import android.util.Log
import id.homebase.homebasekmppoc.lib.core.ActivityProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object FFmpegUtils {
    private const val TAG = "FFmpegUtils"

    actual fun getUniqueId(filePath: String): String {
        val file = File(filePath)
        return UUID.nameUUIDFromBytes("${file.name}_${file.length()}".toByteArray()).toString()
    }

    actual suspend fun grabThumbnail(inputPath: String): String? =
            withContext(Dispatchers.IO) {
                if (!FFmpegNative.isAvailable()) {
                    Log.e(TAG, "FFmpeg not available: ${FFmpegNative.getLoadError()}")
                    return@withContext null
                }

                val context = ActivityProvider.requireActivity().applicationContext
                val file = File(inputPath)
                if (!file.exists()) return@withContext null

                val uniqueId = getUniqueId(inputPath)
                val outputPath = File(context.cacheDir, "thumb_$uniqueId.jpg").absolutePath

                if (File(outputPath).exists()) return@withContext outputPath

                Log.d(TAG, "Extracting thumbnail: $inputPath")

                val result = FFmpegNative.extractThumbnail(inputPath, outputPath, 1.0)
                if (result == 0 && File(outputPath).exists()) {
                    Log.d(TAG, "Thumbnail extracted: $outputPath")
                    return@withContext outputPath
                } else {
                    Log.e(TAG, "Thumbnail extraction failed: $result")
                    return@withContext null
                }
            }

    actual suspend fun getRotationFromFile(filePath: String): Int =
            withContext(Dispatchers.IO) {
                if (!FFmpegNative.isAvailable()) return@withContext 0

                try {
                    FFmpegNative.getVideoRotation(filePath)
                } catch (e: Exception) {
                    Log.w(TAG, "getRotationFromFile failed", e)
                    0
                }
            }

    actual suspend fun compressVideo(inputPath: String, onProgress: ((Float) -> Unit)?): String? =
            withContext(Dispatchers.IO) {
                if (!FFmpegNative.isAvailable()) {
                    Log.e(TAG, "FFmpeg not available: ${FFmpegNative.getLoadError()}")
                    return@withContext null
                }

                val context = ActivityProvider.requireActivity().applicationContext
                val file = File(inputPath)
                if (!file.exists()) {
                    Log.e(TAG, "File not found: $inputPath")
                    return@withContext null
                }

                val outputPath = File(context.cacheDir, "compressed_${file.name}").absolutePath

                // Remove existing output if any
                File(outputPath).delete()

                Log.d(TAG, "Compressing video: $inputPath -> $outputPath")

                // 3 Mbps target bitrate, max 1280px width
                val result = FFmpegNative.compressVideo(inputPath, outputPath, 3_000_000, 1280)

                if (result == 0 && File(outputPath).exists()) {
                    Log.d(TAG, "Compression complete: $outputPath")
                    return@withContext outputPath
                } else {
                    Log.e(TAG, "Compression failed: $result")
                    return@withContext null
                }
            }

    actual suspend fun segmentVideo(inputPath: String): Pair<String, String>? =
            withContext(Dispatchers.IO) {
                if (!FFmpegNative.isAvailable()) {
                    Log.e(TAG, "FFmpeg not available: ${FFmpegNative.getLoadError()}")
                    return@withContext null
                }

                val context = ActivityProvider.requireActivity().applicationContext
                val file = File(inputPath)
                if (!file.exists()) return@withContext null

                val rotation = getRotationFromFile(inputPath)
                val absRot = kotlin.math.abs(((rotation % 360) + 360) % 360)

                val playlistName = "hls_${UUID.randomUUID()}.m3u8"
                val playlistPath = File(context.cacheDir, playlistName).absolutePath

                Log.d(TAG, "Creating HLS segments: $inputPath -> $playlistPath")

                val result = FFmpegNative.segmentToHLS(inputPath, playlistPath, 6)

                if (result == 0 && File(playlistPath).exists()) {
                    // HLS single_file flag creates .ts with same base name
                    val segmentPath = playlistPath.replace(".m3u8", ".ts")
                    Log.d(TAG, "HLS segmentation complete: $playlistPath")
                    return@withContext Pair(playlistPath, segmentPath)
                } else {
                    Log.e(TAG, "HLS segmentation failed: $result")
                    return@withContext null
                }
            }

    actual suspend fun cacheInputVideo(fileName: String, data: ByteArray): String =
            withContext(Dispatchers.IO) {
                val context = ActivityProvider.requireActivity().applicationContext
                val cacheFile = File(context.cacheDir, "input_$fileName")
                cacheFile.writeBytes(data)
                return@withContext cacheFile.absolutePath
            }
}
