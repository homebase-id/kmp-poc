package id.homebase.homebasekmppoc.media

import java.io.BufferedReader
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object FFmpegUtils {

    actual fun getUniqueId(filePath: String): String {
        val file = File(filePath)
        return UUID.nameUUIDFromBytes("${file.name}_${file.length()}".toByteArray()).toString()
    }

    actual suspend fun grabThumbnail(inputPath: String): String? =
            withContext(Dispatchers.IO) {
                if (!FFmpegBinaryManager.isAvailable()) {
                    println("FFmpeg binaries not available for this platform")
                    return@withContext null
                }

                val uniqueId = getUniqueId(inputPath)
                val outputPath = "${System.getProperty("java.io.tmpdir")}/thumb_$uniqueId.jpg"

                val command =
                        listOf(
                                FFmpegBinaryManager.ffmpegPath(),
                                "-y",
                                "-i",
                                inputPath,
                                "-ss",
                                "00:00:01.000",
                                "-vframes",
                                "1",
                                outputPath
                        )

                val exitCode = runProcess(command)
                if (exitCode == 0 && File(outputPath).exists()) {
                    outputPath
                } else {
                    null
                }
            }

    actual suspend fun getRotationFromFile(filePath: String): Int =
            withContext(Dispatchers.IO) {
                if (!FFmpegBinaryManager.isAvailable()) {
                    return@withContext 0
                }

                val command =
                        listOf(
                                FFmpegBinaryManager.ffprobePath(),
                                "-v",
                                "quiet",
                                "-select_streams",
                                "v:0",
                                "-show_entries",
                                "stream_side_data=rotation",
                                "-of",
                                "default=noprint_wrappers=1:nokey=1",
                                filePath
                        )

                val output = runProcessWithOutput(command)
                output.trim().toIntOrNull() ?: 0
            }

    actual suspend fun compressVideo(inputPath: String, onProgress: ((Float) -> Unit)?): String? =
            withContext(Dispatchers.IO) {
                if (!FFmpegBinaryManager.isAvailable()) {
                    println("FFmpeg binaries not available for this platform")
                    return@withContext null
                }

                val inputFile = File(inputPath)
                val outputPath =
                        "${System.getProperty("java.io.tmpdir")}/compressed_${inputFile.name}"

                val command =
                        listOf(
                                FFmpegBinaryManager.ffmpegPath(),
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

                val exitCode = runProcess(command)
                if (exitCode == 0 && File(outputPath).exists()) {
                    outputPath
                } else {
                    null
                }
            }

    actual suspend fun segmentVideo(inputPath: String): Pair<String, String>? =
            withContext(Dispatchers.IO) {
                if (!FFmpegBinaryManager.isAvailable()) {
                    println("FFmpeg binaries not available for this platform")
                    return@withContext null
                }

                val uniqueId = UUID.randomUUID().toString()
                val outputDir = File(System.getProperty("java.io.tmpdir"), "hls_$uniqueId")
                outputDir.mkdirs()

                val playlistPath = File(outputDir, "index.m3u8").absolutePath
                val segmentPattern = File(outputDir, "segment%03d.ts").absolutePath

                val command =
                        listOf(
                                FFmpegBinaryManager.ffmpegPath(),
                                "-y",
                                "-i",
                                inputPath,
                                "-c:v",
                                "libx264",
                                "-preset",
                                "veryfast",
                                "-flags",
                                "+cgop",
                                "-g",
                                "30",
                                "-hls_time",
                                "6",
                                "-hls_list_size",
                                "0",
                                "-hls_segment_filename",
                                segmentPattern,
                                playlistPath
                        )

                val exitCode = runProcess(command)
                if (exitCode == 0 && File(playlistPath).exists()) {
                    Pair(playlistPath, segmentPattern)
                } else {
                    null
                }
            }

    actual suspend fun cacheInputVideo(fileName: String, data: ByteArray): String =
            withContext(Dispatchers.IO) {
                val cacheFile = File(System.getProperty("java.io.tmpdir"), "input_$fileName")
                cacheFile.writeBytes(data)
                cacheFile.absolutePath
            }

    private fun runProcess(command: List<String>): Int {
        val processBuilder = ProcessBuilder(command).redirectErrorStream(true)

        println("Running: ${command.joinToString(" ")}")

        val process = processBuilder.start()

        // Consume output to prevent blocking
        process.inputStream.bufferedReader().use { reader ->
            reader.forEachLine { line -> println("[FFmpeg] $line") }
        }

        val completed = process.waitFor(5, TimeUnit.MINUTES)
        return if (completed) process.exitValue() else -1
    }

    private fun runProcessWithOutput(command: List<String>): String {
        val processBuilder = ProcessBuilder(command).redirectErrorStream(true)

        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().use(BufferedReader::readText)

        process.waitFor(30, TimeUnit.SECONDS)
        return output
    }
}
