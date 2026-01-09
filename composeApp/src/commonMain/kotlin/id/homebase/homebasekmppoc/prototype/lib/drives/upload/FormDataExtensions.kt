package id.homebase.homebasekmppoc.prototype.lib.drives.upload

import id.homebase.homebasekmppoc.prototype.lib.crypto.KeyHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.files.PayloadFile
import id.homebase.homebasekmppoc.prototype.lib.drives.files.ThumbnailFile
import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

/** Common interface for payload descriptors that have an IV field. */
interface PayloadDescriptorWithIv {
    val payloadKey: String
    val iv: ByteArray?
}

/** Extension to make UploadPayloadDescriptor compatible with PayloadDescriptorWithIv. */
private fun UploadPayloadDescriptor.toPayloadDescriptorWithIv() =
    object : PayloadDescriptorWithIv {
        override val payloadKey: String = this@toPayloadDescriptorWithIv.payloadKey
        override val iv: ByteArray? = this@toPayloadDescriptorWithIv.iv
    }

/** Extension to make UpdatePayloadInstruction compatible with PayloadDescriptorWithIv. */
private fun UpdatePayloadInstruction.toPayloadDescriptorWithIv() =
    object : PayloadDescriptorWithIv {
        override val payloadKey: String = this@toPayloadDescriptorWithIv.payloadKey
        override val iv: ByteArray? = this@toPayloadDescriptorWithIv.iv
    }

/** Pre-computed data for a payload ready to be added to form data. */
private data class ProcessedPayload(
    val key: String,
    val contentType: String,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ProcessedPayload

        if (key != other.key) return false
        if (contentType != other.contentType) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + contentType.hashCode()
        return result
    }
}

