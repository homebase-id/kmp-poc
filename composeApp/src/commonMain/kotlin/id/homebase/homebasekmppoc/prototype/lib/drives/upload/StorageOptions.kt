package id.homebase.homebasekmppoc.prototype.lib.drives.upload

import id.homebase.homebasekmppoc.prototype.lib.drives.TargetDrive
import kotlinx.serialization.Serializable

/** Storage options for file uploads. */
@Serializable
data class StorageOptions(
        val drive: TargetDrive,

        /** @deprecated This property is deprecated and will be removed in future versions. */
        @Deprecated("This property is deprecated and will be removed in future versions")
        val overwriteFileId: String? = null,
        val expiresTimestamp: Long? = null,

        /** Storage intent, 'metadataOnly' or default (overwrite). */
        val storageIntent: String? = null // "metadataOnly" is a valid value
)
