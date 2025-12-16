package id.homebase.homebasekmppoc.prototype.lib.drives.files

import co.touchlab.kermit.Logger as KLogger
import id.homebase.homebasekmppoc.prototype.lib.core.OdinClientErrorCode
import id.homebase.homebasekmppoc.prototype.lib.core.OdinClientException
import id.homebase.homebasekmppoc.prototype.lib.drives.FileSystemType
import id.homebase.homebasekmppoc.prototype.lib.drives.TargetDrive
import id.homebase.homebasekmppoc.prototype.lib.http.CreateHttpClientOptions
import id.homebase.homebasekmppoc.prototype.lib.http.OdinClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

/**
 * Provider for drive file delete operations. Ported from JS/TS odin-js deleteFile, deleteFiles, and
 * deleteFilesByGroupId functions.
 */
public class DriveFileProvider(private val client: OdinClient) {

    companion object {
        private const val TAG = "DriveFileProvider"
    }

    /**
     * Deletes a single file from the drive.
     *
     * @param targetDrive The target drive containing the file
     * @param fileId The ID of the file to delete
     * @param recipients Optional list of recipients to notify
     * @param fileSystemType Optional file system type
     * @param hardDelete If true, performs a hard delete instead of soft delete
     * @return True if the file was deleted successfully
     * @throws OdinClientException on errors
     */
    suspend fun deleteFile(
            targetDrive: TargetDrive,
            fileId: String,
            recipients: List<String>? = null,
            fileSystemType: FileSystemType? = null,
            hardDelete: Boolean = false
    ): Boolean {
        require(targetDrive.alias.isNotBlank() || targetDrive.type.isNotBlank()) {
            "TargetDrive must be defined"
        }
        require(fileId.isNotBlank()) { "FileId must be defined" }

        val httpClient =
                client.createHttpClient(
                        CreateHttpClientOptions(
                                overrideEncryption = true,
                                fileSystemType = fileSystemType ?: FileSystemType.Standard
                        )
                )

        val endpoint = if (hardDelete) "/drive/files/harddelete" else "/drive/files/delete"
        val request =
                DeleteFileRequest(
                        file = FileReference(targetDrive = targetDrive, fileId = fileId),
                        recipients = recipients
                )

        try {
            val response: HttpResponse =
                    httpClient.post(endpoint) {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }

            return response.status.isSuccess()
        } catch (e: OdinClientException) {
            throw e
        } catch (e: Exception) {
            KLogger.e(TAG) { "[odin-kt:deleteFile] ${e.message}" }
            throw OdinClientException(
                    "Delete file failed: ${e.message}",
                    OdinClientErrorCode.UnhandledScenario,
                    e
            )
        } finally {
            httpClient.close()
        }
    }

    /**
     * Deletes multiple files from the drive by file IDs.
     *
     * @param targetDrive The target drive containing the files
     * @param fileIds List of file IDs to delete
     * @param recipients Optional list of recipients to notify
     * @param fileSystemType Optional file system type
     * @return True if the files were deleted successfully
     * @throws OdinClientException on errors
     */
    suspend fun deleteFiles(
            targetDrive: TargetDrive,
            fileIds: List<String>,
            recipients: List<String>? = null,
            fileSystemType: FileSystemType? = null
    ): Boolean {
        require(targetDrive.alias.isNotBlank() || targetDrive.type.isNotBlank()) {
            "TargetDrive must be defined"
        }
        require(fileIds.isNotEmpty()) { "FileIds must be defined" }

        val httpClient =
                client.createHttpClient(
                        CreateHttpClientOptions(
                                overrideEncryption = true,
                                fileSystemType = fileSystemType ?: FileSystemType.Standard
                        )
                )

        val request =
                DeleteFilesBatchRequest(
                        requests =
                                fileIds.map { fileId ->
                                    DeleteFileRequest(
                                            file =
                                                    FileReference(
                                                            targetDrive = targetDrive,
                                                            fileId = fileId
                                                    ),
                                            recipients = recipients
                                    )
                                }
                )

        try {
            val response: HttpResponse =
                    httpClient.post("/drive/files/deletefileidbatch") {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }

            return response.status.isSuccess()
        } catch (e: OdinClientException) {
            throw e
        } catch (e: Exception) {
            KLogger.e(TAG) { "[odin-kt:deleteFiles] ${e.message}" }
            throw OdinClientException(
                    "Delete files batch failed: ${e.message}",
                    OdinClientErrorCode.UnhandledScenario,
                    e
            )
        } finally {
            httpClient.close()
        }
    }

    /**
     * Deletes files from the drive by group IDs.
     *
     * @param targetDrive The target drive containing the files
     * @param groupIds List of group IDs to delete
     * @param recipients Optional list of recipients to notify
     * @param fileSystemType Optional file system type
     * @return True if the files were deleted successfully
     * @throws OdinClientException on errors
     */
    suspend fun deleteFilesByGroupId(
            targetDrive: TargetDrive,
            groupIds: List<String>,
            recipients: List<String>? = null,
            fileSystemType: FileSystemType? = null
    ): Boolean {
        require(targetDrive.alias.isNotBlank() || targetDrive.type.isNotBlank()) {
            "TargetDrive must be defined"
        }
        require(groupIds.isNotEmpty()) { "GroupIds must be defined" }

        val httpClient =
                client.createHttpClient(
                        CreateHttpClientOptions(
                                overrideEncryption = true,
                                fileSystemType = fileSystemType ?: FileSystemType.Standard
                        )
                )

        val request =
                DeleteByGroupIdBatchRequest(
                        requests =
                                groupIds.map { groupId ->
                                    DeleteByGroupIdRequest(
                                            targetDrive = targetDrive,
                                            groupId = groupId,
                                            recipients = recipients
                                    )
                                }
                )

        try {
            val response: HttpResponse =
                    httpClient.post("/drive/files/deletegroupidbatch") {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }

            return response.status.isSuccess()
        } catch (e: OdinClientException) {
            throw e
        } catch (e: Exception) {
            KLogger.e(TAG) { "[odin-kt:deleteFilesByGroupId] ${e.message}" }
            throw OdinClientException(
                    "Delete files by group ID batch failed: ${e.message}",
                    OdinClientErrorCode.UnhandledScenario,
                    e
            )
        } finally {
            httpClient.close()
        }
    }
}

// Request data classes for delete operations

@Serializable private data class FileReference(val targetDrive: TargetDrive, val fileId: String)

@Serializable
private data class DeleteFileRequest(val file: FileReference, val recipients: List<String>? = null)

@Serializable private data class DeleteFilesBatchRequest(val requests: List<DeleteFileRequest>)

@Serializable
private data class DeleteByGroupIdRequest(
        val targetDrive: TargetDrive,
        val groupId: String,
        val recipients: List<String>? = null
)

@Serializable
private data class DeleteByGroupIdBatchRequest(val requests: List<DeleteByGroupIdRequest>)
