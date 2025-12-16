package id.homebase.homebasekmppoc.prototype.lib.drives.files

import id.homebase.homebasekmppoc.prototype.lib.drives.upload.EmbeddedThumb
import kotlinx.serialization.Serializable

/** Media file reference for existing files. Ported from TypeScript MediaFile interface. */
@Serializable
data class MediaFile(val fileId: String? = null, val key: String, val contentType: String)

/** New media file for upload. Note: payload uses ByteArray instead of File/Blob. */
@Serializable
data class NewMediaFile(
        val key: String? = null,
        val payload: ByteArray,
        val thumbnailKey: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as NewMediaFile

        if (key != other.key) return false
        if (!payload.contentEquals(other.payload)) return false
        if (thumbnailKey != other.thumbnailKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key?.hashCode() ?: 0
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + (thumbnailKey?.hashCode() ?: 0)
        return result
    }
}

/**
 * Base payload file for upload. Ported from TypeScript BasePayloadFile interface. Note: Uses
 * ByteArray for payload instead of File/Blob.
 */
@Serializable
data class PayloadFile(
        val key: String,
        val payload: ByteArray,
        val previewThumbnail: EmbeddedThumb? = null,
        val descriptorContent: String? = null,
        val skipEncryption: Boolean = false,
        /** IV for manual encryption mode (when skipEncryption = true). */
        val iv: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PayloadFile

        if (key != other.key) return false
        if (!payload.contentEquals(other.payload)) return false
        if (previewThumbnail != other.previewThumbnail) return false
        if (descriptorContent != other.descriptorContent) return false
        if (skipEncryption != other.skipEncryption) return false
        if (iv != null) {
            if (other.iv == null) return false
            if (!iv.contentEquals(other.iv)) return false
        } else if (other.iv != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + (previewThumbnail?.hashCode() ?: 0)
        result = 31 * result + (descriptorContent?.hashCode() ?: 0)
        result = 31 * result + skipEncryption.hashCode()
        result = 31 * result + (iv?.contentHashCode() ?: 0)
        return result
    }
}
