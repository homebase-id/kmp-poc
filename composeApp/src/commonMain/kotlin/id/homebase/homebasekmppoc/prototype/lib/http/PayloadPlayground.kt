@file:OptIn(ExperimentalUuidApi::class)

package id.homebase.homebasekmppoc.prototype.lib.http

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.prototype.lib.authentication.AuthState
import id.homebase.homebasekmppoc.prototype.lib.core.SecureByteArray
import id.homebase.homebasekmppoc.prototype.lib.drives.DriveDefinition
import id.homebase.homebasekmppoc.prototype.lib.drives.FileState
import id.homebase.homebasekmppoc.prototype.lib.drives.GetDrivesByTypeRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.GetQueryBatchRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.PayloadDescriptor
import id.homebase.homebasekmppoc.prototype.lib.drives.SharedSecretEncryptedFileHeader
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.contentLength
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

    suspend fun getHeadersOnDrive(driveAlias: Uuid, driveType: Uuid, fileState: FileState): List<SharedSecretEncryptedFileHeader> {
        val qb = GetQueryBatchRequest(
            alias = driveAlias,
            type = driveType,
            fileState = listOf(fileState),
            maxRecords = 1000,
            includeMetadataHeader = true
        )

        val client = OdinHttpClient(authenticated)
        val response = client.queryBatch(qb)
        return response.searchResults
    }

    //

    suspend fun getImagesOnDrive(driveAlias: Uuid, driveType: Uuid): List<PayloadWrapper> {
        val headers = getHeadersOnDrive(driveAlias, driveType, FileState.Active)

        return buildList {
            headers.forEach { header ->
                header.fileMetadata.payloads?.forEach { payload ->
                    if (payload.contentType?.contains("image") == true) {
                        add(PayloadWrapper(header, payload))
                    }
                }
            }
        }
    }

    //

    suspend fun getImage(payload: PayloadWrapper): ByteArray {
        return payload.getPayloadBytes(authenticated)
    }

    //

    suspend fun getVideosOnDrive(driveAlias: Uuid, driveType: Uuid): List<PayloadWrapper> {
        val headers = getHeadersOnDrive(driveAlias, driveType, FileState.Active)

        return buildList {
            headers.forEach { header ->
                header.fileMetadata.payloads?.forEach { payload ->
                    if (payload.contentType?.contains("video") == true) {
                        add(PayloadWrapper(header, payload))
                    }
                }
            }
        }
    }

    //

    suspend fun getVideo(payload: PayloadWrapper): ByteArray {
        return payload.getPayloadBytes(authenticated)
    }


    //

    suspend fun getVideoMetaData(pw: PayloadWrapper) {
        val header = pw.header
        val payload = pw.payload

        Logger.d("VideoPlayerTestPage") { "Getting video metadata for file: ${header.fileId}" }

        val metadata = payload.descriptorContent

        Logger.d("VideoPlayerTestPage") { "Video metadata: $metadata" }
    }

    suspend fun getFileHeader(fileId: String, alias: String, type: String, fileSystemType: String): SharedSecretEncryptedFileHeader
    {
        val uri = "/api/owner/v1/drive/files/header?alias=$alias&type=$type&fileId=$fileId&xfst=$fileSystemType"

        val client = OdinHttpClient(authenticated)
        val result = client.get<SharedSecretEncryptedFileHeader>(uri)

        return result
    }

    //

}

//

class PayloadWrapper(
    val header: SharedSecretEncryptedFileHeader,
    val payload: PayloadDescriptor) {

    suspend fun getPayloadBytes(authenticated: AuthState.Authenticated): ByteArray {
        val fileId = header.fileId
        val alias = header.targetDrive.alias
        val type = header.targetDrive.type

        val payloadKey = payload.key
        val payloadIvBase64 = payload.iv ?: throw Exception("No IV found in payload descriptor")

        Logger.d("PayloadPlayground") { "Payload key: $payloadKey" }
        Logger.d("PayloadPlayground") { "Payload IV (base64): $payloadIvBase64" }

        // call backend DriveStorageControllerBase.GetPayloadStream
        val uri = "https://${authenticated.identity}/api/owner/v1/drive/files/payload?alias=$alias&type=$type&fileId=$fileId&key=$payloadKey&xfst=128"

        val odinClient = OdinHttpClient(authenticated)
        val encryptedUri = odinClient.buildUriWithEncryptedQueryString(uri)

        Logger.d("PayloadPlayground") { "Making GET request to: $encryptedUri" }

        val client = createHttpClient()
        val response = client.get(encryptedUri) {
            headers {
                append("Cookie", "DY0810=${authenticated.clientAuthToken}")
            }
        }

        Logger.d("PayloadPlayground") { "Response length: ${response.contentLength()}" }

        // Decrypt the KeyHeader using the shared secret
        val sharedSecretBytes = kotlin.io.encoding.Base64.decode(authenticated.sharedSecret)
        val keyHeader = header.sharedSecretEncryptedKeyHeader.decryptAesToKeyHeader(
            SecureByteArray(sharedSecretBytes)
        )

        // Get encrypted payload bytes
        val encryptedBytes = response.body<ByteArray>()
        Logger.d("PayloadPlayground") { "Encrypted payload length: ${encryptedBytes.size}" }

        // Decrypt the payload using the KeyHeader's AES key BUT the payload's IV
        val payloadIv = kotlin.io.encoding.Base64.decode(payloadIvBase64)
        Logger.d("PayloadPlayground") { "Using payload IV: ${payloadIv.size} bytes" }

        val decryptedBytes = keyHeader.decryptWithIv(encryptedBytes, payloadIv)
        Logger.d("PayloadPlayground") { "Decrypted payload length: ${decryptedBytes.size}" }

        return decryptedBytes
    }

}
