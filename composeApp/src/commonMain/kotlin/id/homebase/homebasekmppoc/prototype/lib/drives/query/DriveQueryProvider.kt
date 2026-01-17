package id.homebase.homebasekmppoc.prototype.lib.drives.query

import id.homebase.homebasekmppoc.prototype.lib.base.CredentialsManager
import id.homebase.homebasekmppoc.prototype.lib.base.OdinApiProviderBase
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchResponse
import id.homebase.homebasekmppoc.prototype.lib.drives.files.ValidationUtil
import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import kotlin.uuid.Uuid

/** Drive query provider for querying files from a drive */
class DriveQueryProvider(
    httpClient: HttpClient,
    credentialsManager: CredentialsManager
) : OdinApiProviderBase(httpClient, credentialsManager) {

    suspend fun queryBatch(
        driveId: Uuid,
        request: QueryBatchRequest,
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

        throwForFailure(apiResponse);

        return deserialize<QueryBatchResponse>(apiResponse.body)
    }
}
