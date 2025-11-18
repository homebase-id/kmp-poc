package id.homebase.homebasekmppoc.drives

import kotlinx.serialization.Serializable

/**
 * FileSystemType enumeration
 * Ported from C# Odin.Core.Storage.FileSystemType
 */
@Serializable
enum class FileSystemType(val value: Int) {
    Standard(0),
    Comment(1);
    // Add more as needed from the C# enum

    companion object {
        fun fromInt(value: Int): FileSystemType {
            return entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown FileSystemType: $value")
        }
    }
}
