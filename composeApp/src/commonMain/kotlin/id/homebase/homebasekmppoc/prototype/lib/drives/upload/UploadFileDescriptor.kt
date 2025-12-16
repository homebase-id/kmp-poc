package id.homebase.homebasekmppoc.prototype.lib.drives.upload

import id.homebase.homebasekmppoc.prototype.lib.crypto.EncryptedKeyHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.AccessControlList
import id.homebase.homebasekmppoc.prototype.lib.drives.files.ArchivalStatus
import id.homebase.homebasekmppoc.prototype.lib.drives.files.GlobalTransitIdFileIdentifier
import id.homebase.homebasekmppoc.prototype.lib.serialization.Base64ByteArraySerializer
import kotlinx.serialization.Serializable

/**
 * Embedded thumbnail for preview in upload context. This is a serializable DTO version that differs
 * from the lib/image EmbeddedThumb which uses contentBase64 field name.
 */
@Serializable
data class EmbeddedThumb(
        val pixelWidth: Int,
        val pixelHeight: Int,
        val contentType: String,
        val content: String
)

/** Application file metadata for uploads. */
@Serializable
data class UploadAppFileMetaData(
        val uniqueId: String? = null,
        val tags: List<String>? = null,
        val fileType: Int? = null,
        val dataType: Int? = null,
        val userDate: Long? = null,
        val groupId: String? = null,
        val archivalStatus: ArchivalStatus? = null,
        val content: String? = null,
        val previewThumbnail: EmbeddedThumb? = null
)

/** File metadata for uploads. */
@Serializable
data class UploadFileMetadata(
        val allowDistribution: Boolean,
        val isEncrypted: Boolean,
        val accessControlList: AccessControlList? = null,
        val appData: UploadAppFileMetaData,
        val referencedFile: GlobalTransitIdFileIdentifier? = null,
        val versionTag: String? = null
)

/** File descriptor for uploads. */
@Serializable
data class UploadFileDescriptor(
        val encryptedKeyHeader: EncryptedKeyHeader? = null,
        val fileMetadata: UploadFileMetadata
)

/**
 * Serializable key header for upload results. This is a DTO version meant for API response
 * deserialization. For actual cryptographic operations, use the crypto.KeyHeader class.
 */
@Serializable
data class UploadKeyHeader(
        @Serializable(with = Base64ByteArraySerializer::class) val iv: ByteArray? = null,
        @Serializable(with = Base64ByteArraySerializer::class) val aesKey: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UploadKeyHeader

        if (iv != null) {
            if (other.iv == null) return false
            if (!iv.contentEquals(other.iv)) return false
        } else if (other.iv != null) return false
        if (aesKey != null) {
            if (other.aesKey == null) return false
            if (!aesKey.contentEquals(other.aesKey)) return false
        } else if (other.aesKey != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = iv?.contentHashCode() ?: 0
        result = 31 * result + (aesKey?.contentHashCode() ?: 0)
        return result
    }
}
