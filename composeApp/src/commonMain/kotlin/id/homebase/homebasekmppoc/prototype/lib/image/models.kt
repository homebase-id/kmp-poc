package id.homebase.homebasekmppoc.prototype.lib.image

data class ImageSize(val pixelWidth: Int, val pixelHeight: Int)

data class ImageResult(
    val bytes: ByteArray,
    val naturalSize: ImageSize, // original image size
    val size: ImageSize // resulting (updated) image size
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ImageResult

        if (!bytes.contentEquals(other.bytes)) return false
        if (naturalSize != other.naturalSize) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + naturalSize.hashCode()
        result = 31 * result + size.hashCode()
        return result
    }
}

data class ThumbnailInstruction(
    val quality: Int,
    val maxPixelDimension: Int,
    val maxBytes: Int,
    val type: ImageFormat = ImageFormat.WEBP
)

enum class ImageFormat {
    WEBP, JPEG, PNG, BMP, GIF
}

data class ThumbnailFile(
    val pixelWidth: Int,
    val pixelHeight: Int,
    val payload: ByteArray, // raw bytes -> equivalent to Blob
    val key: String,
    val contentType: String = "image/webp",
    val quality: Int = 76
) {
    // Convenience property to match test expectations
    val imageSize: ImageSize
        get() = ImageSize(pixelWidth, pixelHeight)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ThumbnailFile

        if (pixelWidth != other.pixelWidth) return false
        if (pixelHeight != other.pixelHeight) return false
        if (!payload.contentEquals(other.payload)) return false
        if (key != other.key) return false
        if (contentType != other.contentType) return false
        if (quality != other.quality) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pixelWidth
        result = 31 * result + pixelHeight
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + key.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + quality
        return result
    }
}

data class EmbeddedThumb(
    val pixelWidth: Int,
    val pixelHeight: Int,
    val contentType: String,
    val contentBase64: String
)

