package id.homebase.homebasekmppoc.prototype.lib.drives.upload

import co.touchlab.kermit.Logger as KLogger
import id.homebase.homebasekmppoc.prototype.lib.core.OdinClientErrorCode
import id.homebase.homebasekmppoc.prototype.lib.core.OdinClientException
import id.homebase.homebasekmppoc.prototype.lib.core.OdinErrorResponse
import id.homebase.homebasekmppoc.prototype.lib.core.SecureByteArray
import id.homebase.homebasekmppoc.prototype.lib.crypto.AesCbc
import id.homebase.homebasekmppoc.prototype.lib.crypto.ByteArrayUtil
import id.homebase.homebasekmppoc.prototype.lib.crypto.EncryptedKeyHeader
import id.homebase.homebasekmppoc.prototype.lib.crypto.KeyHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.FileSystemType
import id.homebase.homebasekmppoc.prototype.lib.drives.TargetDrive
import id.homebase.homebasekmppoc.prototype.lib.drives.files.HomebaseFile
import id.homebase.homebasekmppoc.prototype.lib.drives.files.PayloadFile
import id.homebase.homebasekmppoc.prototype.lib.drives.files.ThumbnailFile
import id.homebase.homebasekmppoc.prototype.lib.http.CreateHttpClientOptions
import id.homebase.homebasekmppoc.prototype.lib.http.OdinClient
import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.Serializable

/** Local metadata upload result. */
@Serializable
data class LocalMetadataUploadResult(val newLocalVersionTag: String)

/** Local app data for metadata updates. */
data class LocalAppData(
    val versionTag: String? = null,
    val tags: List<String>? = null,
    val content: String? = null,
    val iv: String? = null
)

// Request classes for local metadata operations
@Serializable
private data class LocalMetadataFileReference(
    val fileId: String,
    val targetDrive: LocalMetadataTargetDrive
)

@Serializable
private data class LocalMetadataTargetDrive(val alias: String, val type: String)

@Serializable
private data class UpdateLocalMetadataTagsRequest(
    val localVersionTag: String?,
    val file: LocalMetadataFileReference,
    val tags: List<String>?
)

@Serializable
private data class UpdateLocalMetadataContentRequest(
    val iv: String? = null,
    val localVersionTag: String?,
    val file: LocalMetadataFileReference,
    val content: String?
)

/** Provider for drive upload and update operations. Ported from JS/TS odin-js UploadProvider. */
@OptIn(ExperimentalEncodingApi::class)
class DriveUploadProvider(private val client: OdinClient) {

    companion object {
        private const val TAG = "DriveUploadProvider"
        private const val LOCAL_METADATA_TAGS_ENDPOINT = "drive/files/update-local-metadata-tags"
        private const val LOCAL_METADATA_CONTENT_ENDPOINT =
            "drive/files/update-local-metadata-content"
    }

    // ==================== HIGH-LEVEL UPLOAD METHODS ====================

