@file:OptIn(ExperimentalUuidApi::class)

package id.homebase.homebasekmppoc.drives

import id.homebase.homebasekmppoc.core.time.UnixTimeUtcRange
import id.homebase.homebasekmppoc.serialization.UuidSerializer
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * File query parameters for filtering drive files
 *
 * Ported from C# Odin.Services.Drives.DriveCore.Query.FileQueryParams
 */
@Serializable
data class FileQueryParams(
    val targetDrive: TargetDrive,
    val fileType: List<Int>? = null,
    val fileState: List<FileState>? = null,
    val dataType: List<Int>? = null,
    val archivalStatus: List<Int>? = null,
    val sender: List<String>? = null,
    val groupId: List<@Serializable(with = UuidSerializer::class) Uuid>? = null,
    val userDate: UnixTimeUtcRange? = null,
    val clientUniqueIdAtLeastOne: List<@Serializable(with = UuidSerializer::class) Uuid>? = null,
    val tagsMatchAtLeastOne: List<@Serializable(with = UuidSerializer::class) Uuid>? = null,
    val tagsMatchAll: List<@Serializable(with = UuidSerializer::class) Uuid>? = null,
    val localTagsMatchAtLeastOne: List<@Serializable(with = UuidSerializer::class) Uuid>? = null,
    val localTagsMatchAll: List<@Serializable(with = UuidSerializer::class) Uuid>? = null,
    val globalTransitId: List<@Serializable(with = UuidSerializer::class) Uuid>? = null
) {
    fun assertIsValid() {
        require(targetDrive.isValid()) { "Invalid target drive" }
    }

    companion object {
        fun fromFileType(drive: TargetDrive, vararg fileType: Int): FileQueryParams {
            return FileQueryParams(
                targetDrive = drive,
                fileType = fileType.toList()
            )
        }
    }
}
