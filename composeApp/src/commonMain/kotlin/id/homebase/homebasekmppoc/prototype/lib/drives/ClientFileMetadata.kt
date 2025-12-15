@file:OptIn(ExperimentalUuidApi::class)

package id.homebase.homebasekmppoc.prototype.lib.drives

import id.homebase.homebasekmppoc.prototype.lib.core.time.UnixTimeUtc
import id.homebase.homebasekmppoc.prototype.lib.serialization.UuidSerializer
import kotlinx.serialization.KSerializer
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Client file metadata Ported from C# Odin.Services.Apps.ClientFileMetadata
 *
 * Note: Simplified version - some complex nested types are stubbed
 */
@Serializable
data class ClientFileMetadata(
        @Serializable(with = UuidSerializer::class) val globalTransitId: Uuid? = null,
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
        @Serializable(with = UuidSerializer::class) val versionTag: Uuid? = null,
        val payloads: List<PayloadDescriptor>? = null,
        val dataSource: DataSource? = null
) {
    fun getPayloadDescriptor(key: String): PayloadDescriptor? {
        return payloads?.firstOrNull { it.keyEquals(key) }
    }
}

@Serializable
data class AppFileMetaData(
        @Serializable(with = UuidSerializer::class) val uniqueId: Uuid? = null,
        val tags: List<@Serializable(with = UuidSerializer::class) Uuid>? = null,
        val fileType: Int? = null,
        val dataType: Int? = null,
        @Serializable(with = UuidSerializer::class) val groupId: Uuid? = null,
        val userDate: Long? = null,
        val content: String? = null,
        val previewThumbnail: ThumbnailDescriptor? = null,
        val archivalStatus: ArchivalStatus? = null
)

/** Archival status for files */
@Serializable(with = ArchivalStatusSerializer::class)
enum class ArchivalStatus(val value: Int) {
    None(0),
    Archived(1),
    Removed(2);

    companion object {
        fun fromInt(value: Int): ArchivalStatus {
            return entries.firstOrNull { it.value == value } ?: None
        }
    }
}

/** Custom serializer for ArchivalStatus that handles integer input (0, 1, 2) */
object ArchivalStatusSerializer : KSerializer<ArchivalStatus> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(
                "ArchivalStatus",
                PrimitiveKind.INT
        )

    override fun serialize(encoder: Encoder, value: ArchivalStatus) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): ArchivalStatus {
        val intValue = decoder.decodeInt()
        return ArchivalStatus.fromInt(intValue)
    }
}

@Serializable
data class LocalAppMetadata(
    val tags: List<@Serializable(with = UuidSerializer::class) Uuid>? = null,
    val versionTag: Uuid? = null
)

/**
 * Drive and file info which identifies a file using a GlobalTransitId. Used externally to the host
 * (can be sent to clients).
 */
@Serializable
data class GlobalTransitIdFileIdentifier(
        /** The drive to access */
        val targetDrive: TargetDrive,

        /** The global transit id to retrieve */
        @Serializable(with = UuidSerializer::class) val globalTransitId: Uuid
) {
    /** Checks if this identifier has valid values. */
    fun hasValue(): Boolean {
        return globalTransitId != Uuid.NIL && targetDrive.isValid()
    }

    /** Converts this GlobalTransitIdFileIdentifier to a FileIdentifier. */
    fun toFileIdentifier(): FileIdentifier {
        return FileIdentifier(globalTransitId = globalTransitId, targetDrive = targetDrive)
    }
}

/** Reaction entry for both comments and summary reactions */
@Serializable
data class ReactionEntry(val key: String, val count: Int, val reactionContent: String)

/** Comment preview within a reaction summary */
@Serializable
data class CommentPreview(
        val created: Long,
        val updated: Long,
        val fileId: String,
        val isEncrypted: Boolean,
        val odinId: String,
        val content: String,
        val reactions: List<ReactionEntry> = emptyList()
)

/**
 * Reaction summary (also known as ReactionPreview in TypeScript) Contains comments, reactions, and
 * total comment count
 */
@Serializable
data class ReactionSummary(
        val comments: List<CommentPreview> = emptyList(),
        val reactions: Map<String, ReactionEntry> = emptyMap(),
        val totalCommentCount: Int = 0
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
        @Serializable(with = UuidSerializer::class) val driveId: Uuid,
        val payloadsAreRemote: Boolean = false
)
