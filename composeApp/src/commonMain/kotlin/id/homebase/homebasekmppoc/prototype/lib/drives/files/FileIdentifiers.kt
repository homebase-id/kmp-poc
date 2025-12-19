package id.homebase.homebasekmppoc.prototype.lib.drives.files

import id.homebase.homebasekmppoc.prototype.lib.drives.TargetDrive
import id.homebase.homebasekmppoc.prototype.lib.serialization.UuidSerializer
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/** Base file identifier interface marker. */
interface BaseFileIdentifier {
    val targetDrive: TargetDrive
}

/**
 * File identifier using fileId. Note: Similar to drives.upload.FileIdFileIdentifier but uses Uuid.
 */
@Serializable
data class FileIdFileIdentifier(
        @Serializable(with = UuidSerializer::class) val fileId: Uuid,
        override val targetDrive: TargetDrive
) : BaseFileIdentifier

/**
 * File identifier using globalTransitId. Note: Already exists as
 * drives.GlobalTransitIdFileIdentifier but this version implements BaseFileIdentifier interface.
 */
@Serializable
data class GlobalTransitIdFileIdentifier(
        @Serializable(with = UuidSerializer::class) val globalTransitId: Uuid,
        override val targetDrive: TargetDrive
) : BaseFileIdentifier

/** File identifier using uniqueId. */
@Serializable
data class UniqueIdFileIdentifier(
        @Serializable(with = UuidSerializer::class) val uniqueId: Uuid,
        override val targetDrive: TargetDrive
) : BaseFileIdentifier

/** Union type for all file identifiers. */
@Serializable
sealed class FileIdentifierUnion : BaseFileIdentifier {
    @Serializable
    data class ByFileId(val identifier: FileIdFileIdentifier) : FileIdentifierUnion() {
        override val targetDrive: TargetDrive
            get() = identifier.targetDrive
    }

    @Serializable
    data class ByGlobalTransitId(val identifier: GlobalTransitIdFileIdentifier) :
            FileIdentifierUnion() {
        override val targetDrive: TargetDrive
            get() = identifier.targetDrive
    }

    @Serializable
    data class ByUniqueId(val identifier: UniqueIdFileIdentifier) : FileIdentifierUnion() {
        override val targetDrive: TargetDrive
            get() = identifier.targetDrive
    }
}
