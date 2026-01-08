package id.homebase.homebasekmppoc.prototype.lib.drives.query

import id.homebase.homebasekmppoc.prototype.lib.ApiServiceExample.CredentialsManager
import id.homebase.homebasekmppoc.prototype.lib.client.OdinApiProviderBase
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchResponse
import id.homebase.homebasekmppoc.prototype.lib.drives.files.ValidationUtil
import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import io.ktor.client.HttpClient

import kotlin.uuid.Uuid

/** Drive query provider for querying files from a drive */

class DriveQueryProvider(
    httpClient: HttpClient,
    credentialsManager: CredentialsManager
) : OdinApiProviderBase(httpClient, credentialsManager) {

    suspend fun queryBatch(
        driveId: Uuid,
        request: QueryBatchRequest,
        options: QueryBatchOptions? = null
    ): QueryBatchResponse {

        ValidationUtil.requireValidUuid(driveId, "driveId")

        val creds = requireCreds()
        val url = apiUrl(
            creds.domain,
            "/drives/$driveId/files/query-batch"
        )

        val jsonRequest = OdinSystemSerializer.serialize(request)

        val apiResponse = encryptedPostJson(
            url = url,
            token = creds.accessToken,
            jsonBody = jsonRequest,
            secret = creds.secret
        )

        // Optional: inspect response
        if (apiResponse.status != 200) {
            // map error / throw domain exception if needed
        }

        val response =
            deserialize<QueryBatchResponse>(apiResponse.body)

        return handleResponse(response, options)
    }

    private suspend fun handleResponse(
        response: QueryBatchResponse,
        options: QueryBatchOptions?
    ): QueryBatchResponse {
        // unchanged from your original
        return response
    }
}