    /**
     * Uploads a file with optional encryption.
     *
     * @param instructions The upload instruction set
     * @param metadata The file metadata
     * @param payloads Optional list of payload files
     * @param thumbnails Optional list of thumbnail files
     * @param encrypt Whether to encrypt the file (default true)
     * @param onVersionConflict Optional callback for version conflict handling
     * @param aesKey Optional pre-existing AES key to use
     * @return The upload result with keyHeader, or null if handled by conflict callback
     */
    suspend fun uploadFile(
        instructions: UploadInstructionSet,
        metadata: UploadFileMetadata,
        payloads: List<PayloadFile>? = null,
        thumbnails: List<ThumbnailFile>? = null,
        encrypt: Boolean = true,
        onVersionConflict: (suspend () -> CreateFileResult?)? = null,
        aesKey: ByteArray? = null
    ): CreateFileResult? {
        // Validate version tag usage


        // Force isEncrypted on metadata to match encrypt flag
        val shouldEncrypt = encrypt || aesKey != null
        val baseMetadata = metadata.copy(isEncrypted = shouldEncrypt)

        // Generate key header if encrypting
        val keyHeader =
            if (shouldEncrypt) {
                if (aesKey != null) {
                    KeyHeader(
                        iv = ByteArrayUtil.getRndByteArray(16),
                        aesKey = SecureByteArray(aesKey)
                    )
                } else {
                    KeyHeader.newRandom16()
                }
            } else {
                null
            }

        // Encrypt metadata content using UploadFileMetadata.encryptContent()
        val encryptedMetadata = baseMetadata.encryptContent(keyHeader)

        // Build manifest
        val manifest = UploadManifest.build(payloads, thumbnails, shouldEncrypt)

        // Create serializable instruction set with manifest (matches TypeScript
        // instructionsWithManifest)
        val serializableInstructions = instructions.toSerializable(manifest)

        // Get shared secret for encrypting key header
        val sharedSecret = client.getSharedSecret()

        // Build encrypted descriptor (encrypted key header + encrypted metadata + all encrypted
        // with sharedSecret)
        val encryptedDescriptor =
            if (sharedSecret != null) {
                buildEncryptedDescriptor(
                    keyHeader,
                    encryptedMetadata,
                    sharedSecret,
                    serializableInstructions.transferIv
                )
            } else {
                null
            }

        // Build form data
        val data =
            buildFormData(
                instructionSet = serializableInstructions,
                encryptedDescriptor = encryptedDescriptor,
                payloads = payloads,
                thumbnails = thumbnails,
                keyHeader = keyHeader,
                manifest = manifest
            )

        // Upload
        val result = pureUpload(data, instructions.systemFileType, onVersionConflict)

        if (result != null) {
            result.keyHeader = keyHeader
        }
        return result
    }

    /**
     * Patches (updates) an existing file.
     *
     * @param keyHeader The key header (encrypted or decrypted)
     * @param instructions The update instruction set
     * @param metadata The file metadata
     * @param payloads Optional list of payload files to add/update
     * @param thumbnails Optional list of thumbnail files
     * @param toDeletePayloads Optional list of payloads to delete
     * @param onVersionConflict Optional callback for version conflict handling
     * @return The update result, or null if handled by conflict callback
     */
    suspend fun patchFile(
        keyHeader: Any?, // EncryptedKeyHeader | KeyHeader | null
        instructions: UpdateLocalInstructionSet,
        metadata: UploadFileMetadata,
        payloads: List<PayloadFile>? = null,
        thumbnails: List<ThumbnailFile>? = null,
        toDeletePayloads: List<PayloadDeleteKey>? = null,
        onVersionConflict: (suspend () -> UpdateFileResult?)? = null
    ): UpdateFileResult? {
        // Decrypt key header if encrypted
        val decryptedKeyHeader: KeyHeader? =
            when (keyHeader) {
                is EncryptedKeyHeader -> {
                    val sharedSecret = client.getSharedSecret()
                    if (sharedSecret != null) {
                        keyHeader.decryptAesToKeyHeader(SecureByteArray(sharedSecret))
                    } else {
                        null
                    }
                }

                is KeyHeader -> keyHeader
                else -> null
            }

        // Generate new IV for the key header if decrypted
        val updatedKeyHeader =
            decryptedKeyHeader?.let {
                KeyHeader(iv = ByteArrayUtil.getRndByteArray(16), aesKey = it.aesKey)
            }

        // Encrypt metadata content
        val encryptedMetadata = metadata.encryptContent(updatedKeyHeader)

        // Build update manifest
        val manifest =
            UpdateManifest.build(
                payloads = payloads,
                toDeletePayloads = toDeletePayloads,
                thumbnails = thumbnails,
                generateIv = updatedKeyHeader != null
            )

        // Create serializable instruction set with manifest
        val serializableInstructions = instructions.toSerializable(manifest)

        // Get shared secret for encrypting key header
        val sharedSecret = client.getSharedSecret()

        // Build encrypted descriptor
        val encryptedDescriptor =
            if (updatedKeyHeader != null && sharedSecret != null) {
                buildEncryptedDescriptor(
                    updatedKeyHeader,
                    encryptedMetadata,
                    sharedSecret,
                    serializableInstructions.transferIv
                )
            } else {
                null
            }

        // Build form data
        val data =
            buildFormData(
                instructionSet = serializableInstructions,
                encryptedDescriptor = encryptedDescriptor,
                payloads = payloads,
                thumbnails = thumbnails,
                keyHeader = updatedKeyHeader,
                manifest = manifest
            )

        // Update
        return pureUpdate(data, onVersionConflict)
    }

