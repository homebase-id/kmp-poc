package id.homebase.homebasekmppoc.prototype.lib.drives.upload

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.prototype.lib.base.CredentialsManager
import id.homebase.homebasekmppoc.prototype.lib.base.OdinApiProviderBase
import id.homebase.homebasekmppoc.prototype.lib.client.ApiResponse
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
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

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

data class UploadFileRequest(
    val driveId: Uuid,
    val instructions: UploadInstructionSet,
    val metadata: UploadFileMetadata,
    val payloads: List<PayloadFile>? = null,
    val thumbnails: List<ThumbnailFile>? = null,
    val fileSystemType: FileSystemType? = null
)

data class UpdateFileByFileIdRequest(
    val driveId: Uuid,
    val fileId: Uuid,
    val keyHeader: KeyHeader?,
    val instructions: FileUpdateInstructionSet,
    val metadata: UploadFileMetadata,
    val payloads: List<PayloadFile>? = null,
    val thumbnails: List<ThumbnailFile>? = null,
)

data class UpdateFileByUniqueIdRequest(
    val driveId: Uuid,
    val uniqueId: Uuid,
    val keyHeader: KeyHeader?,
    val instructions: FileUpdateInstructionSet,
    val metadata: UploadFileMetadata,
    val payloads: List<PayloadFile>? = null,
    val thumbnails: List<ThumbnailFile>? = null,
)


