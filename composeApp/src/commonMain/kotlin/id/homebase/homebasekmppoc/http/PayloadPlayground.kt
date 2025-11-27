@file:OptIn(ExperimentalUuidApi::class)

package id.homebase.homebasekmppoc.http

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.authentication.AuthState
import id.homebase.homebasekmppoc.drives.DriveDefinition
import id.homebase.homebasekmppoc.drives.GetDrivesByTypeRequest
import id.homebase.homebasekmppoc.drives.SharedSecretEncryptedFileHeader
import id.homebase.homebasekmppoc.drives.SystemDriveConstants
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.contentLength
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class PayloadPlayground(private val authenticated: AuthState.Authenticated) {

    //

    suspend fun getEverything() {
        getDrivesByType(SystemDriveConstants.publicPostChannelDrive.type)

        val fileId = "1355aa19-2030-8200-00ef-563eed96bebf"
        val alias = "e8475dc46cb4b6651c2d0dbd0f3aad5f"
        val type = "8f448716e34cedf9014145e043ca6612"
        val fileSystemType = "128"

        val fileHeader = getFileHeader(fileId, alias, type, fileSystemType)
        val payloadBytes = getPayloadBytes(fileHeader, fileSystemType)
    }

    //

    suspend fun getImage(): ByteArray {

        val fileId = "1355aa19-2030-8200-00ef-563eed96bebf"
        val alias = "e8475dc46cb4b6651c2d0dbd0f3aad5f"
        val type = "8f448716e34cedf9014145e043ca6612"
        val fileSystemType = "128"

        Logger.d("PayloadPlayground") { "getImage: Fetching file header..." }
        val fileHeader = getFileHeader(fileId, alias, type, fileSystemType)

        Logger.d("PayloadPlayground") { "getImage: Fetching payload bytes..." }
        val payloadBytes = getPayloadBytes(fileHeader, fileSystemType)

        Logger.d("PayloadPlayground") { "getImage: Verifying image format..." }
        id.homebase.homebasekmppoc.image.ImageFormatDetector.logImageInfo(payloadBytes, "PayloadPlayground")

        return payloadBytes
    }

    //

    suspend fun getDrivesByType(type: Uuid): PagedResult<DriveDefinition> {

        val params = GetDrivesByTypeRequest(
            type.toString(),
            1,
            Int.MAX_VALUE)

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

    suspend fun getFileHeader(fileId: String, alias: String, type: String, fileSystemType: String): SharedSecretEncryptedFileHeader
    {
        val uri = "/api/owner/v1/drive/files/header?alias=$alias&type=$type&fileId=$fileId&xfst=$fileSystemType"

        val client = OdinHttpClient(authenticated)
        val result = client.get<SharedSecretEncryptedFileHeader>(uri)

        return result
    }

    //

    suspend fun getPayloadBytes(header: SharedSecretEncryptedFileHeader, fileSystemType: String): ByteArray
    {
        val fileId = header.fileId
        val alias = header.targetDrive.alias
        val type = header.targetDrive.type

        // Get the payload descriptor (assuming first payload for now)
        val payload = header.fileMetadata.payloads?.get(0)
            ?: throw Exception("No payload found in file metadata")

        val payloadKey = payload.key
        val payloadIvBase64 = payload.iv ?: throw Exception("No IV found in payload descriptor")

        Logger.d("PayloadPlayground") { "Payload key: $payloadKey" }
        Logger.d("PayloadPlayground") { "Payload IV (base64): $payloadIvBase64" }

        val uri = "https://${authenticated.identity}/api/owner/v1/drive/files/payload?alias=$alias&type=$type&fileId=$fileId&key=$payloadKey&xfst=$fileSystemType"

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
            id.homebase.homebasekmppoc.core.SecureByteArray(sharedSecretBytes)
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