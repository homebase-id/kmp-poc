package id.homebase.homebasekmppoc.prototype.lib.drives.upload

import id.homebase.homebasekmppoc.prototype.lib.drives.files.PayloadFile
import id.homebase.homebasekmppoc.prototype.lib.drives.files.ThumbnailFile
import id.homebase.homebasekmppoc.prototype.lib.drives.openFileInput
import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders


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
 * @param sharedSecretEncryptedDescriptor Optional encrypted file descriptor
 * @param payloads Optional list of payload files to upload
 * @param thumbnails Optional list of thumbnail files to upload
 * @return MultiPartFormDataContent ready for HTTP upload
 */
suspend fun buildUploadFormData(
    instructionSet: SerializableUploadInstructionSet,
    sharedSecretEncryptedDescriptor: ByteArray? = null,
    payloads: List<PayloadFile>? = null,
    thumbnails: List<ThumbnailFile>? = null
): MultiPartFormDataContent {

    val runtimePayloads =
        payloads?.map { it.toRuntime(::openFileInput) }

    val runtimeThumbnails =
        thumbnails?.map { it.toRuntime(::openFileInput) }

    return buildFormDataInternal(
        instructionSet = instructionSet,
        sharedSecretEncryptedDescriptor = sharedSecretEncryptedDescriptor,
        payloads = runtimePayloads,
        thumbnails = runtimeThumbnails
    )
}

/**
 * Builds a MultiPartFormDataContent for updating files.
 *
 * @param instructionSet The serializable update instruction set (with manifest embedded)
 * @param sharedSecretEncryptedDescriptor Optional encrypted file descriptor
 * @param payloads Optional list of payload files to upload
 * @param thumbnails Optional list of thumbnail files to upload
 * @return MultiPartFormDataContent ready for HTTP update
 */
suspend fun buildUpdateFormData(
    instructionSet: FileUpdateInstructionSet,
    sharedSecretEncryptedDescriptor: ByteArray? = null,
    payloads: List<PayloadFile>? = null,
    thumbnails: List<ThumbnailFile>? = null
): MultiPartFormDataContent
    {
        val runtimePayloads =
            payloads?.map { it.toRuntime(::openFileInput) }

        val runtimeThumbnails =
            thumbnails?.map { it.toRuntime(::openFileInput) }

        return buildFormDataInternal(
            instructionSet = instructionSet,
            sharedSecretEncryptedDescriptor = sharedSecretEncryptedDescriptor,
            payloads = runtimePayloads,
            thumbnails = runtimeThumbnails
        )
    }

/**
 * Internal implementation of buildFormData. Pre-computes all encrypted data before building the
 * form to avoid suspend issues.
 */
private inline fun <reified T> buildFormDataInternal(
    instructionSet: T,
    sharedSecretEncryptedDescriptor: ByteArray?,
    payloads: List<RuntimePayloadFile>?,
    thumbnails: List<RuntimeThumbnailFile>?
): MultiPartFormDataContent {

    val instructionsJson =
        OdinSystemSerializer.json.encodeToString(instructionSet).encodeToByteArray()

    return MultiPartFormDataContent(
        formData {

            // Instructions
            append(
                "instructions",
                instructionsJson,
                Headers.build {
                    append(HttpHeaders.ContentType, "application/json")
                    append(HttpHeaders.ContentDisposition, "form-data; name=\"instructions\"")
                }
            )

            // Encrypted metadata
            if (sharedSecretEncryptedDescriptor != null) {
                append(
                    "metadata",
                    sharedSecretEncryptedDescriptor,
                    Headers.build {
                        append(HttpHeaders.ContentType, "application/octet-stream")
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"metadata\"")
                    }
                )
            }

            // Payloads (streamed)
            payloads?.forEach { payload ->
                append(
                    "payload",
                    payload.input,
                    Headers.build {
                        append(HttpHeaders.ContentType, payload.contentType)
                        append(
                            HttpHeaders.ContentDisposition,
                            "form-data; name=\"payload\"; filename=\"${payload.key}\""
                        )
                    }
                )

            }

            // Thumbnails (streamed)
            thumbnails?.forEach { thumbnail ->
                append(
                    "thumbnail",
                    thumbnail.input,
                    Headers.build {
                        append(HttpHeaders.ContentType, thumbnail.contentType)
                        append(
                            HttpHeaders.ContentDisposition,
                            "form-data; name=\"thumbnail\"; filename=\"${thumbnail.key}${thumbnail.pixelWidth}\""
                        )
                    }
                )
            }
        }
    )
}

data class RuntimePayloadFile(
    val key: String,
    val contentType: String,
    val input: InputProvider
)

fun PayloadFile.toRuntime(
    openInput: (String) -> InputProvider
): RuntimePayloadFile =
    RuntimePayloadFile(
        key = key,
        contentType = contentType,
        input = openInput(filePath)
    )

data class RuntimeThumbnailFile(
    val key: String,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val contentType: String,
    val input: InputProvider
)

fun ThumbnailFile.toRuntime(
    openInput: (String) -> InputProvider
): RuntimeThumbnailFile =
    RuntimeThumbnailFile(
        key = key,
        pixelWidth = pixelWidth,
        pixelHeight = pixelHeight,
        contentType = contentType,
        input = openInput(filePath)
    )

