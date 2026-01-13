package id.homebase.homebasekmppoc.prototype.lib.drives.files

import id.homebase.homebasekmppoc.prototype.lib.ApiServiceExample.CredentialsManager
import id.homebase.homebasekmppoc.prototype.lib.client.ApiResponse
import id.homebase.homebasekmppoc.prototype.lib.client.ByteApiResponse
import id.homebase.homebasekmppoc.prototype.lib.client.OdinApiProviderBase
import co.touchlab.kermit.Logger as KLogger
import id.homebase.homebasekmppoc.prototype.lib.core.OdinClientErrorCode
import id.homebase.homebasekmppoc.prototype.lib.core.OdinClientException
import id.homebase.homebasekmppoc.prototype.lib.core.SecureByteArray
import id.homebase.homebasekmppoc.prototype.lib.crypto.AesCbc
import id.homebase.homebasekmppoc.prototype.lib.crypto.EncryptedKeyHeader
import id.homebase.homebasekmppoc.prototype.lib.crypto.KeyHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.FileSystemType
import id.homebase.homebasekmppoc.prototype.lib.http.CreateHttpClientOptions
import id.homebase.homebasekmppoc.prototype.lib.http.OdinClient
import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.Serializable
import kotlin.coroutines.cancellation.CancellationException
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
    val chunkLength: Long? = null,
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
public class DriveFileProvider(
    httpClient: HttpClient,
    credentialsManager: CredentialsManager
) : OdinApiProviderBase(httpClient, credentialsManager) {

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
        fileId: Uuid
    ): HomebaseFile? {

        ValidationUtil.requireValidUuid(driveId, "driveId")
        ValidationUtil.requireValidUuid(fileId, "fileId")

        val creds = requireCreds()
        val url = apiUrl(
            creds.domain,
            "/drives/$driveId/files/$fileId/header"
        )

        val response = encryptedGet(
            url = url,
            token = creds.accessToken,
            secret = creds.secret
        )

        // 👇 Intentional business rule
        if (response.status == 404) {
            return null
        }

        // 👇 Centralized error handling for everything else
        throwForFailure(response)

        return deserialize<HomebaseFile>(response.body)
    }

    suspend fun getPayloadBytesRaw(
        driveId: Uuid,
        fileId: Uuid,
        key: String,
        options: PayloadOperationOptions = PayloadOperationOptions()
    ): ByteApiResponse? {

        ValidationUtil.requireValidUuid(driveId, "driveId")
        ValidationUtil.requireValidUuid(fileId, "fileId")
        require(key.isNotBlank()) { "Key must be defined" }

        val creds = requireCreds()

        val queryParams =
            buildMap<String, String> {
                options.lastModified?.let {
                    put("lastModified", it.toString())
                }
            }

        val rangeResult =
            DriveFileHelpers.getRangeHeader(
                options.chunkStart,
                options.chunkLength
            )

        val path =
            if (options.chunkStart != null)
                "/drives/$driveId/files/$fileId/payload/$key/${options.chunkStart}/${options.chunkLength ?: ""}"
            else
                "/drives/$driveId/files/$fileId/payload/$key"

        val url = apiUrl(creds.domain, path)

        val response = requestBytes {
            httpClient.get(url) {
                bearerAuth(creds.accessToken)

                queryParams.forEach { (k, v) ->
                    url { parameters.append(k, v) }
                }

                rangeResult.rangeHeader?.let {
                    header(HttpHeaders.Range, it)
                }
            }
        }

        if (response.status == 404) return null

        if (response.status != 200 && response.status != 206) {
            throwForFailure(
                ApiResponse(
                    status = response.status,
                    headers = response.headers,
                    body = ""
                )
            )
        }

        if (response.bytes.isEmpty()) return null

        return response;
    }

    suspend fun getPayloadBytesDecrypted(
        driveId: Uuid,
        fileId: Uuid,
        key: String,
        chunkStart: Long? = null,
        chunkLength: Long? = null
    ): BytesResponse? {

        val raw =
            getPayloadBytesRaw(
                driveId = driveId,
                fileId = fileId,
                key = key,
                options = PayloadOperationOptions(
                    chunkStart = chunkStart,
                    chunkLength = chunkLength,
                    decrypt = false
                )
            ) ?: return null

        val rangeResult =
            DriveFileHelpers.getRangeHeader(chunkStart, chunkLength)

        val decryptedBytes =
            if (rangeResult.updatedChunkStart != null) {
                val decrypted =
                    decryptChunkedBytes(
                        raw.headers,
                        raw.bytes,
                        startOffset = rangeResult.startOffset,
                        chunkStart = (chunkStart ?: 0).toInt()
                    )

                val sliceEnd =
                    if (chunkLength != null && chunkStart != null) {
                        (chunkLength - chunkStart).toInt()
                    } else {
                        decrypted.size
                    }

                decrypted.sliceArray(0 until minOf(sliceEnd, decrypted.size))
            } else {
                decryptBytes(raw.headers, raw.bytes)
            }

        return BytesResponse(
            bytes = decryptedBytes,
            contentType = raw.contentType
        )
    }


    suspend fun getThumbBytesRaw(
        driveId: Uuid,
        fileId: Uuid,
        payloadKey: String,
        width: Int,
        height: Int,
        lastModified: Long? = null
    ): ByteApiResponse? {

        ValidationUtil.requireValidUuid(driveId, "driveId")
        ValidationUtil.requireValidUuid(fileId, "fileId")
        require(payloadKey.isNotBlank()) { "PayloadKey must be defined" }
        require(width > 0) { "Width must be positive" }
        require(height > 0) { "Height must be positive" }

        val creds = requireCreds()

        val queryParams =
            buildMap<String, String> {
                put("width", width.toString())
                put("height", height.toString())
                lastModified?.let {
                    put("lastModified", it.toString())
                }
            }

        val url =
            apiUrl(
                creds.domain,
                "/drives/$driveId/files/$fileId/payload/$payloadKey/thumb"
            )

        val response = requestBytes {
            httpClient.get(url) {
                bearerAuth(creds.accessToken)
                queryParams.forEach { (k, v) ->
                    url { parameters.append(k, v) }
                }
            }
        }

        if (response.status == 404) return null

        if (response.status !in 200..299) {
            throwForFailure(
                ApiResponse(
                    status = response.status,
                    headers = response.headers,
                    body = ""
                )
            )
        }

        if (response.bytes.isEmpty()) return null

        return response;
    }

    suspend fun getThumbBytesDecrypted(
        driveId: Uuid,
        fileId: Uuid,
        payloadKey: String,
        width: Int,
        height: Int,
        lastModified: Long? = null
    ): BytesResponse? {

        val raw =
            getThumbBytesRaw(
                driveId = driveId,
                fileId = fileId,
                payloadKey = payloadKey,
                width = width,
                height = height,
                lastModified = lastModified
            ) ?: return null

        val decryptedBytes =
            decryptBytes(raw.headers, raw.bytes)

        return BytesResponse(
            bytes = decryptedBytes,
            contentType = raw.contentType
        )
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

        val method = if (hardDelete) "hard-delete" else "delete"
        val endpoint = "drives/${driveId}/files/${fileId}/${method}"

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
        val sharedSecret = credentialsManager.getActiveCredentials()?.sharedSecret ?: return null
        return encryptedKeyHeader.decryptAesToKeyHeader(sharedSecret)
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
     * Decrypts bytes using the shared secret (full payload/thumbnail decryption).
     */
    private suspend fun decryptBytes(
        headers: Headers,
        bytes: ByteArray
    ): ByteArray {

        val payloadEncrypted =
            headers["payloadencrypted"]?.equals("true", ignoreCase = true) == true

        val encryptedHeader64 =
            headers["sharedsecretencryptedheader64"]

        return when {
            payloadEncrypted && encryptedHeader64 != null -> {
                val encryptedKeyHeader =
                    EncryptedKeyHeader.fromBase64(encryptedHeader64)

                val keyHeader =
                    decryptKeyHeader(encryptedKeyHeader)
                        ?: error("Missing shared secret")

                decryptUsingKeyHeader(bytes, keyHeader)
            }

            payloadEncrypted ->
                error("Can't decrypt; missing keyheader")

            else ->
                bytes
        }
    }


    /** Decrypts chunked bytes with offset handling. */
    suspend fun decryptChunkedBytes(
        headers: Headers,
        responseBytes: ByteArray,
        startOffset: Int,
        chunkStart: Int
    ): ByteArray {

        val payloadEncrypted =
            headers["payloadencrypted"]?.equals("True", ignoreCase = false) == true

        val encryptedHeader64 =
            headers["sharedsecretencryptedheader64"]

        if (payloadEncrypted && encryptedHeader64 != null) {

            val encryptedKeyHeader = EncryptedKeyHeader.fromBase64(encryptedHeader64)
            val keyHeader = decryptKeyHeader(encryptedKeyHeader)
                ?: throw IllegalStateException("Can't decrypt; missing key header")

            val key = keyHeader.aesKey

            val (iv, cipher) = run {
                val padding = ByteArray(16) { 16 }

                val encryptedPadding =
                    AesCbc.encrypt(
                        padding,
                        key,
                        iv = responseBytes.copyOfRange(
                            responseBytes.size - 16,
                            responseBytes.size
                        )
                    ).copyOfRange(0, 16)

                if (chunkStart == 0) {
                    // First block
                    Pair(
                        keyHeader.iv,
                        mergeByteArrays(
                            listOf(responseBytes, encryptedPadding)
                        )
                    )
                } else {
                    // Middle blocks
                    Pair(
                        responseBytes.copyOfRange(0, 16),
                        mergeByteArrays(
                            listOf(
                                responseBytes.copyOfRange(16, responseBytes.size),
                                encryptedPadding
                            )
                        )
                    )
                }
            }

            val decryptedBytes = AesCbc.decrypt(cipher, key, iv)

            // Match TS behavior:
            // decryptedBytes.slice(startOffset ? startOffset - 16 : 0)
            val sliceStart =
                if (startOffset > 0) maxOf(startOffset - 16, 0) else 0

            return decryptedBytes.copyOfRange(sliceStart, decryptedBytes.size)

        } else {
            // Not encrypted → return raw bytes with offset
            return responseBytes.copyOfRange(startOffset, responseBytes.size)
        }
    }


    fun mergeByteArrays(chunks: List<ByteArray>): ByteArray {
        var size = 0
        for (chunk in chunks) {
            size += chunk.size
        }

        val merged = ByteArray(size)
        var offset = 0

        for (chunk in chunks) {
            chunk.copyInto(
                destination = merged,
                destinationOffset = offset
            )
            offset += chunk.size
        }

        return merged
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
