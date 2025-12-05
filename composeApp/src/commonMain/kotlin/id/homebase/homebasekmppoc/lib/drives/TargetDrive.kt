package id.homebase.homebasekmppoc.lib.drives

import id.homebase.homebasekmppoc.lib.serialization.UuidSerializer
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * A drive specifier for incoming requests to perform actions on a drive.
 * (essentially, this hides the internal DriveId).
 *
 * Ported from C# Odin.Services.Drives.TargetDrive
 */
@Serializable
data class TargetDrive(
    @Serializable(with = UuidSerializer::class)
    val alias: Uuid,
    @Serializable(with = UuidSerializer::class)
    val type: Uuid
) {

    fun toKey(): ByteArray {
        // Combine type and alias as bytes
        val typeBytes = type.toString().toByteArray(Charsets.UTF_8)
        val aliasBytes = alias.toString().toByteArray(Charsets.UTF_8)
        return typeBytes + aliasBytes
    }

    fun isValid(): Boolean {
        return alias != Uuid.NIL && type != Uuid.NIL
    }

    override fun toString(): String {
        return "Alias=$alias Type=$type"
    }

    companion object {
        fun newTargetDrive(): TargetDrive {
            return TargetDrive(
                alias = Uuid.random(),
                type = Uuid.random()
            )
        }

        fun newTargetDrive(type: Uuid): TargetDrive {
            return TargetDrive(
                alias = Uuid.random(),
                type = type
            )
        }
    }
}