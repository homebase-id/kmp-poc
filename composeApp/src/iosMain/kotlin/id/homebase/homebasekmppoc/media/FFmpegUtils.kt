package id.homebase.homebasekmppoc.media

import id.homebase.homebasekmppoc.media.ffmpeg.*
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.*
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

@OptIn(ExperimentalForeignApi::class)
actual object FFmpegUtils {

    actual fun getUniqueId(filePath: String): String {
        return filePath.hashCode().toString()
    }

    actual suspend fun grabThumbnail(inputPath: String): String? =
            withContext(Dispatchers.IO) {
                val cacheDir = getCacheDirectory()
                val outputPath = "$cacheDir/thumb_${getUniqueId(inputPath)}.jpg"

                val fileManager = NSFileManager.defaultManager
                if (fileManager.fileExistsAtPath(outputPath)) {
                    return@withContext outputPath
                }

                fileManager.removeItemAtPath(outputPath, null)

                memScoped {
                    val fmtCtxPtr = alloc<CPointerVar<AVFormatContext>>()

                    // Open input
                    if (avformat_open_input(fmtCtxPtr.ptr, inputPath, null, null) < 0) {
                        println("FFmpeg: Could not open input: $inputPath")
                        return@withContext null
                    }
                    val fmtCtx = fmtCtxPtr.value

                    if (avformat_find_stream_info(fmtCtx, null) < 0) {
                        avformat_close_input(fmtCtxPtr.ptr)
                        return@withContext null
                    }

                    // Find video stream
                    var videoStreamIdx = -1
                    val nbStreams = fmtCtx?.pointed?.nb_streams ?: 0u
                    for (i in 0u until nbStreams) {
                        val stream = fmtCtx?.pointed?.streams?.get(i.toInt())
                        if (stream?.pointed?.codecpar?.pointed?.codec_type == AVMEDIA_TYPE_VIDEO) {
                            videoStreamIdx = i.toInt()
                            break
                        }
                    }

                    if (videoStreamIdx < 0) {
                        avformat_close_input(fmtCtxPtr.ptr)
                        return@withContext null
                    }

                    val videoStream = fmtCtx?.pointed?.streams?.get(videoStreamIdx)
                    val codecpar = videoStream?.pointed?.codecpar
                    val codecId = codecpar?.pointed?.codec_id ?: 0u

                    // Find decoder
                    val decoder = avcodec_find_decoder(codecId)
                    if (decoder == null) {
                        avformat_close_input(fmtCtxPtr.ptr)
                        return@withContext null
                    }

                    // Allocate decoder context
                    val decCtx = avcodec_alloc_context3(decoder)
                    if (decCtx == null) {
                        avformat_close_input(fmtCtxPtr.ptr)
                        return@withContext null
                    }

                    avcodec_parameters_to_context(decCtx, codecpar)

                    if (avcodec_open2(decCtx, decoder, null) < 0) {
                        avcodec_free_context(cValuesOf(decCtx))
                        avformat_close_input(fmtCtxPtr.ptr)
                        return@withContext null
                    }

                    val width = decCtx.pointed.width
                    val height = decCtx.pointed.height
                    val pixFmt = decCtx.pointed.pix_fmt

                    // Setup MJPEG encoder
                    val encoder = avcodec_find_encoder(AV_CODEC_ID_MJPEG)
                    if (encoder == null) {
                        avcodec_free_context(cValuesOf(decCtx))
                        avformat_close_input(fmtCtxPtr.ptr)
                        return@withContext null
                    }

                    val encCtx = avcodec_alloc_context3(encoder)
                    if (encCtx == null) {
                        avcodec_free_context(cValuesOf(decCtx))
                        avformat_close_input(fmtCtxPtr.ptr)
                        return@withContext null
                    }

                    encCtx.pointed.width = width
                    encCtx.pointed.height = height
                    encCtx.pointed.pix_fmt = AV_PIX_FMT_YUVJ420P
                    encCtx.pointed.time_base.num = 1
                    encCtx.pointed.time_base.den = 25

                    if (avcodec_open2(encCtx, encoder, null) < 0) {
                        avcodec_free_context(cValuesOf(encCtx))
                        avcodec_free_context(cValuesOf(decCtx))
                        avformat_close_input(fmtCtxPtr.ptr)
                        return@withContext null
                    }

                    // Setup scaler
                    val swsCtx =
                            sws_getContext(
                                    width,
                                    height,
                                    pixFmt,
                                    width,
                                    height,
                                    AV_PIX_FMT_YUVJ420P,
                                    SWS_BILINEAR,
                                    null,
                                    null,
                                    null
                            )

                    // Allocate frames
                    val frame = av_frame_alloc()
                    val rgbFrame = av_frame_alloc()
                    val pkt = av_packet_alloc()

                    if (frame == null || rgbFrame == null || pkt == null) {
                        sws_freeContext(swsCtx)
                        avcodec_free_context(cValuesOf(encCtx))
                        avcodec_free_context(cValuesOf(decCtx))
                        avformat_close_input(fmtCtxPtr.ptr)
                        return@withContext null
                    }

                    rgbFrame.pointed.format = AV_PIX_FMT_YUVJ420P
                    rgbFrame.pointed.width = width
                    rgbFrame.pointed.height = height
                    av_frame_get_buffer(rgbFrame, 0)

                    // Seek to 1 second
                    av_seek_frame(fmtCtx, -1, AV_TIME_BASE.toLong(), AVSEEK_FLAG_BACKWARD)

                    // Read and decode one frame
                    var gotFrame = false
                    while (!gotFrame && av_read_frame(fmtCtx, pkt) >= 0) {
                        if (pkt.pointed.stream_index == videoStreamIdx) {
                            if (avcodec_send_packet(decCtx, pkt) == 0) {
                                if (avcodec_receive_frame(decCtx, frame) == 0) {
                                    gotFrame = true
                                }
                            }
                        }
                        av_packet_unref(pkt)
                    }

                    var success = false

                    if (gotFrame) {
                        // Scale frame
                        sws_scale(
                                swsCtx,
                                frame.pointed.data.reinterpret(),
                                frame.pointed.linesize.reinterpret(),
                                0,
                                height,
                                rgbFrame.pointed.data.reinterpret(),
                                rgbFrame.pointed.linesize.reinterpret()
                        )

                        rgbFrame.pointed.pts = 0

                        // Encode to JPEG
                        if (avcodec_send_frame(encCtx, rgbFrame) == 0) {
                            val encPkt = av_packet_alloc()
                            if (encPkt != null && avcodec_receive_packet(encCtx, encPkt) == 0) {
                                // Write to file
                                val file = fopen(outputPath, "wb")
                                if (file != null) {
                                    fwrite(
                                            encPkt.pointed.data,
                                            1u,
                                            encPkt.pointed.size.toULong(),
                                            file
                                    )
                                    fclose(file)
                                    println("FFmpeg: Thumbnail saved to $outputPath")
                                    success = true
                                }
                                av_packet_free(cValuesOf(encPkt))
                            }
                        }
                    }

                    // Cleanup
                    av_packet_free(cValuesOf(pkt))
                    av_frame_free(cValuesOf(frame))
                    av_frame_free(cValuesOf(rgbFrame))
                    sws_freeContext(swsCtx)
                    avcodec_free_context(cValuesOf(encCtx))
                    avcodec_free_context(cValuesOf(decCtx))
                    avformat_close_input(fmtCtxPtr.ptr)

                    if (success) outputPath else null
                }
            }

    actual suspend fun getRotationFromFile(filePath: String): Int =
            withContext(Dispatchers.IO) {
                memScoped {
                    val fmtCtxPtr = alloc<CPointerVar<AVFormatContext>>()
                    if (avformat_open_input(fmtCtxPtr.ptr, filePath, null, null) < 0) {
                        return@withContext 0
                    }
                    val fmtCtx = fmtCtxPtr.value

                    if (avformat_find_stream_info(fmtCtx, null) < 0) {
                        avformat_close_input(fmtCtxPtr.ptr)
                        return@withContext 0
                    }

                    var rotation = 0
                    val nbStreams = fmtCtx?.pointed?.nb_streams ?: 0u
                    for (i in 0u until nbStreams) {
                        val stream = fmtCtx?.pointed?.streams?.get(i.toInt())
                        if (stream?.pointed?.codecpar?.pointed?.codec_type == AVMEDIA_TYPE_VIDEO) {
                            val metadata = stream.pointed.metadata
                            val tag = av_dict_get(metadata, "rotate", null, 0)
                            if (tag != null) {
                                val valueStr = tag.pointed.value?.toKString()
                                rotation = valueStr?.toIntOrNull() ?: 0
                            }
                            break
                        }
                    }

                    avformat_close_input(fmtCtxPtr.ptr)
                    rotation
                }
            }

    actual suspend fun compressVideo(inputPath: String, onProgress: ((Float) -> Unit)?): String? =
            withContext(Dispatchers.IO) {
                val cacheDir = getCacheDirectory()
                val outputPath = "$cacheDir/compressed_${getUniqueId(inputPath)}.mp4"

                val fileManager = NSFileManager.defaultManager
                fileManager.removeItemAtPath(outputPath, null)

                // Full video compression with H.264 via raw FFmpeg C API
                // requires significant setup. For now, copy file.
                // TODO: Implement full H.264 transcoding
                println("FFmpeg iOS: Video compression - copying input for now")

                try {
                    fileManager.copyItemAtPath(inputPath, outputPath, null)
                    if (fileManager.fileExistsAtPath(outputPath)) {
                        return@withContext outputPath
                    }
                } catch (e: Exception) {
                    println("FFmpeg: Compression error: ${e.message}")
                }

                null
            }

    actual suspend fun segmentVideo(inputPath: String): Pair<String, String>? =
            withContext(Dispatchers.IO) {
                // HLS segmentation via raw FFmpeg C API
                // TODO: Implement using avformat muxer with HLS format
                println("FFmpeg iOS: HLS segmentation not yet implemented via C API")
                null
            }

    private fun getCacheDirectory(): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        return paths.firstOrNull() as? String ?: NSTemporaryDirectory()
    }

    actual suspend fun cacheInputVideo(fileName: String, data: ByteArray): String =
            withContext(Dispatchers.IO) {
                val cacheDir = getCacheDirectory()
                val outputPath = "$cacheDir/input_$fileName"

                memScoped {
                    val buffer = allocArrayOf(data)
                    val nsData = NSData.dataWithBytes(bytes = buffer, length = data.size.toULong())
                    nsData.writeToFile(outputPath, true)
                }

                outputPath
            }
}
