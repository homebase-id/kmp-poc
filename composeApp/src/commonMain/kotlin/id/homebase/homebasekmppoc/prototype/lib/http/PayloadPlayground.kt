@file:OptIn(ExperimentalUuidApi::class)

package id.homebase.homebasekmppoc.prototype.lib.http

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.lib.serialization.OdinSystemSerializer
import id.homebase.homebasekmppoc.prototype.lib.authentication.AuthState
import id.homebase.homebasekmppoc.prototype.lib.core.SecureByteArray
import id.homebase.homebasekmppoc.prototype.lib.crypto.CryptoHelper
import id.homebase.homebasekmppoc.prototype.lib.crypto.KeyHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.DriveDefinition
import id.homebase.homebasekmppoc.prototype.lib.drives.FileState
import id.homebase.homebasekmppoc.prototype.lib.drives.GetDrivesByTypeRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.GetQueryBatchRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.PayloadDescriptor
import id.homebase.homebasekmppoc.prototype.lib.drives.SharedSecretEncryptedFileHeader
import id.homebase.homebasekmppoc.prototype.lib.video.VideoMetaData
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.contentLength
import kotlin.io.encoding.Base64
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object PublicPostsChannelDrive {
    val alias: Uuid = Uuid.parse("e8475dc46cb4b6651c2d0dbd0f3aad5f")
    val type: Uuid = Uuid.parse("8f448716e34cedf9014145e043ca6612")
}

//

class PayloadPlayground(private val authenticated: AuthState.Authenticated) {

    //

    suspend fun getDrivesByType(type: Uuid): PagedResult<DriveDefinition> {

        val params = GetDrivesByTypeRequest(
            type.toString(),
            1,
            Int.MAX_VALUE
        )

        val uri = "/api/owner/v1/drive/mgmt/type?${params.toQueryString()}"
        val client = OdinHttpClient(authenticated)
        val drives = client.get<PagedResult<DriveDefinition>>(uri)

        Logger.d("PayloadPlayground") { "Drives:" }
        drives.results.forEach {
            Logger.d("PayloadPlayground") { "  drive: Alias=${it.targetDriveInfo.alias} Type=${it.targetDriveInfo.type} Name=${it.name}" }
        }

        return drives
    }

    //

    suspend fun getHeadersOnDrive(
        appOrOwner: AppOrOwner,
        driveAlias: Uuid,
        driveType: Uuid,
        fileState: FileState): List<SharedSecretEncryptedFileHeader> {
        val qb = GetQueryBatchRequest(
            alias = driveAlias,
            type = driveType,
            fileState = listOf(fileState),
            maxRecords = 1000,
            includeMetadataHeader = true
        )

        val client = OdinHttpClient(authenticated)
        val response = client.queryBatch(appOrOwner, qb)
        return response.searchResults
    }

    //

    suspend fun getVideosOnDrive(appOrOwner: AppOrOwner, driveAlias: Uuid, driveType: Uuid): List<PayloadWrapper> {
        val headers = getHeadersOnDrive(appOrOwner, driveAlias, driveType, FileState.Active)

        return buildList {
            headers.forEach { header ->
                header.fileMetadata.payloads?.forEach { payload ->
                    if (payload.contentType?.contains("video") == true) {
                        add(PayloadWrapper(authenticated, header, payload))
                    }
                }
            }
        }
    }

    //

    suspend fun getFileHeader(
        appOrOwner: AppOrOwner,
        fileId: String,
        alias: String,
        type: String,
        fileSystemType: String): SharedSecretEncryptedFileHeader
    {
        val uri = "/api/$appOrOwner/v1/drive/files/header?alias=$alias&type=$type&fileId=$fileId&xfst=$fileSystemType"

        val client = OdinHttpClient(authenticated)
        val result = client.get<SharedSecretEncryptedFileHeader>(uri)

        return result
    }

    //

}

//


