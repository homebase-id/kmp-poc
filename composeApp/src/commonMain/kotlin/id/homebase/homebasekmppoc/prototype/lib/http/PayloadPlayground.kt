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

    suspend fun getImagesOnDrive(
        appOrOwner: AppOrOwner,
        driveAlias: Uuid,
        driveType: Uuid): List<PayloadWrapper> {
        val headers = getHeadersOnDrive(appOrOwner, driveAlias, driveType, FileState.Active)

        return buildList {
            headers.forEach { header ->
                header.fileMetadata.payloads?.forEach { payload ->
                    if (payload.contentType?.contains("image") == true) {
                        add(PayloadWrapper(authenticated, header, payload))
                    }
                }
            }
        }
    }

    //

    suspend fun getImage(appOrOwner: AppOrOwner, payload: PayloadWrapper): ByteArray {
        return payload.getPayloadBytes(appOrOwner)
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

    suspend fun getVideo(appOrOwner: AppOrOwner, payload: PayloadWrapper): ByteArray {
        return payload.getPayloadBytes(appOrOwner)
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

class PayloadWrapper(
    val authenticated: AuthState.Authenticated,
    val header: SharedSecretEncryptedFileHeader,
    val payload: PayloadDescriptor) {

    //

    val isEncrypted: Boolean get() = header.fileMetadata.isEncrypted

    //

    val compositeKey: String get() = header.fileId.toString() + "-" + payload.key

    //

    fun getPayloadUri(appOrOwner: AppOrOwner): String {
        val fileId = header.fileId
        val alias = header.targetDrive.alias
        val type = header.targetDrive.type
        val payloadKey = payload.key

        // calls backend DriveStorageControllerBase.GetPayloadStream
        val uri = "https://${authenticated.identity}/api/$appOrOwner/v1/drive/files/payload?alias=$alias&type=$type&fileId=$fileId&key=$payloadKey&xfst=128"

        return uri
    }

    //

    suspend fun getEncryptedPayloadUri(appOrOwner: AppOrOwner): String {
        val plain = getPayloadUri(appOrOwner)
        val cipher = CryptoHelper.uriWithEncryptedQueryString(plain, authenticated.sharedSecret)
        return cipher
    }

    //

    suspend fun getPayloadBytes(appOrOwner: AppOrOwner): ByteArray {
        Logger.d("PayloadPlayground") { "Payload key: ${payload.key}" }

        val encryptedUri = getEncryptedPayloadUri(appOrOwner)

        Logger.d("PayloadPlayground") { "Making GET request to: $encryptedUri" }

        val client = createHttpClient()
        val response = client.get(encryptedUri) {
            headers {
                append("Cookie", "$ownerCookieName=${authenticated.clientAuthToken}")
                // append(ownerCookieName, authenticated.clientAuthToken)
            }
        }

        Logger.d("PayloadPlayground") { "Response length: ${response.contentLength()}" }

        val bytes = response.body<ByteArray>()
        Logger.d("PayloadPlayground") { "Payload length: ${bytes.size}" }

        if (!isEncrypted) {
            return bytes
        } else {
            val payloadIvBase64 = payload.iv ?: throw Exception("No IV found in payload descriptor")
            val keyHeader = decryptKeyHeader() ?: throw Exception("Failed to decrypt KeyHeader")

            // Decrypt the payload using the KeyHeader's AES key BUT the payload's IV
            val payloadIv = Base64.decode(payloadIvBase64)
            Logger.d("PayloadPlayground") { "Using payload IV: ${payloadIv.size} bytes" }

            val decryptedBytes = keyHeader.decryptWithIv(bytes, payloadIv)
            Logger.d("PayloadPlayground") { "Decrypted payload length: ${decryptedBytes.size}" }

            return decryptedBytes
        }
    }

    //

    suspend fun decryptKeyHeader(): KeyHeader? {
        if (!isEncrypted) {
            return null
        }

        val sharedSecretBytes = Base64.decode(authenticated.sharedSecret)
        val result =  header.sharedSecretEncryptedKeyHeader.decryptAesToKeyHeader(
            SecureByteArray(sharedSecretBytes)
        )
        return result
    }

    //

    fun getVideoMetaData(appOrOwner: AppOrOwner): VideoMetaData {
        val playlistContent = payload.descriptorContent
            ?: throw Exception("No descriptor content found in payload")

        val videoMetaData = OdinSystemSerializer.deserialize<VideoMetaData>(playlistContent)
        return videoMetaData

        // if (videoMetaData.hlsPlaylist == null) {
        //     throw Exception("No HLS playlist found in video metadata")
        // }
        //
        // val hls = createHlsPlaylist(appOrOwner, videoMetaData)
        // Logger.d("getVideoMetaData") { "HLS Playlist:\n$hls" }
        // return hls
    }

    //

}

