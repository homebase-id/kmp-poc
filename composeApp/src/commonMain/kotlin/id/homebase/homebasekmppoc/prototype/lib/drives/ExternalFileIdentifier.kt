@file:OptIn(ExperimentalUuidApi::class)

package id.homebase.homebasekmppoc.prototype.lib.drives

import id.homebase.homebasekmppoc.prototype.lib.crypto.ByteArrayUtil
import id.homebase.homebasekmppoc.lib.serialization.UuidSerializer
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Drive and file info which identifies a file to be used externally to the host.
 * i.e. you can send this to the client
 *
 * Ported from C# Odin.Services.Drives.ExternalFileIdentifier
 */
@Serializable
data class ExternalFileIdentifier(
    /**
     * The drive to access
     */
    val targetDrive: TargetDrive,

    /**
     * The fileId to retrieve
     */
    @Serializable(with = UuidSerializer::class)
    val fileId: Uuid
) {
    /**
     * Converts this identifier to a byte array key by combining fileId and targetDrive.
     */
    fun toKey(): ByteArray {
        return ByteArrayUtil.combine(fileId.toByteArray(), targetDrive.toKey())
    }

    /**
     * Checks if this identifier has valid values.
     */
    fun hasValue(): Boolean {
        return fileId != Uuid.NIL && targetDrive.isValid()
    }

    override fun toString(): String {
        return "File:[$fileId]\tTargetDrive:[$targetDrive]"
    }

    /**
     * Converts this ExternalFileIdentifier to a FileIdentifier.
     */
    fun toFileIdentifier(): FileIdentifier {
        return FileIdentifier(
            fileId = fileId,
            targetDrive = targetDrive
        )
    }
}
