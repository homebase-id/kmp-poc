package id.homebase.homebasekmppoc.prototype.lib.drives.upload

import id.homebase.homebasekmppoc.prototype.lib.crypto.KeyHeader
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.uuid.Uuid

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


@Serializable
data class UpdateFileResult(
    val fileId: Uuid,
    val driveId: Uuid,
    val globalTransitId: Uuid? = null,
    val recipientStatus: Map<String, TransferUploadStatus>? = null,
    val newVersionTag: Uuid
)
