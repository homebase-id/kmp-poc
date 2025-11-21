package id.homebase.homebasekmppoc.http

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.authentication.AuthState
import id.homebase.homebasekmppoc.core.GuidId
import id.homebase.homebasekmppoc.drives.DriveDefinition
import id.homebase.homebasekmppoc.drives.GetDrivesByTypeRequest
import id.homebase.homebasekmppoc.drives.SharedSecretEncryptedFileHeader
import id.homebase.homebasekmppoc.drives.SystemDriveConstants
import id.homebase.homebasekmppoc.toBase64
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.contentLength
import kotlin.io.encoding.Base64

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

    suspend fun getDrivesByType(type: GuidId): PagedResult<DriveDefinition> {

        val params = GetDrivesByTypeRequest(
            type.toString(),
            1,
            Int.MAX_VALUE)

        val url = "/api/owner/v1/drive/mgmt/type?${params.toQueryString()}"
        val client = OdinHttpClient(authenticated)
        val drives = client.get<PagedResult<DriveDefinition>>(url)

        drives.results.forEach {
            Logger.d("PayloadPlayground") { "Drive: Alias=${it.targetDriveInfo.alias} Type=${it.targetDriveInfo.type} Name=${it.name}" }
        }

        return drives
    }

    //

    suspend fun getFileHeader(fileId: String, alias: String, type: String, fileSystemType: String): SharedSecretEncryptedFileHeader
    {
        val url = "/api/owner/v1/drive/files/header?alias=$alias&type=$type&fileId=$fileId&xfst=$fileSystemType"

        val client = OdinHttpClient(authenticated)
        val result = client.get<SharedSecretEncryptedFileHeader>(url)

        return result
    }

    //

    suspend fun getPayloadBytes(header: SharedSecretEncryptedFileHeader, fileSystemType: String): ByteArray
    {
        // val uri = "https://$identity/api/owner/v1/drive/files/payload?alias=e8475dc46cb4b6651c2d0dbd0f3aad5f&type=8f448716e34cedf9014145e043ca6612&fileId=5201aa19-6010-2200-8aa8-fced8bf4cc24&key=pst_mdi0&xfst=128"
        //
        // val encryptedUri = buildUriWithEncryptedQueryString(uri)
        //
        // Logger.d("OdinHttpClient") { "Making GET request to: $encryptedUri" }
        //
        // val client = createHttpClient()
        //
        // var response: io.ktor.client.statement.HttpResponse
        // if (uri.contains("/api/owner")) {
        //     response = client.get(encryptedUri) {
        //         headers {
        //             append("Cookie", "DY0810=$clientAuthToken")
        //         }
        //     }
        // } else  {
        //     response = client.get(encryptedUri) {
        //         headers {
        //             append("Cookie", "XT32=$clientAuthToken")
        //         }
        //     }
        // }
        //
        // Logger.d("OdinHttpClient") { "response length: ${response.contentLength()}" }
        //
        // // Get response as raw bytes
        // val cipherBytes = response.body<ByteArray>()
        // Logger.d("OdinHttpClient") { "getPayloadBytes encrypted response length: ${cipherBytes.size}" }
        // Logger.d("OdinHttpClient") { "getPayloadBytes first 32 bytes: ${cipherBytes.take(32).joinToString(" ") { "%02x".format(it) }}" }
        // Logger.d("OdinHttpClient") { "sharedSecret length: ${sharedSecret.size}" }
        // Logger.d("OdinHttpClient") { "sharedSecret: ${sharedSecret.toBase64()}" }

        // Decrypt the response using raw byte decryption (no JSON)
        //val decryptedBytes = CryptoHelper.decryptContent(cipherBytes, sharedSecret)
        //Logger.d("OdinHttpClient") { "getPayloadBytes decrypted length: ${decryptedBytes.size}" }

        //return decryptedBytes
        return ByteArray(1)
    }


}