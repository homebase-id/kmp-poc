package id.homebase.homebasekmppoc.prototype.lib.drives.files

import co.touchlab.kermit.Logger as KLogger
import id.homebase.homebasekmppoc.prototype.lib.core.OdinClientErrorCode
import id.homebase.homebasekmppoc.prototype.lib.core.OdinClientException
import id.homebase.homebasekmppoc.prototype.lib.core.SecureByteArray
import id.homebase.homebasekmppoc.prototype.lib.crypto.EncryptedKeyHeader
import id.homebase.homebasekmppoc.prototype.lib.crypto.KeyHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.FileSystemType
import id.homebase.homebasekmppoc.prototype.lib.drives.TargetDrive
import id.homebase.homebasekmppoc.prototype.lib.http.CreateHttpClientOptions
import id.homebase.homebasekmppoc.prototype.lib.http.OdinClient
import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.Serializable

/** Options for file operations. */
data class FileOperationOptions(
        val fileSystemType: FileSystemType = FileSystemType.Standard,
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
public class DriveFileProvider(private val client: OdinClient) {

    companion object {
        private const val TAG = "DriveFileProvider"
    }

    // ==================== GET METHODS ====================

    /**
     * Gets a file header with optional decryption.
     *
     * @param targetDrive The target drive containing the file
     * @param fileId The ID of the file
     * @param options Optional operation options
     * @return The HomebaseFile or null if not found
     */
    suspend fun getFileHeader(
            targetDrive: TargetDrive,
            fileId: String,
            options: FileOperationOptions = FileOperationOptions()
    ): HomebaseFile? {
        require(targetDrive.isValid()) { "TargetDrive must be defined" }
        require(fileId.isNotBlank()) { "FileId must be defined" }

        val fileHeader = getFileHeaderBytes(targetDrive, fileId, options)
        if (fileHeader == null) return null

        // If decrypt is enabled and content is JSON, try to parse it
        if (options.decrypt && fileHeader.fileMetadata.appData.content != null) {
            val decryptedContent = fileHeader.fileMetadata.appData.content
            // The content is already decrypted by getFileHeaderBytes, return as-is
            return fileHeader
        }

        return fileHeader
    }

    /**
     * Gets raw file header bytes with optional decryption.
     *
     * @param targetDrive The target drive containing the file
     * @param fileId The ID of the file
     * @param options Optional operation options
     * @return The HomebaseFile or null if not found
     */
    suspend fun getFileHeaderBytes(
            targetDrive: TargetDrive,
            fileId: String,
            options: FileOperationOptions = FileOperationOptions()
    ): HomebaseFile? {
        require(targetDrive.isValid()) { "TargetDrive must be defined" }
        require(fileId.isNotBlank()) { "FileId must be defined" }

        val httpClient =
                client.createHttpClient(
                        CreateHttpClientOptions(
                                overrideEncryption = false,
                                fileSystemType = options.fileSystemType
                        )
                )

        val queryParams =
                buildMap<String, String> {
                    put("alias", targetDrive.alias.toString())
                    put("type", targetDrive.type.toString())
                    put("fileId", fileId)
                }

        try {
            val response =
                    httpClient.get("/drive/files/header") {
                        url {
                            queryParams.forEach { (key, value) -> parameters.append(key, value) }
                        }
                    }

            if (response.status == HttpStatusCode.NotFound) {
                return null
            }

            if (!response.status.isSuccess()) {
                KLogger.e(TAG) { "[odin-kt:getFileHeader] Request failed: ${response.status}" }
                return null
            }

            val body = response.bodyAsText()
            val fileHeader = OdinSystemSerializer.json.decodeFromString<HomebaseFile>(body)

            if (options.decrypt) {
                val keyHeader =
                        if (fileHeader.fileMetadata.isEncrypted) {
                            decryptKeyHeader(fileHeader.sharedSecretEncryptedKeyHeader)
                        } else {
                            null
                        }

                // Decrypt JSON content if needed
                if (keyHeader != null && fileHeader.fileMetadata.appData.content != null) {
                    val decryptedContent = decryptJsonContent(fileHeader.fileMetadata, keyHeader)
                    return fileHeader.copy(
                            fileMetadata =
                                    fileHeader.fileMetadata.copy(
                                            appData =
                                                    fileHeader.fileMetadata.appData.copy(
                                                            content = decryptedContent
                                                    )
                                    )
                    )
                }
            }

            return fileHeader
        } catch (e: Exception) {
            if (e.message?.contains("404") == true) return null
            KLogger.e(TAG) { "[odin-kt:getFileHeader] ${e.message}" }
            throw e
        } finally {
            httpClient.close()
        }
    }

    /**
     * Gets payload bytes with optional decryption and range support.
     *
     * @param targetDrive The target drive containing the file
     * @param fileId The ID of the file
     * @param key The payload key
     * @param options Optional operation options with range support
     * @return BytesResponse containing the payload bytes and content type, or null if not found
     */
    suspend fun getPayloadBytes(
            targetDrive: TargetDrive,
            fileId: String,
            key: String,
            options: PayloadOperationOptions = PayloadOperationOptions()
    ): BytesResponse? {
        require(targetDrive.isValid()) { "TargetDrive must be defined" }
        require(fileId.isNotBlank()) { "FileId must be defined" }
        require(key.isNotBlank()) { "Key must be defined" }

        val httpClient =
                client.createHttpClient(
                        CreateHttpClientOptions(
                                overrideEncryption = false,
                                fileSystemType = options.fileSystemType
                        )
                )

        val queryParams =
                buildMap<String, String> {
                    put("alias", targetDrive.alias.toString())
                    put("type", targetDrive.type.toString())
                    put("fileId", fileId)
                    put("key", key)
                    if (options.lastModified != null) {
                        put("lastModified", options.lastModified.toString())
                    }
                }

        val rangeResult = DriveFileHelpers.getRangeHeader(options.chunkStart, options.chunkEnd)

        try {
            val response =
                    httpClient.get("/drive/files/payload") {
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
                        decryptBytes(bytes)
                    }

            return BytesResponse(bytes = resultBytes, contentType = contentType)
        } catch (e: Exception) {
            if (e.message?.contains("404") == true) return null
            KLogger.e(TAG) { "[odin-kt:getPayloadBytes] ${e.message}" }
            return null
        } finally {
            httpClient.close()
        }
    }

    /**
     * Gets payload as parsed JSON object.
     *
     * @param targetDrive The target drive containing the file
     * @param fileId The ID of the file
     * @param key The payload key
     * @param options Optional operation options
     * @return The parsed JSON object or null if not found
     */
    suspend inline fun <reified T> getPayloadAsJson(
            targetDrive: TargetDrive,
            fileId: String,
            key: String,
            options: PayloadOperationOptions = PayloadOperationOptions()
    ): T? {
        val bytesResponse = getPayloadBytes(targetDrive, fileId, key, options) ?: return null
        return DriveFileHelpers.parseBytesToObject<T>(
                BytesWithContentType(bytesResponse.bytes, bytesResponse.contentType)
        )
    }

    /**
     * Gets thumbnail bytes.
     *
     * @param targetDrive The target drive containing the file
     * @param fileId The ID of the file
     * @param payloadKey The payload key
     * @param width Desired thumbnail width
     * @param height Desired thumbnail height
     * @param options Optional operation options
     * @return BytesResponse containing the thumbnail bytes and content type, or null if not found
     */
    suspend fun getThumbBytes(
            targetDrive: TargetDrive,
            fileId: String,
            payloadKey: String,
            width: Int,
            height: Int,
            options: FileOperationOptions = FileOperationOptions()
    ): BytesResponse? {
        require(targetDrive.isValid()) { "TargetDrive must be defined" }
        require(fileId.isNotBlank()) { "FileId must be defined" }
        require(payloadKey.isNotBlank()) { "PayloadKey must be defined" }
        require(width > 0) { "Width must be positive" }
        require(height > 0) { "Height must be positive" }

        val httpClient =
                client.createHttpClient(
                        CreateHttpClientOptions(
                                overrideEncryption = false,
                                fileSystemType = options.fileSystemType
                        )
                )

        val queryParams =
                buildMap<String, String> {
                    put("alias", targetDrive.alias.toString())
                    put("type", targetDrive.type.toString())
                    put("fileId", fileId)
                    put("payloadKey", payloadKey)
                    put("width", width.toString())
                    put("height", height.toString())
                    if (options.lastModified != null) {
                        put("lastModified", options.lastModified.toString())
                    }
                }

        try {
            val response =
                    httpClient.get("/drive/files/thumb") {
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
                        decryptBytes(bytes)
                    } else {
                        bytes
                    }

            return BytesResponse(bytes = resultBytes, contentType = contentType)
        } catch (e: Exception) {
            if (e.message?.contains("404") == true) return null
            KLogger.e(TAG) { "[odin-kt:getThumbBytes] ${e.message}" }
            return null
        } finally {
            httpClient.close()
        }
    }

    /**
     * Gets transfer history for a file.
     *
     * @param targetDrive The target drive containing the file
     * @param fileId The ID of the file
     * @param fileSystemType Optional file system type
     * @return The TransferHistory or null if not found
     */
    suspend fun getTransferHistory(
            targetDrive: TargetDrive,
            fileId: String,
            fileSystemType: FileSystemType = FileSystemType.Standard
    ): TransferHistory? {
        require(targetDrive.isValid()) { "TargetDrive must be defined" }
        require(fileId.isNotBlank()) { "FileId must be defined" }

        val httpClient =
                client.createHttpClient(
                        CreateHttpClientOptions(
                                overrideEncryption = false,
                                fileSystemType = fileSystemType
                        )
                )

        val queryParams =
                buildMap<String, String> {
                    put("alias", targetDrive.alias.toString())
                    put("type", targetDrive.type.toString())
                    put("fileId", fileId)
                }

        try {
            val response =
                    httpClient.get("/drive/files/transfer-history") {
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
            httpClient.close()
        }
    }

    // ==================== DELETE METHODS ====================

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
        require(targetDrive.isValid()) { "TargetDrive must be defined" }
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

    /** Deletes multiple files from the drive by file IDs. */
    suspend fun deleteFiles(
            targetDrive: TargetDrive,
            fileIds: List<String>,
            recipients: List<String>? = null,
            fileSystemType: FileSystemType? = null
    ): Boolean {
        require(targetDrive.isValid()) { "TargetDrive must be defined" }
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

    /** Deletes files from the drive by group IDs. */
    suspend fun deleteFilesByGroupId(
            targetDrive: TargetDrive,
            groupIds: List<String>,
            recipients: List<String>? = null,
            fileSystemType: FileSystemType? = null
    ): Boolean {
        require(targetDrive.isValid()) { "TargetDrive must be defined" }
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

    // ==================== PRIVATE HELPER METHODS ====================

    /** Decrypts the key header using the shared secret. */
    private suspend fun decryptKeyHeader(encryptedKeyHeader: EncryptedKeyHeader): KeyHeader? {
        val sharedSecret = client.getSharedSecret() ?: return null
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
    private suspend fun decryptBytes(bytes: ByteArray): ByteArray {
        // The OdinClient typically handles decryption via OdinEncryptionPlugin
        // For direct byte decryption, we'd need the IV and key header
        // This is handled by the OdinEncryptionPlugin in the HTTP client
        return bytes
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
