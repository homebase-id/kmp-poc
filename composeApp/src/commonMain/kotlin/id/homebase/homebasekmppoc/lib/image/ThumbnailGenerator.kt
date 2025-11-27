package id.homebase.homebasekmppoc.lib.image

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

// Thumb presets
val baseThumbSizes = listOf(
    ThumbnailInstruction(quality = 84, maxPixelDimension = 320, maxBytes = 26 * 1024),
    ThumbnailInstruction(quality = 84, maxPixelDimension = 640, maxBytes = 102 * 1024),
    ThumbnailInstruction(quality = 76, maxPixelDimension = 1080, maxBytes = 291 * 1024),
    ThumbnailInstruction(quality = 76, maxPixelDimension = 1600, maxBytes = 640 * 1024)
)

val tinyThumbSize = ThumbnailInstruction(quality = 76, maxPixelDimension = 20, maxBytes = 768)

@OptIn(ExperimentalEncodingApi::class)
private fun toBase64(bytes: ByteArray): String = Base64.encode(bytes)

fun getRevisedThumbs(sourceSize: ImageSize, thumbs: List<ThumbnailInstruction>): List<ThumbnailInstruction> {
    val sourceMax = max(sourceSize.pixelWidth, sourceSize.pixelHeight)
    val thresholdMin = ((90 * sourceMax) / 100.0).roundToInt()
    val thresholdMax = ((110 * sourceMax) / 100.0).roundToInt()

    val keptThumbs = thumbs.filter {
        it.maxPixelDimension <= sourceMax &&
                (it.maxPixelDimension < thresholdMin || it.maxPixelDimension > thresholdMax)
    }.toMutableList()

    if (keptThumbs.size < thumbs.size) {
        val nearest = thumbs.minByOrNull { abs(it.maxPixelDimension - sourceMax) }
        val maxBytes = if (nearest != null) {
            val scale = sourceMax.toDouble() / nearest.maxPixelDimension.toDouble()
            var candidate = (nearest.maxBytes * scale).roundToInt()
            candidate = candidate.coerceIn(10 * 1024, 1024 * 1024)
            candidate
        } else {
            300 * 1024
        }

        val q = if (sourceMax <= 640) 84 else 76
        keptThumbs.add(ThumbnailInstruction(quality = q, maxPixelDimension = sourceMax, maxBytes = maxBytes))
    }

    return keptThumbs.sortedBy { it.maxPixelDimension }
}

/**
 * Main entry — mirrors createThumbnails in JS
 */
suspend fun createThumbnails(
    imageBytes: ByteArray,
    payloadKey: String,
    thumbSizes: List<ThumbnailInstruction>? = null
): Triple<ImageSize, EmbeddedThumb, List<ThumbnailFile>> = withContext(Dispatchers.Default) {

    // Determine natural size using ImageUtils
    val naturalSize = ImageUtils.getNaturalSize(imageBytes)

    // GIF and SVG handling: for SVG files we expect bytes to be svg xml; for GIFs we treat them specially
    // Here we try simple detection by header bytes
    val header = imageBytes.take(16).toByteArray().decodeToString().lowercase()
    val isSvg = header.contains("<svg") || header.contains("<?xml")
    val isGif = imageBytes.size >= 3 && imageBytes[0] == 0x47.toByte() /* G */ && imageBytes[1] == 0x49.toByte() /* I */ && imageBytes[2] == 0x46.toByte() /* F */

    if (isSvg) {
        // For SVG, we return the original vector format
        val vectorThumb = ThumbnailFile(pixelWidth = 50, pixelHeight = 50, payload = imageBytes, key = payloadKey)
        val embedded = EmbeddedThumb(
            pixelWidth = naturalSize.pixelWidth,
            pixelHeight = naturalSize.pixelHeight,
            contentType = "image/svg+xml",
            contentBase64 = toBase64(vectorThumb.payload)
        )

        return@withContext Triple(naturalSize, embedded, listOf(vectorThumb))
    }

    if (isGif) {
        // For GIF, create tiny thumb only (webp) and possibly include original if < 1MB
        val tinyThumbFile = createImageThumbnail(imageBytes, payloadKey, tinyThumbSize, isTinyThumb = true)
        val includeOriginal = imageBytes.size < 1024 * 1024
        val additional = mutableListOf<ThumbnailFile>()
        if (includeOriginal) {
            additional.add(
                ThumbnailFile(
                    pixelWidth = naturalSize.pixelWidth,
                    pixelHeight = naturalSize.pixelHeight,
                    payload = imageBytes,
                    key = payloadKey,
                    contentType = "image/gif",
                    quality = 100
                )
            )
        }
        val embeddedTiny = EmbeddedThumb(
            pixelWidth = naturalSize.pixelWidth,
            pixelHeight = naturalSize.pixelHeight,
            contentType = "image/webp",
            contentBase64 = toBase64(tinyThumbFile.payload)
        )
        return@withContext Triple(naturalSize, embeddedTiny, additional)
    }

    // general image case
    val tinyThumbFile = createImageThumbnail(imageBytes, payloadKey, tinyThumbSize, isTinyThumb = true)

    val requestedSizes = thumbSizes ?: baseThumbSizes
    val applicableThumbs = getRevisedThumbs(naturalSize, requestedSizes)

    // Create additional thumbnails (tiny + requested ones)
    val additional = mutableListOf<ThumbnailFile>()
    additional.add(tinyThumbFile)

    val produced = applicableThumbs.map { instr ->
        // create with no tiny flag
        createImageThumbnail(imageBytes, payloadKey, instr, isTinyThumb = false)
    }
    additional.addAll(produced)

    val embeddedTiny = EmbeddedThumb(
        pixelWidth = tinyThumbFile.pixelWidth,
        pixelHeight = tinyThumbFile.pixelHeight,
        contentType = "image/webp",
        contentBase64 = toBase64(tinyThumbFile.payload)
    )

    return@withContext Triple(naturalSize, embeddedTiny, additional)
}

