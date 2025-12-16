package id.homebase.homebasekmppoc.prototype.lib.drives.files

import id.homebase.homebasekmppoc.prototype.lib.crypto.EncryptedKeyHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.FileSystemType
import id.homebase.homebasekmppoc.prototype.lib.drives.ServerMetadata
import id.homebase.homebasekmppoc.prototype.lib.serialization.UuidSerializer
import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * File state enum for HomebaseFile. Note: This is separate from drives.FileState which uses int
 * values.
 */
@Serializable
enum class HomebaseFileState(val value: String) {
    @SerialName("active") Active("active"),
    @SerialName("deleted") Deleted("deleted");

    companion object {
        fun fromString(value: String): HomebaseFileState {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: Active
        }
    }
}

/**
 * Base Homebase file structure. Ported from TypeScript BaseHomebaseFile interface.
 *
 * Note: This is similar to SharedSecretEncryptedFileHeader but uses string-based file state and
 * matches the TypeScript API more closely.
 */
@Serializable
data class HomebaseFile(
    @Serializable(with = UuidSerializer::class) val fileId: Uuid,
    val fileSystemType: FileSystemType,
    val fileState: HomebaseFileState = HomebaseFileState.Active,
    val fileMetadata: FileMetadata,
    val sharedSecretEncryptedKeyHeader: EncryptedKeyHeader,
    val serverMetadata: ServerMetadata? = null
) {
    val isActive: Boolean
        get() = fileState == HomebaseFileState.Active

    val isDeleted: Boolean
        get() = fileState == HomebaseFileState.Deleted
}

/** Deleted Homebase file (convenience type). */
typealias DeletedHomebaseFile = HomebaseFile
