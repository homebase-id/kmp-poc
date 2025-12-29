package id.homebase.homebasekmppoc.prototype.lib.drives.upload

import id.homebase.homebasekmppoc.prototype.lib.crypto.KeyHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.files.GlobalTransitIdFileIdentifier
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.uuid.Uuid

/** Result of an upload operation. */
//@Serializable
//data class UploadResult(
//        /** Key header used for encryption (not serialized, set after upload). */
//        @Transient var keyHeader: KeyHeader? = null,
//        val file: FileIdFileIdentifier,
//        val globalTransitIdFileIdentifier: GlobalTransitIdFileIdentifier,
//        val recipientStatus: Map<String, TransferUploadStatus>? = null,
//        val newVersionTag: String
//)
//

@Serializable
data class CreateFileResult(

    /** Key header used for encryption (not serialized, set after upload). */
    @Transient var keyHeader: KeyHeader? = null,
    val fileId: Uuid,
    val driveId: Uuid,
    var globalTransitId: Uuid? = null,
    val recipientStatus: Map<String, TransferUploadStatus>? = null,
    val newVersionTag: Uuid
)


/** Result of an update operation. */
@Serializable
data class UpdateResult(
    /** File identifier, undefined when locale == Peer. */
    val file: FileIdFileIdentifier? = null,
    val globalTransitIdFileIdentifier: GlobalTransitIdFileIdentifier,
    val newVersionTag: String,
    val recipientStatus: Map<String, TransferUploadStatus> = emptyMap()
)

@Serializable
data class UpdateFileResult(
    val fileId: Uuid,
    val driveId: Uuid,
    val globalTransitId: Uuid? = null,
    val recipientStatus: Map<String, TransferUploadStatus>? = null,
    val newVersionTag: Uuid
)
