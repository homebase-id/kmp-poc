package id.homebase.homebasekmppoc.lib.drives.query

import id.homebase.homebasekmppoc.lib.core.SecureByteArray
import id.homebase.homebasekmppoc.lib.drives.*
import id.homebase.homebasekmppoc.lib.http.OdinClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/**
 * Drive query provider for querying files from a drive
 */
class DriveQueryProvider(private val odinClient: OdinClient) {

    /**
     * Query a batch of files from a drive
     *
     * @param request QueryBatchRequest containing query parameters and result options
     * @param options Optional settings for decryption and HTTP configuration
     * @return QueryBatchResponse containing the results
     */
    suspend fun queryBatch(
        request: QueryBatchRequest,
        options: QueryBatchOptions? = null
    ): QueryBatchResponse {
        // Set includeMetadataHeader if decrypt is enabled
        val finalRequest = if (options?.decrypt == true) {
            request.copy(
                resultOptionsRequest = request.resultOptionsRequest.copy(
                    includeMetadataHeader = true
                )
            )
        } else {
            request
        }

        val queryParams = finalRequest.toQueryString()
        val getUrl = "/drive/query/batch?$queryParams"

        // Max URL is 1800 so we keep room for encryption overhead
        // Check if baseURL + getUrl length > 1800
        val baseUrl = odinClient.getEndpointUrl()
        val totalLength = baseUrl.length + getUrl.length

        return if (totalLength > 1800) {
            // Use POST for long URLs
            queryBatchPost(finalRequest, options)
        } else {
            // Use GET for short URLs
            queryBatchGet(getUrl, options)
        }
    }

    /**
     * Query batch using GET request
     */
    private suspend fun queryBatchGet(
        url: String,
        options: QueryBatchOptions? = null
    ): QueryBatchResponse {
        val client = odinClient.createHttpClient()

        val response = client.get(url) {
            // Apply custom HTTP configuration if provided
            options?.httpConfig?.invoke(this)
        }.body<QueryBatchResponse>()

        return handleResponse(response, options)
    }

    /**
     * Query batch using POST request
     */
    private suspend fun queryBatchPost(
        request: QueryBatchRequest,
        options: QueryBatchOptions? = null
    ): QueryBatchResponse {
        val client = odinClient.createHttpClient()

        val url = "/drive/query/batch"

        val response: QueryBatchResponse = client.post(url) {
            setBody(request)
            // Apply custom HTTP configuration if provided
            options?.httpConfig?.invoke(this)
        }.body<QueryBatchResponse>()

        return handleResponse(response, options)
    }

    /**
     * Handle the HTTP response and decrypt if needed
     */
    private suspend fun handleResponse(
        response: QueryBatchResponse,
        options: QueryBatchOptions? = null
    ): QueryBatchResponse {
        val shouldDecrypt = options?.decrypt ?: false
        if(shouldDecrypt)  {
            val decryptedResults = response.searchResults.map { dsr ->
                if(dsr.fileMetadata.appData.content == null) {
                    return@map dsr
                }
                // Decrypt key header if file is encrypted
                val ss = odinClient.getSharedSecret()

                val keyHeader = if (dsr.fileMetadata.isEncrypted && ss != null) {
                    dsr.sharedSecretEncryptedKeyHeader.decryptAesToKeyHeader(SecureByteArray(ss))
                } else {
                    null
                }

                // Decrypt JSON content
                val decryptedContent = keyHeader?.decrypt(dsr.fileMetadata.appData.content)?.decodeToString()

                // Return updated search result with decrypted content
                dsr.copy(
                    fileMetadata = dsr.fileMetadata.copy(
                        appData = dsr.fileMetadata.appData.copy(
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