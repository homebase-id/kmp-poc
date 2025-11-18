package id.homebase.homebasekmppoc.drives

import kotlinx.serialization.Serializable

/**
 * File state enumeration
 * Ported from C# Odin.Services.Drives.DriveCore.Storage.FileState
 */
@Serializable
enum class FileState(val value: Int) {
    Deleted(0),
    Active(1);
    // Archived(3) - commented out in original

    companion object {
        fun fromInt(value: Int): FileState {
            return entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown FileState: $value")
        }
    }
}
