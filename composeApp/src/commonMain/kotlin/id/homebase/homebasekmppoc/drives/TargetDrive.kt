package id.homebase.homebasekmppoc.drives

import id.homebase.homebasekmppoc.core.GuidId
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.Serializable

/**
 * A drive specifier for incoming requests to perform actions on a drive.
 * (essentially, this hides the internal DriveId).
 *
 * Ported from C# Odin.Services.Drives.TargetDrive
 */
@Serializable
data class TargetDrive(
    val alias: GuidId,
    val type: GuidId
) {

    fun clone(): TargetDrive {
        return TargetDrive(
            alias = alias.clone(),
            type = type.clone()
        )
    }

    fun toKey(): ByteArray {
        // Combine type and alias as bytes
        val typeBytes = type.value.toByteArray(Charsets.UTF_8)
        val aliasBytes = alias.value.toByteArray(Charsets.UTF_8)
        return typeBytes + aliasBytes
    }

    fun isValid(): Boolean {
        return GuidId.isValid(alias) && GuidId.isValid(type)
    }

    override fun toString(): String {
        return "Alias=$alias Type=$type"
    }

    companion object {
        fun newTargetDrive(): TargetDrive {
            return TargetDrive(
                alias = GuidId.newGuid(),
                type = GuidId.newGuid()
            )
        }

        fun newTargetDrive(type: GuidId): TargetDrive {
            return TargetDrive(
                alias = GuidId.newGuid(),
                type = type
            )
        }
    }
}
