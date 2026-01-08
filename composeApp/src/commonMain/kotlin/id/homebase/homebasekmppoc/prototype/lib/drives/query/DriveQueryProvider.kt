package id.homebase.homebasekmppoc.prototype.lib.drives.query

import id.homebase.homebasekmppoc.prototype.lib.ApiServiceExample.CredentialsManager
import id.homebase.homebasekmppoc.prototype.lib.core.SecureByteArray
import id.homebase.homebasekmppoc.prototype.lib.crypto.CryptoHelper
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchResponse
import id.homebase.homebasekmppoc.prototype.lib.drives.files.ValidationUtil
import id.homebase.homebasekmppoc.prototype.lib.http.OdinClient
import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import kotlin.uuid.Uuid

/** Drive query provider for querying files from a drive */
class DriveQueryProvider(
    private val odinClient: OdinClient,
    private val httpClient: HttpClient,
    private val credentialsManager: CredentialsManager
) {

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
        ValidationUtil.requireValidUuid(driveId, "driveId")

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


        val (domain, cat, secret) = checkNotNull(credentialsManager.getActiveCredentials());

        val url = "https://${domain}/api/v2/drives/${driveId}/files/query-batch"
        val client = httpClient;

        val response = client
            .post(url) {
                bearerAuth(cat)
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(encryptBody(finalRequest, secret))
            }

        val bodyCipher = response.body<String>()
        val bodyJson = decryptContentAsString(bodyCipher, secret)

        val qbResponse = OdinSystemSerializer.deserialize<QueryBatchResponse>(bodyJson)
        return handleResponse(qbResponse, options)
    }

    private suspend fun encryptBody(body: QueryBatchRequest, secret: SecureByteArray): TextContent {
        var bodyText = OdinSystemSerializer.serialize(body)
        val payload = CryptoHelper.encryptData(bodyText, secret.unsafeBytes)
        return TextContent(
            OdinSystemSerializer.json.encodeToString(payload),
            ContentType.Application.Json
        )
    }


    private suspend fun decryptContentAsString(
        cipherJson: String,
        secret: SecureByteArray
    ): String {
        return CryptoHelper.decryptContentAsString(cipherJson, secret.unsafeBytes)
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