/** Pre-computed data for a thumbnail ready to be added to form data. */
private data class ProcessedThumbnail(
    val filename: String,
    val contentType: String,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ProcessedThumbnail

        if (filename != other.filename) return false
        if (contentType != other.contentType) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = filename.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * Builds a MultiPartFormDataContent for uploading files. Ported from TypeScript buildFormData
 * function.
 *
 * @param instructionSet The serializable upload instruction set (with manifest embedded)
 * @param encryptedDescriptor Optional encrypted file descriptor
 * @param payloads Optional list of payload files to upload
 * @param thumbnails Optional list of thumbnail files to upload
 * @param keyHeader Optional key header for encryption
 * @param manifest Optional manifest for payload descriptor IVs (for encryption)
 * @return MultiPartFormDataContent ready for HTTP upload
 */
suspend fun buildFormData(
    instructionSet: SerializableUploadInstructionSet,
    encryptedDescriptor: ByteArray? = null,
    payloads: List<PayloadFile>? = null,
    thumbnails: List<ThumbnailFile>? = null,
    keyHeader: KeyHeader? = null,
    manifest: UploadManifest? = null
): MultiPartFormDataContent =
    buildFormDataInternal(
        instructionSet = instructionSet,
        encryptedMetadataDescriptor = encryptedDescriptor,
        payloads = payloads,
        thumbnails = thumbnails,
        keyHeader = keyHeader,
        payloadDescriptors =
            manifest?.payloadDescriptors?.map { it.toPayloadDescriptorWithIv() }
    )

/**
 * Builds a MultiPartFormDataContent for updating files.
 *
 * @param instructionSet The serializable update instruction set (with manifest embedded)
 * @param encryptedDescriptor Optional encrypted file descriptor
 * @param payloads Optional list of payload files to upload
 * @param thumbnails Optional list of thumbnail files to upload
 * @param keyHeader Optional key header for encryption
 * @param manifest Optional manifest for payload descriptor IVs (for encryption)
 * @return MultiPartFormDataContent ready for HTTP update
 */
suspend fun buildFormData(
    instructionSet: SerializableUpdateLocalInstructionSet,
    encryptedDescriptor: ByteArray? = null,
    payloads: List<PayloadFile>? = null,
    thumbnails: List<ThumbnailFile>? = null,
    keyHeader: KeyHeader? = null,
    manifest: UpdateManifest? = null
): MultiPartFormDataContent =
    buildFormDataInternal(
        instructionSet = instructionSet,
        encryptedMetadataDescriptor = encryptedDescriptor,
        payloads = payloads,
        thumbnails = thumbnails,
        keyHeader = keyHeader,
        payloadDescriptors =
            manifest?.payloadDescriptors?.map { it.toPayloadDescriptorWithIv() }
    )

/**
 * Builds a MultiPartFormDataContent for peer updates.
 *
 * @param instructionSet The update instruction set (peer)
 * @param encryptedDescriptor Optional encrypted file descriptor
 * @param payloads Optional list of payload files to upload
 * @param thumbnails Optional list of thumbnail files to upload
 * @param keyHeader Optional key header for encryption
 * @param manifest Optional manifest containing payload descriptors with IVs
 * @return MultiPartFormDataContent ready for HTTP update
 */
suspend fun buildFormData(
    instructionSet: UpdatePeerInstructionSet,
    encryptedDescriptor: ByteArray? = null,
    payloads: List<PayloadFile>? = null,
    thumbnails: List<ThumbnailFile>? = null,
    keyHeader: KeyHeader? = null,
    manifest: UpdateManifest? = null
): MultiPartFormDataContent =
    buildFormDataInternal(
        instructionSet = instructionSet,
        encryptedMetadataDescriptor = encryptedDescriptor,
        payloads = payloads,
        thumbnails = thumbnails,
        keyHeader = keyHeader,
        payloadDescriptors =
            manifest?.payloadDescriptors?.map { it.toPayloadDescriptorWithIv() }
    )

/**
 * Internal implementation of buildFormData. Pre-computes all encrypted data before building the
 * form to avoid suspend issues.
 */
private suspend inline fun <reified T> buildFormDataInternal(
    instructionSet: T,
    encryptedMetadataDescriptor: ByteArray?,
    payloads: List<PayloadFile>?,
    thumbnails: List<ThumbnailFile>?,
    keyHeader: KeyHeader?,
    payloadDescriptors: List<PayloadDescriptorWithIv>?
): MultiPartFormDataContent {
    // Pre-compute all encrypted payloads
    val processedPayloads =
        payloads?.map { payload ->
            ProcessedPayload(
                key = payload.key,
                contentType = payload.contentType,
                data = processPayload(payload, keyHeader, payloadDescriptors)
            )
        }

    // Pre-compute all encrypted thumbnails
    val processedThumbnails =
        thumbnails?.map { thumbnail ->
            ProcessedThumbnail(
                filename = "${thumbnail.key}${thumbnail.pixelWidth}",
                contentType = thumbnail.contentType,
                data = processThumbnail(thumbnail, keyHeader, payloadDescriptors)
            )
        }

    // Serialize instructions
    val instructionsJson =
        OdinSystemSerializer.json.encodeToString(instructionSet).encodeToByteArray()

    return MultiPartFormDataContent(
        formData {
            // Append instructions as JSON blob
            append(
                "instructions",
                instructionsJson,
                Headers.build {
                    append(HttpHeaders.ContentType, "application/json")
                    append(
                        HttpHeaders.ContentDisposition,
                        "form-data; name=\"instructions\""
                    )
                }
            )

            // Append encrypted metadata if present
            if (encryptedMetadataDescriptor != null) {
                append(
                    "metadata",
                    encryptedMetadataDescriptor,
                    Headers.build {
                        append(
                            HttpHeaders.ContentType,
                            "application/octet-stream"
                        )
                        append(
                            HttpHeaders.ContentDisposition,
                            "form-data; name=\"metadata\""
                        )
                    }
                )
            }

            // Append pre-processed payloads
            processedPayloads?.forEach { payload ->
                append(
                    "payload",
                    payload.data,
                    Headers.build {
                        append(
                            HttpHeaders.ContentType,
                            payload.contentType
                        )
                        append(
                            HttpHeaders.ContentDisposition,
                            "form-data; name=\"payload\"; filename=\"${payload.key}\""
                        )
                    }
                )
            }

            // Append pre-processed thumbnails
            processedThumbnails?.forEach { thumbnail ->
                append(
                    "thumbnail",
                    thumbnail.data,
                    Headers.build {
                        append(
                            HttpHeaders.ContentType,
                            thumbnail.contentType
                        )
                        append(
                            HttpHeaders.ContentDisposition,
                            "form-data; name=\"thumbnail\"; filename=\"${thumbnail.filename}\""
                        )
                    }
                )
            }
        }
    )
}

/** Processes a payload file with optional encryption. */
private suspend fun processPayload(
    payload: PayloadFile,
    keyHeader: KeyHeader?,
    payloadDescriptors: List<PayloadDescriptorWithIv>?
): ByteArray {
    return if (keyHeader != null && !payload.skipEncryption) {
        // Find the IV from the manifest or use the keyHeader's IV
        val iv =
            payloadDescriptors?.find { it.payloadKey == payload.key }?.iv
                ?: keyHeader.iv
        keyHeader.encryptDataAes(payload.payload, iv)
    } else {
        payload.payload
    }
}

/** Processes a thumbnail file with optional encryption. */
private suspend fun processThumbnail(
    thumbnail: ThumbnailFile,
    keyHeader: KeyHeader?,
    payloadDescriptors: List<PayloadDescriptorWithIv>?
): ByteArray {
    return if (keyHeader != null && !thumbnail.skipEncryption) {
        // Find the IV from the manifest or use the keyHeader's IV
        val iv =
            payloadDescriptors?.find { it.payloadKey == thumbnail.key }?.iv
                ?: keyHeader.iv
        keyHeader.encryptDataAes(thumbnail.payload, iv)
    } else {
        thumbnail.payload
    }
}
