package id.homebase.homebasekmppoc.prototype.lib.drives.upload

import id.homebase.homebasekmppoc.prototype.lib.drives.ThumbnailDescriptor
import kotlinx.serialization.Serializable

/** Payload descriptor for uploads. */
@Serializable
data class UploadPayloadDescriptor(
        val payloadKey: String,
        val descriptorContent: String? = null,
        val contentType: String? = null,
        val previewThumbnail: ThumbnailDescriptor? = null,
        val thumbnails: List<UploadThumbnailDescriptor>? = null
)

/** Thumbnail descriptor for upload payloads. */
@Serializable
data class UploadThumbnailDescriptor(
        val thumbnailKey: String,
        val pixelWidth: Int,
        val pixelHeight: Int,
        val contentType: String
)

/** Upload manifest containing payload descriptors. */
@Serializable
data class UploadManifest(val payloadDescriptors: List<UploadPayloadDescriptor>? = null)

/** Update payload instruction. */
@Serializable
data class UpdatePayloadInstruction(
        val payloadKey: String,
        val operationType: PayloadOperationType,
        val descriptorContent: String? = null,
        val contentType: String? = null,
        val previewThumbnail: ThumbnailDescriptor? = null,
        val thumbnails: List<UploadThumbnailDescriptor>? = null
)

/** Payload operation types for updates. */
@Serializable
enum class PayloadOperationType {
    AppendOrOverwrite,
    DeletePayload
}

/** Update manifest containing payload update instructions. */
@Serializable
data class UpdateManifest(val payloadDescriptors: List<UpdatePayloadInstruction>? = null)
