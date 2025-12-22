package id.homebase.homebasekmppoc.prototype.lib.drives.query

import id.homebase.homebasekmppoc.prototype.lib.core.SecureByteArray
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchResponse
import id.homebase.homebasekmppoc.prototype.lib.http.OdinClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.uuid.Uuid

/** Drive query provider for querying files from a drive */
class DriveQueryProvider(private val odinClient: OdinClient) {

    /**
     * Query a batch of files from a drive
     *
     * @param request QueryBatchRequest containing query parameters and result options
     * @param options Optional settings for decryption and HTTP configuration
     * @return QueryBatchResponse containing the results
     */
    suspend fun queryBatch(
        driveId: Uuid,
        request: QueryBatchRequest,
        options: QueryBatchOptions? = null
    ): QueryBatchResponse {

        requireNotNull(driveId) { "driveId is required" }
        require(driveId != Uuid.NIL) {
            "driveId must not be all zeros"
        }

        // Set includeMetadataHeader if decrypt is enabled
        val finalRequest =
            if (options?.decrypt == true) {
                request.copy(
                    resultOptionsRequest =
                        request.resultOptionsRequest.copy(includeMetadataHeader = true)
                )
            } else {
                request
            }


        val url = "drives/${driveId}/files/query-batch"
        val client = odinClient.createHttpClient()

        val response: QueryBatchResponse =
            client
                .post(url) {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(finalRequest)
                    // Apply custom HTTP configuration if provided
                    options?.httpConfig?.invoke(this)
                }
                .body<QueryBatchResponse>()

        return handleResponse(response, options)
    }

    /** Handle the HTTP response and decrypt if needed */
    private suspend fun handleResponse(
        response: QueryBatchResponse,
        options: QueryBatchOptions? = null
    ): QueryBatchResponse {
        val shouldDecrypt = options?.decrypt ?: false
        if (shouldDecrypt) {
            val decryptedResults =
                response.searchResults.map { dsr ->
                    if (dsr.fileMetadata.appData.content == null) {
                        return@map dsr
                    }
                    // Decrypt key header if file is encrypted
                    val ss = odinClient.getSharedSecret()

                    val keyHeader =
                        if (dsr.fileMetadata.isEncrypted && ss != null) {
                            dsr.sharedSecretEncryptedKeyHeader.decryptAesToKeyHeader(
                                SecureByteArray(ss)
                            )
                        } else {
                            null
                        }

                    // Decrypt JSON content
                    val decryptedContent =
                        keyHeader
                            ?.decrypt(dsr.fileMetadata.appData.content)
                            ?.decodeToString()

                    // Return updated search result with decrypted content
                    dsr.copy(
                        fileMetadata =
                            dsr.fileMetadata.copy(
                                appData =
                                    dsr.fileMetadata.appData.copy(
                                        content = decryptedContent
                                    )
                            )
                    )
                }

            return response.copy(searchResults = decryptedResults)
        }
        return response
    }
}
