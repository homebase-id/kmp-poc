package id.homebase.homebasekmppoc.media

import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import id.homebase.homebasekmppoc.lib.core.ActivityProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class VideoMetadata(val duration: Long, val width: Int, val height: Int, val rotation: Int)

actual object FFmpegUtils {
    private const val TAG = "FFmpegUtils"

    actual fun getUniqueId(filePath: String): String {
        val file = File(filePath)
        // Simple deterministic ID generation based on file props
        return UUID.nameUUIDFromBytes("${file.name}_${file.length()}".toByteArray()).toString()
    }

    actual suspend fun grabThumbnail(inputPath: String): String? =
            withContext(Dispatchers.IO) {
                val context = ActivityProvider.requireActivity().applicationContext
                val file = File(inputPath)
                if (!file.exists()) return@withContext null

                val uniqueId = getUniqueId(inputPath)
                val outputDir = context.cacheDir
                // thumb0001-ID.png
                val outputName = "thumb0001-$uniqueId.png"
                val outputPath = File(outputDir, outputName).absolutePath

                if (File(outputPath).exists()) return@withContext outputPath

                val commandDestination = File(outputDir, "thumb%04d-$uniqueId.png").absolutePath

                // -frames:v 1
                val command = "-y -i \"$inputPath\" -frames:v 1 \"$commandDestination\""

                Log.d(TAG, "Thumbnail command: $command")

                val session = FFmpegKit.execute(command)
                if (ReturnCode.isSuccess(session.returnCode)) {
                    // Validate file creation
                    if (File(outputPath).exists()) {
                        return@withContext outputPath
                    } else {
                        Log.w(TAG, "Thumbnail generated success but file not found at $outputPath")
                        return@withContext null
                    }
                } else {
                    Log.e(TAG, "Thumbnail failed: ${session.failStackTrace}")
                    return@withContext null
                }
            }

    actual suspend fun getRotationFromFile(filePath: String): Int =
            withContext(Dispatchers.IO) {
                try {
                    val session =
                            FFprobeKit.execute(
                                    "-v quiet -select_streams v:0 -show_entries side_data_list=side_data_type,rotation -of json=compact=1 \"$filePath\""
                            )
                    val output = session.output
                    if (output.isNullOrBlank()) return@withContext 0

                    val json = JSONObject(output)
                    val streams = json.optJSONArray("streams")
                    val sideDataList =
                            streams?.optJSONObject(0)?.optJSONArray("side_data_list")
                                    ?: json.optJSONArray("side_data_list")

                    if (sideDataList != null) {
                        for (i in 0 until sideDataList.length()) {
                            val entry = sideDataList.optJSONObject(i)
                            if (entry?.optString("side_data_type") == "Display Matrix") {
                                val rotationStr = entry.optString("rotation")
                                val rotation = rotationStr.toDoubleOrNull()?.toInt() ?: 0
                                return@withContext if (rotation in -360..360) rotation else 0
                            }
                        }
                    }
                    return@withContext 0
                } catch (e: Exception) {
                    Log.w(TAG, "getRotationFromFile failed", e)
                    return@withContext 0
                }
            }

    actual suspend fun compressVideo(inputPath: String, onProgress: ((Float) -> Unit)?): String? =
            withContext(Dispatchers.IO) {
                val context = ActivityProvider.requireActivity().applicationContext
                val file = File(inputPath)
                if (!file.exists()) {
                    Log.e(TAG, "File not found: $inputPath")
                    return@withContext null
                }

                val outputDir = context.cacheDir
                val outputPath = File(outputDir, "compressed_${file.name}").absolutePath

                // distinct inputs for robust handling
                val arguments =
                        mutableListOf(
                                "-y",
                                "-i",
                                inputPath,
                                "-c:v",
                                "libx264",
                                "-b:v",
                                "3000k",
                                "-vf",
                                "scale='min(1280,iw)':-2",
                                "-preset",
                                "fast",
                                outputPath
                        )

                Log.d(TAG, "Compression arguments: $arguments")

                val session = FFmpegKit.executeWithArguments(arguments.toTypedArray())
                if (ReturnCode.isSuccess(session.returnCode)) {
                    return@withContext outputPath
                } else {
                    Log.e(TAG, "Compression failed: ${session.failStackTrace}")
                    return@withContext null
                }
            }

    actual suspend fun segmentVideo(inputPath: String): Pair<String, String>? =
            withContext(Dispatchers.IO) {
                val context = ActivityProvider.requireActivity().applicationContext
                val file = File(inputPath)
                if (!file.exists()) return@withContext null

                val rotation = getRotationFromFile(inputPath)
                val absRot = kotlin.math.abs(((rotation % 360) + 360) % 360)
                val needsRotationFix = absRot == 90 || absRot == 270

                val outputDir = context.cacheDir
                val playlistName = "ffmpeg-segmented-${UUID.randomUUID()}.m3u8"
                val playlistPath = File(outputDir, playlistName).absolutePath

                val commandArgs = mutableListOf<String>()
                commandArgs.add("-y")
                commandArgs.add("-i")
                commandArgs.add(inputPath)

                if (!needsRotationFix) {
                    commandArgs.add("-codec:v")
                    commandArgs.add("copy")
                    commandArgs.add("-codec:a")
                    commandArgs.add("copy")
                } else {
                    commandArgs.add("-c:v")
                    commandArgs.add("libx264")
                    commandArgs.add("-preset")
                    commandArgs.add("veryfast")
                    commandArgs.add("-crf")
                    commandArgs.add("23")
                    commandArgs.add("-g")
                    commandArgs.add("30")
                    commandArgs.add("-bf")
                    commandArgs.add("2")
                    commandArgs.add("-c:a")
                    commandArgs.add("copy")
                }

                commandArgs.add("-hls_time")
                commandArgs.add("6")
                commandArgs.add("-hls_list_size")
                commandArgs.add("0")
                commandArgs.add("-hls_flags")
                commandArgs.add("single_file")
                commandArgs.add("-f")
                commandArgs.add("hls")
                commandArgs.add(playlistPath)

                val command = commandArgs.joinToString(" ")
                Log.d(TAG, "Segment command: $command")

                val session = FFmpegKit.execute(command)
                if (ReturnCode.isSuccess(session.returnCode)) {
                    val segmentPath = playlistPath.replace(".m3u8", ".ts")
                    return@withContext Pair(playlistPath, segmentPath)
                } else {
                    Log.e(TAG, "Segmentation failed: ${session.failStackTrace}")
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
