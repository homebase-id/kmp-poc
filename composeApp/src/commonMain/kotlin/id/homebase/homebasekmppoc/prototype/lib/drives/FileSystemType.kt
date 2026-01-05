package id.homebase.homebasekmppoc.prototype.lib.drives

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * FileSystemType enumeration
 * Ported from C# Odin.Core.Storage.FileSystemType
 */
@Serializable
enum class FileSystemType(val value: Int) {
    @SerialName("standard")
    Standard(128),

    @SerialName("comment")
    Comment(32);
    // Add more as needed from the C# enum

    companion object {
        fun fromInt(value: Int): FileSystemType {
            return entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown FileSystemType: $value")
        }
    }
}