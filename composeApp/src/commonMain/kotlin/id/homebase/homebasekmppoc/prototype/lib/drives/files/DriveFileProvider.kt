package id.homebase.homebasekmppoc.prototype.lib.drives.files

import co.touchlab.kermit.Logger as KLogger
import id.homebase.homebasekmppoc.prototype.lib.core.OdinClientErrorCode
import id.homebase.homebasekmppoc.prototype.lib.core.OdinClientException
import id.homebase.homebasekmppoc.prototype.lib.core.SecureByteArray
import id.homebase.homebasekmppoc.prototype.lib.crypto.EncryptedKeyHeader
import id.homebase.homebasekmppoc.prototype.lib.crypto.KeyHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.FileSystemType
import id.homebase.homebasekmppoc.prototype.lib.http.CreateHttpClientOptions
import id.homebase.homebasekmppoc.prototype.lib.http.OdinClient
import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/** Options for file operations. */
data class FileOperationOptions(
    val decrypt: Boolean = true,
    val lastModified: Long? = null
)

/** Options for payload operations with range support. */
data class PayloadOperationOptions(
    val fileSystemType: FileSystemType = FileSystemType.Standard,
    val decrypt: Boolean = true,
    val chunkStart: Long? = null,
    val chunkEnd: Long? = null,
    val lastModified: Long? = null
)

/** Response containing bytes and their content type. */
data class BytesResponse(val bytes: ByteArray, val contentType: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as BytesResponse
        if (!bytes.contentEquals(other.bytes)) return false
        if (contentType != other.contentType) return false
        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + contentType.hashCode()
        return result
    }
}

/** Provider for drive file operations. Ported from JS/TS odin-js DriveFileProvider. */
@OptIn(ExperimentalEncodingApi::class)
public class DriveFileProvider(private val odinClient: OdinClient) {

    companion object {
        private const val TAG = "DriveFileProvider"
    }

    // ==================== GET METHODS ====================

    /**
     * Gets a file header with optional decryption.
     *
     * @param driveId The target drive id containing the file
     * @param fileId The ID of the file
     * @param options Optional operation options
     * @return The HomebaseFile or null if not found
     */

