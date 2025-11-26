@file:OptIn(ExperimentalUuidApi::class)

package id.homebase.homebasekmppoc.drives

import id.homebase.homebasekmppoc.core.time.UnixTimeUtc
import id.homebase.homebasekmppoc.serialization.UuidSerializer
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Client file metadata
 * Ported from C# Odin.Services.Apps.ClientFileMetadata
 *
 * Note: Simplified version - some complex nested types are stubbed
 */
@Serializable
data class ClientFileMetadata(
    @Serializable(with = UuidSerializer::class)
    val globalTransitId: Uuid? = null,
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
    @Serializable(with = UuidSerializer::class)
    val versionTag: Uuid? = null,
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
    @Serializable(with = UuidSerializer::class)
    val uniqueId: Uuid? = null,
    val tags: List<@Serializable(with = UuidSerializer::class) Uuid>? = null,
    val fileType: Int? = null,
    val dataType: Int? = null,
    @Serializable(with = UuidSerializer::class)
    val groupId: Uuid? = null,
    val userDate: Long? = null,
    val content: String? = null,
    val previewThumbnail: ThumbnailDescriptor? = null,
    val archivalStatus: Int? = null
    // Add fields as needed from the C# AppFileMetaData
)

@Serializable
data class LocalAppMetadata(
    @Serializable(with = UuidSerializer::class)
    val versionTag: Uuid? = null
    // Add fields as needed
)

/**
 * Drive and file info which identifies a file using a GlobalTransitId.
 * Used externally to the host (can be sent to clients).
 */
@Serializable
data class GlobalTransitIdFileIdentifier(
    /**
     * The drive to access
     */
    val targetDrive: TargetDrive,

    /**
     * The global transit id to retrieve
     */
    @Serializable(with = UuidSerializer::class)
    val globalTransitId: Uuid
) {
    /**
     * Checks if this identifier has valid values.
     */
    fun hasValue(): Boolean {
        return globalTransitId != Uuid.NIL && targetDrive.isValid()
    }

    /**
     * Converts this GlobalTransitIdFileIdentifier to a FileIdentifier.
     */
    fun toFileIdentifier(): FileIdentifier {
        return FileIdentifier(
            globalTransitId = globalTransitId,
            targetDrive = targetDrive
        )
    }
}

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
    @Serializable(with = UuidSerializer::class)
    val driveId: Uuid,
    val payloadsAreRemote: Boolean = false
)