/**
 * Create a single thumbnail according to instruction.
 * - Resizes to fit inside maxPixelDimension x maxPixelDimension preserving aspect.
 * - Tries initial quality; if resulting size > maxBytes it reduces quality in a loop until satisfying or quality==1.
 * - Returns ThumbnailFile (payload bytes).
 */
suspend fun createImageThumbnail(
    imageBytes: ByteArray,
    payloadKey: String,
    instruction: ThumbnailInstruction,
    isTinyThumb: Boolean = false
): ThumbnailFile = withContext(Dispatchers.Default) {
    // Determine target format (tiny -> webp forced)
    val targetFormat = if (isTinyThumb) ImageFormat.WEBP else instruction.type

    val maxDim = instruction.maxPixelDimension

    var quality = instruction.quality.coerceIn(1, 100)
    var currentInputBytes = imageBytes

    // First resize attempt
    var result = ImageUtils.resizePreserveAspect(
        currentInputBytes,
        maxWidth = maxDim,
        maxHeight = maxDim,
        outputFormat = targetFormat,
        quality = quality
    )

    var safetyCounter = 0
    while (result.bytes.size > instruction.maxBytes && quality > 1 && safetyCounter < 20) {
        safetyCounter++

        // Adjust quality drop proportional to excess
        val excessRatio = result.bytes.size.toDouble() / instruction.maxBytes.toDouble()
        val qualityDrop = minOf(40, maxOf(5, (quality * excessRatio * 0.5).roundToInt()))
        quality = (quality - qualityDrop).coerceAtLeast(1)

        // Use previous result as input for next compression attempt
        currentInputBytes = result.bytes
        result = ImageUtils.compressOnly(
            currentInputBytes,
            outputFormat = targetFormat,
            quality = quality
        )

        // If still too big for tiny thumb and quality reached near 1, try shrinking pixel dimensions
        if (result.bytes.size > instruction.maxBytes && quality <= 2 && isTinyThumb) {
            val newDim = maxOf(1, maxDim - 5)
            if (newDim < 1) {
                throw IllegalStateException("Cannot shrink tiny thumb further")
            }

            // Try with smaller dimensions
            result = ImageUtils.resizePreserveAspect(
                imageBytes,
                maxWidth = newDim,
                maxHeight = newDim,
                outputFormat = targetFormat,
                quality = 2
            )
            break
        }
    }

    val finalBytes = result.bytes
    val thumb = ThumbnailFile(
        pixelWidth = result.size.pixelWidth,
        pixelHeight = result.size.pixelHeight,
        payload = finalBytes,
        key = payloadKey,
        contentType = when (targetFormat) {
            ImageFormat.WEBP -> "image/webp"
            ImageFormat.JPEG -> "image/jpeg"
            ImageFormat.PNG -> "image/png"
            ImageFormat.BMP -> "image/bmp"
            ImageFormat.GIF -> "image/gif"
        },
        quality = quality
    )

    return@withContext thumb
}