    // ==================== LOCAL METADATA METHODS ====================

    /** Updates local metadata tags for a file. */
    suspend fun uploadLocalMetadataTags(
        file: FileIdFileIdentifier,
        localAppData: LocalAppData,
        onVersionConflict: (suspend () -> LocalMetadataUploadResult?)? = null
    ): LocalMetadataUploadResult? {
        val httpClient = client.createHttpClient(CreateHttpClientOptions(overrideEncryption = true))

        val requestBody =
            UpdateLocalMetadataTagsRequest(
                localVersionTag = localAppData.versionTag,
                file =
                    LocalMetadataFileReference(
                        fileId = file.fileId,
                        targetDrive =
                            LocalMetadataTargetDrive(
                                alias =
                                    file.targetDrive.alias
                                        .toString(),
                                type =
                                    file.targetDrive.type
                                        .toString()
                            )
                    ),
                tags = localAppData.tags
            )
                .let { OdinSystemSerializer.json.encodeToString(it) }

        try {
            val response =
                httpClient.patch(LOCAL_METADATA_TAGS_ENDPOINT) {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                return OdinSystemSerializer.json.decodeFromString<LocalMetadataUploadResult>(body)
            }

            return handleLocalMetadataError(response, onVersionConflict)
        } finally {
            // httpClient.close()
        }
    }

    /** Updates local metadata content for a file. */
    suspend fun uploadLocalMetadataContent(
        targetDrive: TargetDrive,
        file: HomebaseFile,
        localAppData: LocalAppData,
        onVersionConflict: (suspend () -> LocalMetadataUploadResult?)? = null
    ): LocalMetadataUploadResult? {
        val fileIdentifier =
            FileIdFileIdentifier(fileId = file.fileId.toString(), targetDrive = targetDrive)

        // Decrypt key header if file is encrypted
        val decryptedKeyHeader: KeyHeader? =
            if (file.fileMetadata.isEncrypted) {
                val sharedSecret = client.getSharedSecret()
                if (sharedSecret != null) {
                    file.sharedSecretEncryptedKeyHeader.decryptAesToKeyHeader(
                        SecureByteArray(sharedSecret)
                    )
                } else {
                    throw OdinClientException(
                        "Missing shared secret for encrypted file",
                        OdinClientErrorCode.SharedSecretEncryptionIsInvalid
                    )
                }
            } else {
                null
            }

        // Generate new IV and encrypt content if needed
        val keyHeader: KeyHeader? =
            if (file.fileMetadata.isEncrypted && decryptedKeyHeader != null) {
                KeyHeader(
                    iv = localAppData.iv?.let { Base64.decode(it) }
                        ?: ByteArrayUtil.getRndByteArray(16),
                    aesKey = decryptedKeyHeader.aesKey
                )
            } else {
                null
            }

        val (ivToSend, encryptedContent) =
            if (keyHeader != null && localAppData.content != null) {
                val contentBytes = localAppData.content.encodeToByteArray()
                val encrypted = keyHeader.encryptDataAes(contentBytes)
                Base64.encode(keyHeader.iv) to Base64.encode(encrypted)
            } else {
                null to localAppData.content
            }

        val httpClient = client.createHttpClient(CreateHttpClientOptions(overrideEncryption = true))

        val requestBody =
            UpdateLocalMetadataContentRequest(
                iv = ivToSend,
                localVersionTag = localAppData.versionTag,
                file =
                    LocalMetadataFileReference(
                        fileId = fileIdentifier.fileId,
                        targetDrive =
                            LocalMetadataTargetDrive(
                                alias =
                                    fileIdentifier.targetDrive
                                        .alias.toString(),
                                type =
                                    fileIdentifier.targetDrive
                                        .type.toString()
                            )
                    ),
                content = encryptedContent
            )
                .let { OdinSystemSerializer.json.encodeToString(it) }

        try {
            val response =
                httpClient.patch(LOCAL_METADATA_CONTENT_ENDPOINT) {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                return OdinSystemSerializer.json.decodeFromString<LocalMetadataUploadResult>(body)
            }

            return handleLocalMetadataError(response, onVersionConflict)
        } finally {
            // httpClient.close()
        }
    }

