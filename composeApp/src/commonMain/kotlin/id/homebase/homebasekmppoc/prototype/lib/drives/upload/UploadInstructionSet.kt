package id.homebase.homebasekmppoc.prototype.lib.drives.upload

import id.homebase.homebasekmppoc.prototype.lib.crypto.ByteArrayUtil
import id.homebase.homebasekmppoc.prototype.lib.drives.FileSystemType
import id.homebase.homebasekmppoc.prototype.lib.serialization.Base64ByteArraySerializer
import kotlinx.serialization.Serializable

/** Base upload instruction set for file uploads. */
@Serializable
data class UploadInstructionSet(
        val storageOptions: StorageOptions? = null,
        val transitOptions: TransitOptions? = null,
        @Serializable(with = Base64ByteArraySerializer::class) val transferIv: ByteArray? = null,
) {
    /**
     * Creates a serializable instruction set with manifest for sending to the server. Matches
     * TypeScript `instructionsWithManifest` pattern. Note: systemFileType is intentionally excluded
     * as it's stripped before sending.
     */
    fun toSerializable(manifest: UploadManifest): SerializableUploadInstructionSet {
        return SerializableUploadInstructionSet(
                storageOptions = storageOptions,
                transitOptions = transitOptions,
                transferIv = transferIv ?: ByteArrayUtil.getRndByteArray(16),
                manifest = manifest
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UploadInstructionSet

        if (storageOptions != other.storageOptions) return false
        if (transitOptions != other.transitOptions) return false
        if (transferIv != null) {
            if (other.transferIv == null) return false
            if (!transferIv.contentEquals(other.transferIv)) return false
        } else if (other.transferIv != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = storageOptions?.hashCode() ?: 0
        result = 31 * result + (transitOptions?.hashCode() ?: 0)
        result = 31 * result + (transferIv?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * Internal serializable instruction set with manifest for API requests. Note: systemFileType is
 * intentionally excluded as it's stripped before sending.
 */
@Serializable
data class SerializableUploadInstructionSet(
        val storageOptions: StorageOptions? = null,
        val transitOptions: TransitOptions? = null,
        @Serializable(with = Base64ByteArraySerializer::class) val transferIv: ByteArray,
        val manifest: UploadManifest
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SerializableUploadInstructionSet

        if (storageOptions != other.storageOptions) return false
        if (transitOptions != other.transitOptions) return false
        if (!transferIv.contentEquals(other.transferIv)) return false
        if (manifest != other.manifest) return false

        return true
    }

    override fun hashCode(): Int {
        var result = storageOptions?.hashCode() ?: 0
        result = 31 * result + (transitOptions?.hashCode() ?: 0)
        result = 31 * result + transferIv.contentHashCode()
        result = 31 * result + manifest.hashCode()
        return result
    }
}