/** Provider for drive upload and update operations. Ported from JS/TS odin-js UploadProvider. */
@OptIn(ExperimentalEncodingApi::class)
class DriveUploadProvider(
    private val client: OdinClient,
    httpClient: HttpClient,
    credentialsManager: CredentialsManager
) : OdinApiProviderBase(httpClient, credentialsManager) {

    companion object {
        private const val TAG = "DriveUploadProvider"
        private const val LOCAL_METADATA_TAGS_ENDPOINT = "drive/files/update-local-metadata-tags"
        private const val LOCAL_METADATA_CONTENT_ENDPOINT =
            "drive/files/update-local-metadata-content"
    }

    // ==================== HIGH-LEVEL UPLOAD METHODS ====================

    suspend fun uploadFile(
        request: UploadFileRequest,
        onVersionConflict: (suspend () -> CreateFileResult?)? = null
    ): CreateFileResult? {

        val baseMetadata = request.metadata.copy(isEncrypted = true)

        val keyHeader = KeyHeader.newRandom16()

        val encryptedMetadata = baseMetadata.encryptContent(keyHeader)

        val manifest = UploadManifest.build(request.payloads, request.thumbnails, true)

        val serializableInstructions =
            request.instructions.toSerializable(manifest)

        val sharedSecret = client.getSharedSecret()

        val sharedSecretEncryptedDescriptor =
            if (sharedSecret != null) {
                buildEncryptedUploadDescriptor(
                    keyHeader,
                    encryptedMetadata,
                    sharedSecret,
                    serializableInstructions.transferIv
                )
            } else {
                null
            }

        val data =
            buildUploadFormData(
                instructionSet = serializableInstructions,
                sharedSecretEncryptedDescriptor = sharedSecretEncryptedDescriptor,
                payloads = request.payloads,
                thumbnails = request.thumbnails
            )

        val result = pureUpload(request.driveId, data, request.fileSystemType, onVersionConflict)

        if (result != null) {
            result.keyHeader = keyHeader
        }

        return result
    }

    suspend fun updateFileByFileId(
        request: UpdateFileByFileIdRequest,
        onVersionConflict: (suspend () -> UpdateFileResult?)? = null
    ): UpdateFileResult? {

        val sharedSecret = client.getSharedSecret()

        // Build encrypted descriptor
        val sharedSecretEncryptedDescriptor =
            if (sharedSecret != null) {
                buildSharedSecretEncryptedUpdateDescriptor(
                    request.keyHeader,
                    request.metadata,
                    sharedSecret,
                    request.instructions.transferIv
                )
            } else {
                null
            }

        val data =
            buildUpdateFormData(
                instructionSet = request.instructions,
                sharedSecretEncryptedDescriptor = sharedSecretEncryptedDescriptor,
                payloads = request.payloads,
                thumbnails = request.thumbnails
            )

        val path = "/drives/${request.driveId}/files/${request.fileId}";
        return pureUpdate(data, path, onVersionConflict)
    }

    suspend fun updateFileByUniqueId(
        request: UpdateFileByUniqueIdRequest,
        onVersionConflict: (suspend () -> UpdateFileResult?)? = null
    ): UpdateFileResult? {

        val sharedSecret = client.getSharedSecret()

        // Build encrypted descriptor
        val sharedSecretEncryptedDescriptor =
            if (sharedSecret != null) {
                buildSharedSecretEncryptedUpdateDescriptor(
                    request.keyHeader,
                    request.metadata,
                    sharedSecret,
                    request.instructions.transferIv
                )
            } else {
                null
            }

        val data =
            buildUpdateFormData(
                instructionSet = request.instructions,
                sharedSecretEncryptedDescriptor = sharedSecretEncryptedDescriptor,
                payloads = request.payloads,
                thumbnails = request.thumbnails
            )

        val path = "/drives/${request.driveId}/files/by-uid/${request.uniqueId}";
        return pureUpdate(data, path, onVersionConflict)
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
        driveId: Uuid,
        data: MultiPartFormDataContent,
        fileSystemType: FileSystemType? = null,
        onVersionConflict: (suspend () -> CreateFileResult?)? = null
    ): CreateFileResult? {

        val credentials = requireCreds()
        val queryParams =
            buildMap<String, String> {
                if (fileSystemType != null) {
                    put("xsft", fileSystemType.toString())
                }
            }

        val url =
            apiUrl(
                credentials.domain,
                buildString {
                    append("/drives/${driveId}/files")

                    if (queryParams.isNotEmpty()) {
                        append("?")
                        append(
                            queryParams.entries.joinToString("&") {
                                "${it.key}=${it.value}"
                            }
                        )
                    }
                }
            )


        Logger.i(TAG) { "drive upload url: [${url}]" }

        val response =
            plainPostMultipart(
                url = url,
                token = credentials.accessToken,
                formData = data
            )

        if (response.status in 200..299) {
            return OdinSystemSerializer.deserialize(response.body)
        }

        return handleErrorResponse(response, onVersionConflict) { it() }
    }


    /** Performs a raw update to an existing file on the drive. */
    suspend fun pureUpdate(
        data: MultiPartFormDataContent,
        path: String,
        onVersionConflict: (suspend () -> UpdateFileResult?)? = null
    ): UpdateFileResult? {
        val credentials = requireCreds()

        val url = apiUrl(credentials.domain, path)
        val response =
            plainPatchMultipart(
                url = url,
                token = credentials.accessToken,
                formData = data
            )

        if (response.status in 200..299) {
            return OdinSystemSerializer.deserialize(response.body)
        }

        return handleErrorResponse(response, onVersionConflict) { it() }
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
    private suspend fun buildEncryptedUploadDescriptor(
        keyHeader: KeyHeader?,
        metadata: UploadFileMetadata,
        sharedSecret: ByteArray,
        transferIv: ByteArray
    ): ByteArray {
        // Encrypt the key header using shared secret and transferIv (matches TypeScript
        // encryptKeyHeader)
        val sharedSecretEncryptedKeyHeader =
            EncryptedKeyHeader.encryptKeyHeaderAes(
                keyHeader ?: KeyHeader.empty(),
                transferIv,
                SecureByteArray(sharedSecret)
            )

        // Create the file descriptor
        val descriptor =
            UploadFileDescriptor(
                encryptedKeyHeader = sharedSecretEncryptedKeyHeader,
                fileMetadata = metadata
            )

        // Serialize to JSON (matches TypeScript jsonStringify64)
        val descriptorJson = OdinSystemSerializer.json.encodeToString(descriptor)
        val descriptorBytes = descriptorJson.encodeToByteArray()

        // Encrypt the entire descriptor with sharedSecret using transferIv (matches TypeScript
        // encryptWithSharedSecret)
        return AesCbc.encrypt(descriptorBytes, sharedSecret, transferIv)
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
    private suspend fun buildSharedSecretEncryptedUpdateDescriptor(
        keyHeader: KeyHeader?,
        metadata: UploadFileMetadata,
        sharedSecret: ByteArray,
        transferIv: ByteArray
    ): ByteArray {
        // Encrypt the key header using shared secret and transferIv (matches TypeScript
        // encryptKeyHeader)
        val sharedSecretEncryptedKeyHeader =
            EncryptedKeyHeader.encryptKeyHeaderAes(
                keyHeader ?: KeyHeader.empty(),
                transferIv,
                SecureByteArray(sharedSecret)
            )

        // Create the file descriptor
        val descriptor =
            UpdateFileDescriptor(
                encryptedKeyHeader = sharedSecretEncryptedKeyHeader,
                fileMetadata = metadata
            )

        // Serialize to JSON (matches TypeScript jsonStringify64)
        val descriptorJson = OdinSystemSerializer.json.encodeToString(descriptor)
        val descriptorBytes = descriptorJson.encodeToByteArray()

        // Encrypt the entire descriptor with sharedSecret using transferIv (matches TypeScript
        // encryptWithSharedSecret)
        return AesCbc.encrypt(descriptorBytes, sharedSecret, transferIv)
    }

    private suspend fun handleLocalMetadataError(
        response: HttpResponse,
        onVersionConflict: (suspend () -> LocalMetadataUploadResult?)? = null
    ): LocalMetadataUploadResult? {
        val errorBody = response.bodyAsText()
        val errorResponse =
            try {
                OdinSystemSerializer.json.decodeFromString<OdinErrorResponse>(errorBody)
            } catch (_: Exception) {
                null
            }

        if (errorResponse?.errorCode == OdinClientErrorCode.VersionTagMismatch &&
            onVersionConflict != null
        ) {
            return onVersionConflict()
        }

        KLogger.d(TAG) { "[odin-kt] ${response.status}: $errorBody" }
        throw OdinClientException(
            errorResponse?.message ?: "Request failed",
            errorResponse?.errorCode ?: OdinClientErrorCode.UnhandledScenario
        )
    }

    private suspend fun <T> handleErrorResponse(
        response: ApiResponse,
        onVersionConflict: (suspend () -> T?)? = null,
        invokeCallback: suspend ((suspend () -> T?)) -> T?
    ): T? {

        val errorResponse =
            runCatching {
                OdinSystemSerializer.deserialize<OdinErrorResponse>(response.body)
            }.getOrNull()

        if (
            errorResponse?.errorCode == OdinClientErrorCode.VersionTagMismatch &&
            onVersionConflict != null
        ) {
            return invokeCallback(onVersionConflict)
        }

        // Optional warning if handler not provided
        if (errorResponse?.errorCode == OdinClientErrorCode.VersionTagMismatch) {
            KLogger.w(TAG) {
                "VersionTagMismatch encountered with no onVersionConflict handler"
            }
        }

        throwForFailure(response)
        return null
    }


}