    // ==================== LOW-LEVEL UPLOAD METHODS ====================

    /** Performs a raw upload to the drive. */
    suspend fun pureUpload(
        data: MultiPartFormDataContent,
        fileSystemType: FileSystemType? = null,
        onVersionConflict: (suspend () -> CreateFileResult?)? = null
    ): CreateFileResult? {
        val httpClient =
            client.createHttpClient(CreateHttpClientOptions(overrideEncryption = true))

        try {
            val queryParams =
                buildMap<String, String> {
                    if (fileSystemType != null) {
                        put("xsft", fileSystemType.toString())
                    }
                }

            val url = "drives/files"
            val response: HttpResponse =
                httpClient.post(url) {
                    url {
                        queryParams.forEach { (key, value) ->
                            parameters.append(key, value)
                        }
                    }
                    setBody(data)
                }

            return handleUploadResponse(response, onVersionConflict)
        } catch (e: OdinClientException) {
            throw e
        } catch (e: Exception) {
            KLogger.e(TAG) { "Upload failed: ${e.message}" }
            throw OdinClientException(
                "Upload failed: ${e.message}",
                OdinClientErrorCode.UnhandledScenario,
                e
            )
        } finally {
            // httpClient.close()
        }
    }

    /** Performs a raw update to an existing file on the drive. */
    suspend fun pureUpdate(
        data: MultiPartFormDataContent,
        onVersionConflict: (suspend () -> UpdateFileResult?)? = null
    ): UpdateFileResult? {
        val httpClient =
            client.createHttpClient(
                CreateHttpClientOptions(
                    overrideEncryption = true
                )
            )

        try {
            val url = "drives/files"
            val response: HttpResponse = httpClient.patch(url) { setBody(data) }
            return handleUpdateResponse(response, onVersionConflict)
        } catch (e: OdinClientException) {
            throw e
        } catch (e: Exception) {
            KLogger.e(TAG) { "Update failed: ${e.message}" }
            throw OdinClientException(
                "Update failed: ${e.message}",
                OdinClientErrorCode.UnhandledScenario,
                e
            )
        } finally {
            // using a shared client per gpt
            // httpClient.close()
        }
    }

    /**
     * Builds an encrypted descriptor (UploadFileDescriptor encrypted with sharedSecret). Matches
     * TypeScript buildDescriptor function.
     *
     * @param keyHeader The key header to encrypt
     * @param metadata The already-encrypted file metadata
     * @param sharedSecret The shared secret for encryption
     * @param transferIv The transfer IV from instructions
     * @return AES-CBC encrypted descriptor bytes
     */
    private suspend fun buildEncryptedDescriptor(
        keyHeader: KeyHeader?,
        metadata: UploadFileMetadata,
        sharedSecret: ByteArray,
        transferIv: ByteArray
    ): ByteArray {
        // Encrypt the key header using shared secret and transferIv (matches TypeScript
        // encryptKeyHeader)
        val encryptedKeyHeader =
            EncryptedKeyHeader.encryptKeyHeaderAes(
                keyHeader ?: KeyHeader.empty(),
                transferIv,
                SecureByteArray(sharedSecret)
            )

        // Create the file descriptor
        val descriptor =
            UploadFileDescriptor(
                encryptedKeyHeader = encryptedKeyHeader,
                fileMetadata = metadata
            )

        // Serialize to JSON (matches TypeScript jsonStringify64)
        val descriptorJson = OdinSystemSerializer.json.encodeToString(descriptor)
        val descriptorBytes = descriptorJson.encodeToByteArray()

        // Encrypt the entire descriptor with sharedSecret using transferIv (matches TypeScript
        // encryptWithSharedSecret)
        return AesCbc.encrypt(descriptorBytes, sharedSecret, transferIv)
    }

