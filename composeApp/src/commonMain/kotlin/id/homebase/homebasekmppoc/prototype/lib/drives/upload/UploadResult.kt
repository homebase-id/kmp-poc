package id.homebase.homebasekmppoc.prototype.lib.drives.upload

import id.homebase.homebasekmppoc.prototype.lib.crypto.KeyHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.files.GlobalTransitIdFileIdentifier
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** Result of an upload operation. */
@Serializable
data class UploadResult(
        /** Key header used for encryption (not serialized, set after upload). */
        @Transient var keyHeader: KeyHeader? = null,
        val file: FileIdFileIdentifier,
        val globalTransitIdFileIdentifier: GlobalTransitIdFileIdentifier,
        val recipientStatus: Map<String, TransferUploadStatus>? = null,
        val newVersionTag: String
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