    suspend fun getFileHeader(
        driveId: Uuid,
        fileId: Uuid,
    ): HomebaseFile? {

        ValidationUtil.requireValidUuid(driveId, "driveId")
        ValidationUtil.requireValidUuid(fileId, "fileId")

        val httpClient = odinClient.createHttpClient(CreateHttpClientOptions())

        return try {
            httpClient
                .get("drives/$driveId/files/$fileId/header")
                .body<HomebaseFile>()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) {
                null
            } else {
                throw e
            }
        }
    }


    /**
     * Gets payload bytes with optional decryption and range support.
     *
     * @param driveId
     * The target drive containing the file
     * @param fileId The ID of the file
     * @param key The payload key
     * @param options Optional operation options with range support
     * @return BytesResponse containing the payload bytes and content type, or null if not found
     */
    suspend fun getPayloadBytes(
        driveId: Uuid,
        fileId: Uuid,
        key: String,
        options: PayloadOperationOptions = PayloadOperationOptions()
    ): BytesResponse? {

        ValidationUtil.requireValidUuid(driveId, "driveId")
        ValidationUtil.requireValidUuid(fileId, "fileId")

        require(key.isNotBlank()) { "Key must be defined" }

        val httpClient = odinClient.createHttpClient(CreateHttpClientOptions())

        val queryParams =
            buildMap<String, String> {
                if (options.lastModified != null) {
                    put("lastModified", options.lastModified.toString())
                }
            }

        val rangeResult = DriveFileHelpers.getRangeHeader(options.chunkStart, options.chunkEnd)

        try {
            val response =
                httpClient.get("drives/${driveId}/files/${fileId}/payload/${key}") {
                    url {
                        queryParams.forEach { (key, value) -> parameters.append(key, value) }
                    }
                    if (rangeResult.rangeHeader != null) {
                        header(HttpHeaders.Range, rangeResult.rangeHeader)
                    }
                }

            if (response.status == HttpStatusCode.NotFound) {
                return null
            }

            if (!response.status.isSuccess() && response.status != HttpStatusCode.PartialContent) {
                KLogger.e(TAG) { "[odin-kt:getPayloadBytes] Request failed: ${response.status}" }
                return null
            }

            val bytes = response.readRawBytes()
            if (bytes.isEmpty()) return null

            val contentType =
                response.headers["decryptedcontenttype"]
                    ?: response.contentType()?.toString() ?: "application/octet-stream"

            val resultBytes =
                if (!options.decrypt) {
                    bytes
                } else if (rangeResult.updatedChunkStart != null) {
                    // Chunked decryption - decrypt and slice
                    val decrypted = decryptChunkedBytes(bytes, rangeResult.startOffset)
                    val sliceEnd =
                        if (options.chunkEnd != null && options.chunkStart != null) {
                            (options.chunkEnd - options.chunkStart).toInt()
                        } else {
                            decrypted.size
                        }
                    decrypted.sliceArray(0 until minOf(sliceEnd, decrypted.size))
                } else {
                    // Full decryption
                    decryptBytes(response, bytes)
                }

            return BytesResponse(bytes = resultBytes, contentType = contentType)
        } catch (e: Exception) {
            if (e.message?.contains("404") == true) return null
            KLogger.e(TAG) { "[odin-kt:getPayloadBytes] ${e.message}" }
            return null
        } finally {
            // handled globally
            // httpClient.close()
        }
    }

    /**
     * Gets payload as parsed JSON object.
     *
     * @param driveId The target drive containing the file
     * @param fileId The ID of the file
     * @param key The payload key
     * @param options Optional operation options
     * @return The parsed JSON object or null if not found
     */
    suspend inline fun <reified T> getPayloadAsJson(
        driveId: Uuid,
        fileId: Uuid,
        key: String,
        options: PayloadOperationOptions = PayloadOperationOptions()
    ): T? {
        ValidationUtil.requireValidUuid(driveId, "driveId")
        ValidationUtil.requireValidUuid(fileId, "fileId")

        val bytesResponse = getPayloadBytes(driveId, fileId, key, options) ?: return null
        return DriveFileHelpers.parseBytesToObject<T>(
            BytesWithContentType(bytesResponse.bytes, bytesResponse.contentType)
        )
    }

    /**
     * Gets thumbnail bytes.
     *
     * @param driveId The target drive containing the file
     * @param fileId The ID of the file
     * @param payloadKey The payload key
     * @param width Desired thumbnail width
     * @param height Desired thumbnail height
     * @param options Optional operation options
     * @return BytesResponse containing the thumbnail bytes and content type, or null if not found
     */
    suspend fun getThumbBytes(
        driveId: Uuid,
        fileId: Uuid,
        payloadKey: String,
        width: Int,
        height: Int,
        options: FileOperationOptions = FileOperationOptions()
    ): BytesResponse? {
        ValidationUtil.requireValidUuid(driveId, "driveId")
        ValidationUtil.requireValidUuid(fileId, "fileId")

        require(payloadKey.isNotBlank()) { "PayloadKey must be defined" }
        require(width > 0) { "Width must be positive" }
        require(height > 0) { "Height must be positive" }

        val httpClient =
            odinClient.createHttpClient(CreateHttpClientOptions(overrideEncryption = false))

        val queryParams =
            buildMap<String, String> {
                put("width", width.toString())
                put("height", height.toString())
                if (options.lastModified != null) {
                    put("lastModified", options.lastModified.toString())
                }
            }


        try {
            val response =
                httpClient.get("drives/${driveId}/files/${fileId}/payload/${payloadKey}/thumb") {
                    url {
                        queryParams.forEach { (key, value) -> parameters.append(key, value) }
                    }
                }

            if (response.status == HttpStatusCode.NotFound) {
                return null
            }

            if (!response.status.isSuccess()) {
                KLogger.e(TAG) { "[odin-kt:getThumbBytes] Request failed: ${response.status}" }
                return null
            }

            val bytes = response.readRawBytes()
            if (bytes.isEmpty()) return null

            val contentType =
                response.headers["decryptedcontenttype"]
                    ?: response.contentType()?.toString() ?: "image/jpeg"

            val resultBytes =
                if (options.decrypt) {
                    decryptBytes(response, bytes)
                } else {
                    bytes
                }

            return BytesResponse(bytes = resultBytes, contentType = contentType)
        } catch (e: ClientRequestException) {
            // HTTP 4xx
            if (e.response.status == HttpStatusCode.NotFound) {
                return null
            }
            throw e
        }
        catch (e: ServerResponseException) {
            // HTTP 5xx → real server error
            throw e
        }
        catch (e: Exception) {
            KLogger.e(TAG, e) { "[odin-kt:getThumbBytes] Unexpected failure" }
            throw e
        }
        finally {
            // httpClient.close()
        }
    }

    /**
     * Gets transfer history for a file.
     *
     * @param driveId The target drive containing the file
     * @param fileId The ID of the file
     * @param fileSystemType Optional file system type
     * @return The TransferHistory or null if not found
     */
    suspend fun getTransferHistory(
        driveId: Uuid,
        fileId: Uuid,
        fileSystemType: FileSystemType = FileSystemType.Standard
    ): TransferHistory? {

        ValidationUtil.requireValidUuid(driveId, "driveId")
        ValidationUtil.requireValidUuid(fileId, "fileId")

        val httpClient = odinClient.createHttpClient(CreateHttpClientOptions())

        val queryParams = buildMap<String, String> { }

        try {
            val response =
                httpClient.get("drives/${driveId}/files/${fileId}/transfer-history") {
                    url {
                        queryParams.forEach { (key, value) -> parameters.append(key, value) }
                    }
                }

            if (response.status == HttpStatusCode.NotFound) {
                return null
            }

            if (!response.status.isSuccess()) {
                KLogger.e(TAG) { "[odin-kt:getTransferHistory] Request failed: ${response.status}" }
                return null
            }

            val body = response.bodyAsText()
            return OdinSystemSerializer.json.decodeFromString<TransferHistory>(body)
        } catch (e: Exception) {
            if (e.message?.contains("404") == true) return null
            KLogger.e(TAG) { "[odin-kt:getTransferHistory] ${e.message}" }
            return null
        } finally {
            // httpClient.close()
        }
    }

    // ==================== DELETE METHODS ====================

    /**
     * Deletes a single file from the drive.
     *
     * @param driveId The target drive containing the file
     * @param fileId The ID of the file to delete
     * @param recipients Optional list of recipients to notify
     * @param fileSystemType Optional file system type
     * @param hardDelete If true, performs a hard delete instead of soft delete
     * @return True if the file was deleted successfully
     * @throws OdinClientException on errors
     */
    suspend fun deleteFile(
        driveId: Uuid,
        fileId: Uuid,
        recipients: List<String>? = null,
        fileSystemType: FileSystemType? = null,
        hardDelete: Boolean = false
    ): Boolean {

        ValidationUtil.requireValidUuid(driveId, "driveId")
        ValidationUtil.requireValidUuid(fileId, "fileId")

        val httpClient =
            odinClient.createHttpClient(CreateHttpClientOptions(overrideEncryption = true))

        val endpoint =
            if (hardDelete) "drives/${driveId}/files/${fileId}/hard-delete"
            else "drives/${driveId}files/${fileId}/delete"

        // fileId not used  because we pass it in via query string
        val request = DeleteFileRequest(fileId = Uuid.NIL, recipients = recipients)

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
            // httpClient.close()
        }
    }

    /** Deletes multiple files from the drive by file IDs. */
    suspend fun deleteFiles(
        driveId: Uuid,
        fileIds: List<Uuid>,
        recipients: List<String>? = null,
        fileSystemType: FileSystemType? = null
    ): Boolean {
        ValidationUtil.requireValidUuid(driveId, "driveId")
        ValidationUtil.requireValidUuidList(fileIds, "fileIds")

        val httpClient =
            odinClient.createHttpClient(CreateHttpClientOptions(overrideEncryption = true))

        val request =
            DeleteFilesBatchRequest(
                requests =
                    fileIds.map { fileId ->
                        DeleteFileRequest(
                            fileId = fileId,
                            recipients = recipients
                        )
                    }
            )

        try {
            val response: HttpResponse =
                httpClient.post("/drives/${driveId}/files/delete-batch/by-file-id") {
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
            // httpClient.close()
        }
    }

    /** Deletes files from the drive by group IDs. */
    suspend fun deleteFilesByGroupId(
        driveId: Uuid,
        groupIds: List<Uuid>,
        recipients: List<String>? = null,
        fileSystemType: FileSystemType? = null
    ): Boolean {
        ValidationUtil.requireValidUuid(driveId, "driveId")
        ValidationUtil.requireValidUuidList(groupIds, "groupIds")

        val httpClient =
            odinClient.createHttpClient(CreateHttpClientOptions(overrideEncryption = true))

        val request =
            DeleteByGroupIdBatchRequest(
                requests =
                    groupIds.map { groupId ->
                        DeleteByGroupIdRequest(
                            groupId = groupId,
                            recipients = recipients
                        )
                    }
            )

        try {
            val response: HttpResponse =
                httpClient.post("/drives/${driveId}/files/delete-batch/by-group-id") {
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
            // httpClient.close()
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /** Decrypts the key header using the shared secret. */
    private suspend fun decryptKeyHeader(encryptedKeyHeader: EncryptedKeyHeader): KeyHeader? {
        val sharedSecret = odinClient.getSharedSecret() ?: return null
        return encryptedKeyHeader.decryptAesToKeyHeader(SecureByteArray(sharedSecret))
    }

    /** Decrypts JSON content from file metadata. */
    private suspend fun decryptJsonContent(metadata: FileMetadata, keyHeader: KeyHeader): String? {
        val content = metadata.appData.content ?: return null
        if (!metadata.isEncrypted) return content

        return try {
            val encryptedBytes = Base64.decode(content)
            val decryptedBytes = keyHeader.decrypt(encryptedBytes)
            decryptedBytes.decodeToString()
        } catch (e: Exception) {
            KLogger.e(TAG) { "[odin-kt:decryptJsonContent] ${e.message}" }
            null
        }
    }

    /**
     * Decrypts bytes using the shared secret (for full payload/thumbnail decryption). Note: This is
     * a placeholder - actual implementation depends on your encryption setup.
     */
    private suspend fun decryptBytes(
        response: HttpResponse,
        bytes: ByteArray
    ): ByteArray {

        val payloadEncrypted =
            response.headers["payloadencrypted"]?.equals("true", ignoreCase = true) == true

        val encryptedHeader64 = response.headers["sharedsecretencryptedheader64"]

        if (payloadEncrypted && encryptedHeader64 != null) {

            val encryptedKeyHeader =
                EncryptedKeyHeader.fromBase64(encryptedHeader64)

            val keyHeader =
                decryptKeyHeader(encryptedKeyHeader)
                    ?: throw IllegalStateException("Missing shared secret")

            return decryptUsingKeyHeader(bytes, keyHeader)

        } else if (payloadEncrypted) {

            // Same behavior as TS
            throw IllegalStateException("Can't decrypt; missing keyheader")

        } else {

            // Not encrypted → return as-is
            return bytes
        }
    }


    /** Decrypts chunked bytes with offset handling. */
    private suspend fun decryptChunkedBytes(bytes: ByteArray, startOffset: Int): ByteArray {
        // Handle chunked decryption with offset
        // The actual decryption is handled by OdinEncryptionPlugin
        return if (startOffset > 0 && bytes.size > startOffset) {
            bytes.sliceArray(startOffset until bytes.size)
        } else {
            bytes
        }
    }

    private suspend fun decryptUsingKeyHeader(
        encryptedBytes: ByteArray,
        keyHeader: KeyHeader
    ): ByteArray {
        return keyHeader.decrypt(encryptedBytes)
    }

}

// Request data classes for delete operations

@Serializable
private data class DeleteFileRequest(val fileId: Uuid, val recipients: List<String>? = null)

@Serializable
private data class DeleteFilesBatchRequest(val requests: List<DeleteFileRequest>)

@Serializable
private data class DeleteByGroupIdRequest(
    val groupId: Uuid,
    val recipients: List<String>? = null
)

@Serializable
private data class DeleteByGroupIdBatchRequest(val requests: List<DeleteByGroupIdRequest>)
