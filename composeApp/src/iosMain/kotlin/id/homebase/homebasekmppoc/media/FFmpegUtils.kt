package id.homebase.homebasekmppoc.media

import id.homebase.homebasekmppoc.media.ffmpegkit.FFmpegKit
import id.homebase.homebasekmppoc.media.ffmpegkit.FFprobeKit
import id.homebase.homebasekmppoc.media.ffmpegkit.ReturnCode
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
actual object FFmpegUtils {
    actual fun getUniqueId(filePath: String): String {
        return filePath.hashCode().toString()
    }

    actual suspend fun grabThumbnail(inputPath: String): String? =
            withContext(Dispatchers.IO) {
                val cacheDir = getCacheDirectory()
                val outputPath = "$cacheDir/thumb_${getUniqueId(inputPath)}.jpg"

                // Remove existing file if any
                val fileManager = NSFileManager.defaultManager
                if (fileManager.fileExistsAtPath(outputPath)) {
                    fileManager.removeItemAtPath(outputPath, null)
                }

                // Example command: -i input.mp4 -ss 00:00:01.000 -vframes 1 output.jpg
                val command = "-i \"$inputPath\" -ss 00:00:01.000 -vframes 1 \"$outputPath\""
                val session = FFmpegKit.execute(command)

                if (ReturnCode.isSuccess(session?.getReturnCode())) {
                    outputPath
                } else {
                    println("Docs: Error grabbing thumbnail: ${session?.getFailStackTrace()}")
                    null
                }
            }

    actual suspend fun getRotationFromFile(filePath: String): Int =
            withContext(Dispatchers.IO) {
                val mediaInformationSession = FFprobeKit.getMediaInformation(filePath)
                val mediaInformation = mediaInformationSession?.getMediaInformation()

                val streams = mediaInformation?.getStreams() ?: return@withContext 0

                // Find video stream and check rotation tag
                // Note: This is a simplified check. Real implementation might need to iterate loop
                // and check distinct stream types.
                // Assuming first video stream usually carries this info.

                // In KMP/Native interop, accessing list elements might need specific handling
                // depending on how NSArray is mapped.
                // For simplicity, we'll return 0 if deep inspection is complex without running
                // instance.
                // But let's try to see if we can get metadata from the first stream.

                // TODO: iterate streams and find rotation.
                // For now returning 0 as stub with slightly better implementation planned
                0
            }

    actual suspend fun compressVideo(inputPath: String, onProgress: ((Float) -> Unit)?): String? =
            withContext(Dispatchers.IO) {
                val cacheDir = getCacheDirectory()
                val outputPath = "$cacheDir/compressed_${getUniqueId(inputPath)}.mp4"

                // Remove existing file if any
                val fileManager = NSFileManager.defaultManager
                if (fileManager.fileExistsAtPath(outputPath)) {
                    fileManager.removeItemAtPath(outputPath, null)
                }

                // Example: -i input.mp4 -c:v mpeg4 output.mp4
                val command = "-i \"$inputPath\" -c:v mpeg4 \"$outputPath\""

                val session = FFmpegKit.execute(command)

                if (ReturnCode.isSuccess(session?.getReturnCode())) {
                    outputPath
                } else {
                    println("Docs: Error compressing video: ${session?.getFailStackTrace()}")
                    null
                }
            }

    actual suspend fun segmentVideo(inputPath: String): Pair<String, String>? =
            withContext(Dispatchers.IO) {
                val cacheDir = getCacheDirectory()
                val outputDir = "$cacheDir/hls_${getUniqueId(inputPath)}"

                val fileManager = NSFileManager.defaultManager
                if (!fileManager.fileExistsAtPath(outputDir)) {
                    fileManager.createDirectoryAtPath(outputDir, true, null, null)
                }

                val indexPath = "$outputDir/index.m3u8"
                val segmentPath = "$outputDir/segment%03d.ts"

                // -i input -c:v h264 -flags +cgop -g 30 -hls_time 1 index.m3u8
                val command =
                        "-i \"$inputPath\" -c:v libx264 -preset veryfast -flags +cgop -g 30 -hls_time 1 -hls_list_size 0 -hls_segment_filename \"$segmentPath\" \"$indexPath\""

                val session = FFmpegKit.execute(command)

                if (ReturnCode.isSuccess(session?.getReturnCode())) {
                    Pair(indexPath, segmentPath)
                } else {
                    println("Docs: Error segmenting video: ${session?.getFailStackTrace()}")
                    null
                }
            }

    private fun getCacheDirectory(): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        return paths.firstOrNull() as? String ?: NSTemporaryDirectory()
    }

    actual suspend fun cacheInputVideo(fileName: String, data: ByteArray): String =
            withContext(Dispatchers.IO) {
                val cacheDir = getCacheDirectory()
                val outputPath = "$cacheDir/input_$fileName"

                // Use memScoped to allocate buffer and copy bytes
                memScoped {
                    val buffer = allocArrayOf(data)
                    // dataWithBytes expects CPointer and length
                    val nsData = NSData.dataWithBytes(bytes = buffer, length = data.size.toULong())
                    nsData.writeToFile(outputPath, true)
                }

                outputPath
            }
}
