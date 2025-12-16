package id.homebase.homebasekmppoc.prototype.lib.drives.upload

import co.touchlab.kermit.Logger as KLogger
import id.homebase.homebasekmppoc.prototype.lib.core.OdinClientErrorCode
import id.homebase.homebasekmppoc.prototype.lib.core.OdinClientException
import id.homebase.homebasekmppoc.prototype.lib.core.OdinErrorResponse
import id.homebase.homebasekmppoc.prototype.lib.drives.FileSystemType
import id.homebase.homebasekmppoc.prototype.lib.http.CreateHttpClientOptions
import id.homebase.homebasekmppoc.prototype.lib.http.OdinClient
import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * Provider for drive upload and update operations. Ported from JS/TS odin-js pureUpload and
 * pureUpdate functions.
 */
class DriveUploadProvider(private val client: OdinClient) {

    companion object {
        private const val TAG = "DriveUploadProvider"
        private const val UPLOAD_ENDPOINT = "/drive/files/upload"
        private const val UPDATE_ENDPOINT = "/drive/files/update"
    }

    /**
     * Performs a raw upload to the drive.
     *
     * @param data The multipart form data to upload
     * @param fileSystemType Optional file system type to use
     * @param onVersionConflict Optional callback for version conflict handling
     * @return The upload result, or null if version conflict was handled by callback
     * @throws OdinClientException on errors
     */
    suspend fun pureUpload(
            data: MultiPartFormDataContent,
            fileSystemType: FileSystemType? = null,
            onVersionConflict: (suspend () -> UploadResult?)? = null
    ): UploadResult? {
        val httpClient =
                client.createHttpClient(
                        CreateHttpClientOptions(
                                overrideEncryption = true,
                                fileSystemType = fileSystemType ?: FileSystemType.Standard
                        )
                )

        try {
            val response: HttpResponse = httpClient.post(UPLOAD_ENDPOINT) { setBody(data) }

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
            httpClient.close()
        }
    }

    /**
     * Performs a raw update to an existing file on the drive.
     *
     * @param data The multipart form data for the update
     * @param fileSystemType Optional file system type to use
     * @param onVersionConflict Optional callback for version conflict handling
     * @return The update result, or null if version conflict was handled by callback
     * @throws OdinClientException on errors
     */
    suspend fun pureUpdate(
            data: MultiPartFormDataContent,
            fileSystemType: FileSystemType? = null,
            onVersionConflict: (suspend () -> UpdateResult?)? = null
    ): UpdateResult? {
        val httpClient =
                client.createHttpClient(
                        CreateHttpClientOptions(
                                overrideEncryption = true,
                                fileSystemType = fileSystemType ?: FileSystemType.Standard
                        )
                )

        try {
            val response: HttpResponse = httpClient.patch(UPDATE_ENDPOINT) { setBody(data) }

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
            httpClient.close()
        }
    }

    /** Handles the upload response, including error handling and version conflict. */
    private suspend fun handleUploadResponse(
            response: HttpResponse,
            onVersionConflict: (suspend () -> UploadResult?)? = null
    ): UploadResult? {
        return when {
            response.status.isSuccess() -> {
                val body = response.bodyAsText()
                OdinSystemSerializer.json.decodeFromString<UploadResult>(body)
            }
            else -> {
                handleErrorResponse(response, onVersionConflict) { callback -> callback() }
            }
        }
    }

    /** Handles the update response, including error handling and version conflict. */
    private suspend fun handleUpdateResponse(
            response: HttpResponse,
            onVersionConflict: (suspend () -> UpdateResult?)? = null
    ): UpdateResult? {
        return when {
            response.status.isSuccess() -> {
                val body = response.bodyAsText()
                OdinSystemSerializer.json.decodeFromString<UpdateResult>(body)
            }
            else -> {
                handleErrorResponse(response, onVersionConflict) { callback -> callback() }
            }
        }
    }

    /**
     * Handles error responses from the server. Checks for version tag mismatch and calls the
     * conflict handler if provided.
     *
     * @param response The HTTP response with an error status
     * @param onVersionConflict Optional callback for version conflict handling
     * @param invokeCallback Function to invoke the callback and return the correct type
     * @return The result from the version conflict callback, or throws an exception
     */
    private suspend fun <T> handleErrorResponse(
            response: HttpResponse,
            onVersionConflict: (suspend () -> T?)? = null,
            invokeCallback: suspend ((suspend () -> T?)) -> T?
    ): T? {
        val body = response.bodyAsText()

        // Parse error response - errorCode is automatically deserialized to OdinClientErrorCode
        val errorResponse =
                try {
                    OdinSystemSerializer.json.decodeFromString<OdinErrorResponse>(body)
                } catch (e: Exception) {
                    null
                }

        // Check for version tag mismatch
        if (errorResponse?.errorCode == OdinClientErrorCode.VersionTagMismatch) {
            if (onVersionConflict == null) {
                KLogger.w(TAG) {
                    "VersionTagMismatch, to avoid this, add an onVersionConflict handler"
                }
            } else {
                return invokeCallback(onVersionConflict)
            }
        }

        // Log the error based on status code
        when (response.status.value) {
            400 -> KLogger.e(TAG) { "Bad Request: $body" }
            else -> KLogger.e(TAG) { "Request failed with status ${response.status.value}: $body" }
        }

        // Use the parsed error code, or fall back to HTTP status-based codes
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
