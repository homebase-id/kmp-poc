package id.homebase.homebasekmppoc.http

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.authentication.AuthState
import id.homebase.homebasekmppoc.core.GuidId
import id.homebase.homebasekmppoc.crypto.CryptoHelper
import id.homebase.homebasekmppoc.drives.DriveDefinition
import id.homebase.homebasekmppoc.drives.GetDrivesByTypeRequest
import id.homebase.homebasekmppoc.drives.SharedSecretEncryptedFileHeader
import id.homebase.homebasekmppoc.drives.SystemDriveConstants
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.contentLength
import io.ktor.utils.io.core.toByteArray

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

        val fileHeader = getFileHeader(fileId, alias, type, fileSystemType)
        val payloadBytes = getPayloadBytes(fileHeader, fileSystemType)

        return payloadBytes
    }

    //

    suspend fun getDrivesByType(type: GuidId): PagedResult<DriveDefinition> {

        val params = GetDrivesByTypeRequest(
            type.toString(),
            1,
            Int.MAX_VALUE)

        val uri = "/api/owner/v1/drive/mgmt/type?${params.toQueryString()}"
        val client = OdinHttpClient(authenticated)
        val drives = client.get<PagedResult<DriveDefinition>>(uri)

        drives.results.forEach {
            Logger.d("PayloadPlayground") { "Drive: Alias=${it.targetDriveInfo.alias} Type=${it.targetDriveInfo.type} Name=${it.name}" }
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
        // SEB:TODO fix OdinHttpClient so it can handle raw bytes as well as JSON

        val fileId = header.fileId
        val alias = header.targetDrive.alias
        val type = header.targetDrive.type
        val key = header.fileMetadata.payloads?.get(0)?.key ?: throw Exception("No payload key found in file metadata")
        val uri = "https://${authenticated.identity}/api/owner/v1/drive/files/payload?alias=$alias&type=$type&fileId=$fileId&key=pst_mdi0&xfst=$fileSystemType"

        val odinClient = OdinHttpClient(authenticated)

        val encryptedUri = odinClient.buildUriWithEncryptedQueryString(uri)

        Logger.d("OdinHttpClient") { "Making GET request to: $encryptedUri" }

        val client = createHttpClient()

        val response = client.get(encryptedUri) {
            headers {
                append("Cookie", "DY0810=${authenticated.clientAuthToken}")
            }
        }

        // Decrypt the AES key using the shared secret
        val sharedSecretBytes = kotlin.io.encoding.Base64.decode(authenticated.sharedSecret)
        val keyHeader = header.sharedSecretEncryptedKeyHeader.decryptAesToKeyHeader(
            id.homebase.homebasekmppoc.core.SecureByteArray(sharedSecretBytes)
        )

        Logger.d("OdinHttpClient") { "response length: ${response.contentLength()}" }

        // Get response as raw bytes
        val encryptedBytes = response.body<ByteArray>()
        Logger.d("OdinHttpClient") { "Encrypted payload length: ${encryptedBytes.size}" }

        // Decrypt the payload using the key header
        val decryptedBytes = keyHeader.decrypt(encryptedBytes)
        Logger.d("OdinHttpClient") { "Decrypted payload length: ${decryptedBytes.size}" }

        return decryptedBytes
    }


}