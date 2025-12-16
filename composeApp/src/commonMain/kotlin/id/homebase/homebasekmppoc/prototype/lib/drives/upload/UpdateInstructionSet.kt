package id.homebase.homebasekmppoc.prototype.lib.drives.upload

import id.homebase.homebasekmppoc.prototype.lib.drives.FileSystemType
import id.homebase.homebasekmppoc.prototype.lib.drives.files.GlobalTransitIdFileIdentifier
import id.homebase.homebasekmppoc.prototype.lib.serialization.Base64ByteArraySerializer
import kotlinx.serialization.Serializable

/** File identifier using fileId for local operations. */
@Serializable
data class FileIdFileIdentifier(
        val fileId: String,
        val targetDrive: id.homebase.homebasekmppoc.prototype.lib.drives.TargetDrive
)

/** Represents the locale of an update operation. */
enum class UpdateLocale {
    Peer,
    Local
}

/** Base update instruction set interface. */
interface BaseUpdateInstructionSet {
    val transferIv: ByteArray?
    val systemFileType: FileSystemType?
    val useAppNotification: Boolean?
    val appNotificationOptions: PushNotificationOptions?
}

/** Update instruction set for peer operations. */
@Serializable
data class UpdatePeerInstructionSet(
        val file: GlobalTransitIdFileIdentifier,
        val versionTag: String? = null,
        val recipients: List<String>? = null,
        @Serializable(with = Base64ByteArraySerializer::class)
        override val transferIv: ByteArray? = null,
        override val systemFileType: FileSystemType? = null,
        override val useAppNotification: Boolean? = null,
        override val appNotificationOptions: PushNotificationOptions? = null
) : BaseUpdateInstructionSet {
    val locale: UpdateLocale = UpdateLocale.Peer

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UpdatePeerInstructionSet

        if (file != other.file) return false
        if (versionTag != other.versionTag) return false
        if (recipients != other.recipients) return false
        if (transferIv != null) {
            if (other.transferIv == null) return false
            if (!transferIv.contentEquals(other.transferIv)) return false
        } else if (other.transferIv != null) return false
        if (systemFileType != other.systemFileType) return false
        if (useAppNotification != other.useAppNotification) return false
        if (appNotificationOptions != other.appNotificationOptions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = file.hashCode()
        result = 31 * result + (versionTag?.hashCode() ?: 0)
        result = 31 * result + (recipients?.hashCode() ?: 0)
        result = 31 * result + (transferIv?.contentHashCode() ?: 0)
        result = 31 * result + (systemFileType?.hashCode() ?: 0)
        result = 31 * result + (useAppNotification?.hashCode() ?: 0)
        result = 31 * result + (appNotificationOptions?.hashCode() ?: 0)
        return result
    }
}

/** Update instruction set for local operations. */
@Serializable
data class UpdateLocalInstructionSet(
        val file: FileIdFileIdentifier,
        val versionTag: String? = null,
        val recipients: List<String>? = null,
        @Serializable(with = Base64ByteArraySerializer::class)
        override val transferIv: ByteArray? = null,
        override val systemFileType: FileSystemType? = null,
        override val useAppNotification: Boolean? = null,
        override val appNotificationOptions: PushNotificationOptions? = null
) : BaseUpdateInstructionSet {
    val locale: UpdateLocale = UpdateLocale.Local

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UpdateLocalInstructionSet

        if (file != other.file) return false
        if (versionTag != other.versionTag) return false
        if (recipients != other.recipients) return false
        if (transferIv != null) {
            if (other.transferIv == null) return false
            if (!transferIv.contentEquals(other.transferIv)) return false
        } else if (other.transferIv != null) return false
        if (systemFileType != other.systemFileType) return false
        if (useAppNotification != other.useAppNotification) return false
        if (appNotificationOptions != other.appNotificationOptions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = file.hashCode()
        result = 31 * result + (versionTag?.hashCode() ?: 0)
        result = 31 * result + (recipients?.hashCode() ?: 0)
        result = 31 * result + (transferIv?.contentHashCode() ?: 0)
        result = 31 * result + (systemFileType?.hashCode() ?: 0)
        result = 31 * result + (useAppNotification?.hashCode() ?: 0)
        result = 31 * result + (appNotificationOptions?.hashCode() ?: 0)
        return result
    }
}

/** Union type for update instruction sets. */
@Serializable
sealed class UpdateInstructionSet {
    @Serializable
    data class Peer(val instructionSet: UpdatePeerInstructionSet) : UpdateInstructionSet()

    @Serializable
    data class Local(val instructionSet: UpdateLocalInstructionSet) : UpdateInstructionSet()
}
