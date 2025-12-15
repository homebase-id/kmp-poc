package id.homebase.homebasekmppoc.prototype.lib.drives.files

import id.homebase.homebasekmppoc.prototype.lib.drives.ArchivalStatus
import id.homebase.homebasekmppoc.prototype.lib.drives.ThumbnailDescriptor
import id.homebase.homebasekmppoc.prototype.lib.serialization.UuidSerializer
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/** New file metadata for creating files. Ported from TypeScript NewFileMetadata interface. */
@Serializable
data class NewFileMetadata(
        val created: Long? = null,
        val updated: Long? = null,
        val transitCreated: Long? = null,
        val transitUpdated: Long? = null,
        @Serializable(with = UuidSerializer::class) val globalTransitId: Uuid? = null,
        val isEncrypted: Boolean = false,
        val originalAuthor: String? = null,
        val senderOdinId: String? = null,
        val appData: NewAppFileMetaData,
        val versionTag: String? = null,
        val payloads: List<NewPayloadDescriptor>? = null
)

/**
 * New app file metadata for creating files. Ported from TypeScript NewAppFileMetaData interface.
 */
@Serializable
data class NewAppFileMetaData(
        val content: String,
        val fileType: Int? = null,
        val dataType: Int? = null,
        val groupId: String? = null,
        val userDate: Long? = null,
        val tags: List<String>? = null,
        val uniqueId: String? = null,
        val previewThumbnail: PayloadEmbeddedThumb? = null,
        val archivalStatus: ArchivalStatus? = null
)

/**
 * New payload descriptor with pending file support. Ported from TypeScript NewPayloadDescriptor
 * interface.
 */
@Serializable
data class NewPayloadDescriptor(
        val key: String? = null,
        val descriptorContent: String? = null,
        val contentType: String? = null,
        val bytesWritten: Long? = null,
        val lastModified: Long? = null,
        val thumbnails: List<ThumbnailDescriptor>? = null,
        val previewThumbnail: ThumbnailDescriptor? = null,
        val iv: String? = null,

        /** Pending file URL for lazy loading. */
        val pendingFileUrl: String? = null,

        /** Upload progress tracking. */
        val uploadProgress: UploadProgress? = null
)

/** Upload progress tracking. */
@Serializable data class UploadProgress(val phase: String? = null, val progress: Double? = null)

/** New Homebase file for creation. Ported from TypeScript NewHomebaseFile interface. */
@Serializable
data class NewHomebaseFile(
        @Serializable(with = UuidSerializer::class) val fileId: Uuid? = null,
        val fileSystemType: String? = null,
        val fileMetadata: NewFileMetadata,
        val serverMetadata: NewServerMetaData? = null
)

/** New server metadata (subset of full ServerMetaData). */
@Serializable
data class NewServerMetaData(
        val accessControlList: FileAccessControlList? = null,
        val originalRecipientCount: Int? = null,
        val transferHistory: TransferHistorySummary? = null
)

/** Transfer history summary for server metadata. */
@Serializable data class TransferHistorySummary(val summary: RecipientTransferSummary)

/** Access control list with security group type. This version uses the SecurityGroupType enum. */
@Serializable
data class FileAccessControlList(
        val requiredSecurityGroup: SecurityGroupType = SecurityGroupType.Owner,
        val circleIdList: List<String>? = null,
        val odinIdList: List<String>? = null
)
