package id.homebase.homebasekmppoc.prototype.lib.drives.upload

import id.homebase.homebasekmppoc.prototype.lib.drives.files.GlobalTransitIdFileIdentifier
import kotlinx.serialization.Serializable

/** Result of an upload operation. */
@Serializable
data class UploadResult(
        val keyHeader: UploadKeyHeader? = null,
        val file: FileIdFileIdentifier,
        val globalTransitIdFileIdentifier: GlobalTransitIdFileIdentifier,
        val recipientStatus: Map<String, TransferUploadStatus> = emptyMap(),
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
