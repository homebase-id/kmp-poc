package id.homebase.homebasekmppoc.drives

import id.homebase.homebasekmppoc.core.GuidId
import id.homebase.homebasekmppoc.core.UnixTimeUtc
import kotlinx.serialization.Serializable

/**
 * Client file metadata
 * Ported from C# Odin.Services.Apps.ClientFileMetadata
 *
 * Note: Simplified version - some complex nested types are stubbed
 */
@Serializable
data class ClientFileMetadata(
    val globalTransitId: GuidId? = null,
    val created: UnixTimeUtc = UnixTimeUtc.ZeroTime,
    val updated: UnixTimeUtc = UnixTimeUtc.ZeroTime,
    val transitCreated: UnixTimeUtc = UnixTimeUtc.ZeroTime,
    val transitUpdated: UnixTimeUtc = UnixTimeUtc.ZeroTime,
    val isEncrypted: Boolean = false,
    val senderOdinId: String? = null,
    val originalAuthor: String? = null,
    val appData: AppFileMetaData = AppFileMetaData(),
    val localAppData: LocalAppMetadata? = null,
    val referencedFile: GlobalTransitIdFileIdentifier? = null,
    val reactionPreview: ReactionSummary? = null,
    val versionTag: GuidId? = null,
    val payloads: List<PayloadDescriptor>? = null,
    val dataSource: DataSource? = null
) {
    fun getPayloadDescriptor(key: String): PayloadDescriptor? {
        return payloads?.firstOrNull { it.keyEquals(key) }
    }
}

/**
 * Stub types - implement as needed based on your requirements
 */
@Serializable
data class AppFileMetaData(
    val uniqueId: GuidId? = null,
    val tags: List<GuidId>? = null,
    val fileType: Int? = null,
    val dataType: Int? = null,
    val groupId: GuidId? = null,
    val userDate: Long? = null,
    val content: String? = null,
    val previewThumbnail: ThumbnailDescriptor? = null,
    val archivalStatus: Int? = null
    // Add fields as needed from the C# AppFileMetaData
)

@Serializable
data class LocalAppMetadata(
    val versionTag: GuidId? = null
    // Add fields as needed
)

@Serializable
data class GlobalTransitIdFileIdentifier(
    val globalTransitId: GuidId,
    val targetDrive: String? = null
    // Add fields as needed
)

@Serializable
data class ReactionSummary(
    val reactions: Map<String, Int>? = null
    // Add fields as needed
)

@Serializable
data class PayloadDescriptor(
    val key: String,
    val contentType: String? = null,
    val thumbnails: List<ThumbnailDescriptor>? = null,
    val iv: String? = null,
    val bytesWritten: Long? = null,
    val lastModified: Long? = null,
    val descriptorContent: String? = null,
    val previewThumbnail: ThumbnailDescriptor? = null,
    val uid: Long? = null
    // Add fields as needed
) {
    fun keyEquals(otherKey: String): Boolean {
        return key.equals(otherKey, ignoreCase = true)
    }
}

@Serializable
data class ThumbnailDescriptor(
    val pixelWidth: Int? = null,
    val pixelHeight: Int? = null,
    val contentType: String? = null,
    val content: String? = null,
    val bytesWritten: Long? = null
)

@Serializable
data class DataSource(
    val identity: String,
    val driveId: GuidId,
    val payloadsAreRemote: Boolean = false
)