    private suspend fun handleUploadResponse(
        response: HttpResponse,
        onVersionConflict: (suspend () -> CreateFileResult?)? = null
    ): CreateFileResult? {
        return when {
            response.status.isSuccess() -> {
                val body = response.bodyAsText()
                OdinSystemSerializer.json.decodeFromString<CreateFileResult>(body)
            }

            else -> handleErrorResponse(response, onVersionConflict) { it() }
        }
    }

    private suspend fun handleUpdateResponse(
        response: HttpResponse,
        onVersionConflict: (suspend () -> UpdateFileResult?)? = null
    ): UpdateFileResult? {
        return when {
            response.status.isSuccess() -> {
                val body = response.bodyAsText()
                OdinSystemSerializer.json.decodeFromString<UpdateFileResult>(body)
            }

            else -> handleErrorResponse(response, onVersionConflict) { it() }
        }
    }

    private suspend fun handleLocalMetadataError(
        response: HttpResponse,
        onVersionConflict: (suspend () -> LocalMetadataUploadResult?)? = null
    ): LocalMetadataUploadResult? {
        val errorBody = response.bodyAsText()
        val errorResponse =
            try {
                OdinSystemSerializer.json.decodeFromString<OdinErrorResponse>(errorBody)
            } catch (e: Exception) {
                null
            }

        if (errorResponse?.errorCode == OdinClientErrorCode.VersionTagMismatch &&
            onVersionConflict != null
        ) {
            return onVersionConflict()
        }

        KLogger.e(TAG) { "[odin-kt] ${response.status}: $errorBody" }
        throw OdinClientException(
            errorResponse?.message ?: "Request failed",
            errorResponse?.errorCode ?: OdinClientErrorCode.UnhandledScenario
        )
    }

    private suspend fun <T> handleErrorResponse(
        response: HttpResponse,
        onVersionConflict: (suspend () -> T?)? = null,
        invokeCallback: suspend ((suspend () -> T?)) -> T?
    ): T? {
        val body = response.bodyAsText()

        val errorResponse =
            try {
                OdinSystemSerializer.json.decodeFromString<OdinErrorResponse>(body)
            } catch (e: Exception) {
                null
            }

        if (errorResponse?.errorCode == OdinClientErrorCode.VersionTagMismatch) {
            if (onVersionConflict == null) {
                KLogger.w(TAG) {
                    "VersionTagMismatch, to avoid this, add an onVersionConflict handler"
                }
            } else {
                return invokeCallback(onVersionConflict)
            }
        }

        when (response.status.value) {
            400 -> KLogger.e(TAG) { "Bad Request: $body" }
            else -> KLogger.e(TAG) { "Request failed with status ${response.status.value}: $body" }
        }

        val clientErrorCode =
            errorResponse?.errorCode
                ?: when (response.status.value) {
                    400 -> OdinClientErrorCode.ArgumentError
                    401, 403 -> OdinClientErrorCode.InvalidAuthToken
                    404 -> OdinClientErrorCode.FileNotFound
                    in 500..599 ->
                        OdinClientErrorCode.RemoteServerReturnedInternalServerError

                    else -> OdinClientErrorCode.UnhandledScenario
                }

        throw OdinClientException(
            errorResponse?.message ?: "Request failed with status ${response.status.value}",
            clientErrorCode
        )
    }
}
