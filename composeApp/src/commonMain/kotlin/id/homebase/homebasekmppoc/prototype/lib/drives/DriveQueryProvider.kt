package id.homebase.homebasekmppoc.prototype.lib.drives

import id.homebase.homebasekmppoc.lib.crypto.CryptoHelper
import id.homebase.homebasekmppoc.lib.drives.QueryBatchResponse
import id.homebase.homebasekmppoc.prototype.lib.http.createHttpClient
import id.homebase.homebasekmppoc.prototype.encodeUrl
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * Drive query provider for querying files from a drive
 * Ported from C# YouAuthClientReferenceImplementation.DriveQueryProvider
 */
class DriveQueryProvider(private val httpClient: HttpClient) {

    /**
     * Query a batch of files from a drive
     *
     * @param domain The domain of the identity to query
     * @param clientAuthToken The client authentication token (BX0900 cookie)
     * @param sharedSecret The shared secret for encrypting query parameters and decrypting response
     * @param driveAlias The drive alias to query
     * @param driveType The drive type to query
     * @return QueryBatchResponse containing the results
     */
    suspend fun queryBatch(
        domain: String,
        clientAuthToken: String,
        sharedSecret: String,
        driveAlias: String,
        driveType: String
    ): QueryBatchResponse {
        // Build query parameters
        val queryParams = buildMap {
            put("maxRecords", "1000")
            put("includeMetadataHeader", "true")
            put("alias", driveAlias)
            put("type", driveType)
            put("fileState", "1") // Active files
        }

        // Build query string
        val queryString = queryParams.entries.joinToString("&") { (key, value) ->
            "$key=${encodeUrl(value)}"
        }

        // Build base URL
        val url = "https://$domain/api/apps/v1/drive/query/batch?$queryString"

        return queryBatch(url, clientAuthToken, sharedSecret)
    }

    //

    suspend fun queryBatch(
        uri: String,
        clientAuthToken: String,
        sharedSecret: String): QueryBatchResponse {

        // Encrypt query string
        val url = CryptoHelper.uriWithEncryptedQueryString(uri, sharedSecret)

        // Make HTTP request
        val response: HttpResponse = httpClient.get(url) {
            header("X-ODIN-FILE-SYSTEM-TYPE", "Standard")
            header("Cookie", "BX0900=$clientAuthToken")
        }

        // Read response content
        val content = response.bodyAsText()

        // Handle response
        return when (response.status) {
            HttpStatusCode.OK -> {
                try {
                    CryptoHelper.decryptContent<QueryBatchResponse>(content, sharedSecret)
                } catch (e: Exception) {
                    throw Exception("Oh no1 ${response.status.value}: ${e.message}", e)
                }
            }
            else -> {
                // Try to decrypt error response
                val errorJson = try {
                    CryptoHelper.decryptContentAsString(content, sharedSecret)
                } catch (e: Exception) {
                    throw Exception("Oh no2 ${response.status.value}: $content", e)
                }
                throw Exception("Oh no3 ${response.status.value}: $errorJson")
            }
        }
    }

    companion object {
        /**
         * Create a default DriveQueryProvider with a configured HTTP client
         */
        fun create(): DriveQueryProvider {
            val client = createHttpClient()
            return DriveQueryProvider(client)
        }
    }
}

