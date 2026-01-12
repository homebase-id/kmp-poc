package id.homebase.homebasekmppoc.media

import android.util.Log

/**
 * Native FFmpeg bindings for Android. Provides direct access to FFmpeg C library functions via JNI.
 */
object FFmpegNative {
    private const val TAG = "FFmpegNative"
    private var isInitialized = false
    private var loadError: String? = null

    init {
        try {
            // Load FFmpeg libraries in dependency order
            System.loadLibrary("avutil")
            System.loadLibrary("swresample")
            System.loadLibrary("avcodec")
            System.loadLibrary("avformat")
            System.loadLibrary("swscale")
            System.loadLibrary("avfilter")
            // Load our JNI wrapper
            System.loadLibrary("ffmpeg_jni")

            // Initialize FFmpeg
            val result = nativeInit()
            isInitialized = (result == 0)
            if (isInitialized) {
                Log.i(TAG, "FFmpeg initialized successfully: ${getVersion()}")
            } else {
                loadError = "Native init returned $result"
            }
        } catch (e: UnsatisfiedLinkError) {
            loadError = e.message
            Log.e(TAG, "Failed to load FFmpeg libraries", e)
        }
    }

    fun isAvailable(): Boolean = isInitialized

    fun getLoadError(): String? = loadError

    // ========================================================================
    // Native method declarations
    // ========================================================================

    @JvmStatic private external fun nativeInit(): Int

    @JvmStatic external fun getVersion(): String

    /** Get media duration in microseconds. */
    @JvmStatic external fun getMediaDuration(path: String): Long

    /** Get video rotation from metadata (0, 90, 180, 270). */
    @JvmStatic external fun getVideoRotation(path: String): Int

    /** Get video dimensions as [width, height], or null on error. */
    @JvmStatic external fun getVideoDimensions(path: String): IntArray?

    /**
     * Extract a thumbnail frame at the specified time.
     * @param inputPath Path to video file
     * @param outputPath Path for output JPEG
     * @param timeSeconds Time position in seconds
     * @return 0 on success, -1 on error
     */
    @JvmStatic
    external fun extractThumbnail(inputPath: String, outputPath: String, timeSeconds: Double): Int

    /**
     * Compress video using H.264 encoder.
     * @param inputPath Path to input video
     * @param outputPath Path for output video
     * @param targetBitrate Target video bitrate in bits/sec (e.g., 3000000 for 3Mbps)
     * @param maxWidth Maximum output width (height scales proportionally). 0 = no scaling.
     * @return 0 on success, -1 on error
     */
    @JvmStatic
    external fun compressVideo(
            inputPath: String,
            outputPath: String,
            targetBitrate: Int,
            maxWidth: Int
    ): Int

    /**
     * Segment video into HLS format.
     * @param inputPath Path to input video
     * @param playlistPath Path for .m3u8 playlist output
     * @param segmentDuration Segment duration in seconds
     * @return 0 on success, -1 on error
     */
    @JvmStatic
    external fun segmentToHLS(inputPath: String, playlistPath: String, segmentDuration: Int): Int
}
