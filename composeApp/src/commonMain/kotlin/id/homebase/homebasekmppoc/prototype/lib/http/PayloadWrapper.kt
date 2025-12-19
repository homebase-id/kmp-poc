package id.homebase.homebasekmppoc.prototype.lib.http

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.prototype.lib.authentication.AuthState
import id.homebase.homebasekmppoc.prototype.lib.core.SecureByteArray
import id.homebase.homebasekmppoc.prototype.lib.crypto.CryptoHelper
import id.homebase.homebasekmppoc.prototype.lib.crypto.KeyHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.PayloadDescriptor
import id.homebase.homebasekmppoc.prototype.lib.drives.SharedSecretEncryptedFileHeader
import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import id.homebase.homebasekmppoc.prototype.lib.video.VideoMetaData
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.http.contentLength
import kotlin.io.encoding.Base64

class PayloadWrapper(
    val authenticated: AuthState.Authenticated,
    val header: SharedSecretEncryptedFileHeader,
    val payloadDescriptor: PayloadDescriptor) {

    //

    val headerWrapper = HeaderWrapper(authenticated, header)

    val isEncrypted: Boolean get() = headerWrapper.isEncrypted

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

    fun getCompositeKey(): String {
        return header.fileId.toString() + "-" + payloadDescriptor.key
    }

    //

    fun getPayloadUri(appOrOwner: AppOrOwner): String {
        val fileId = header.fileId
        val alias = header.targetDrive.alias
        val type = header.targetDrive.type
        val payloadKey = payloadDescriptor.key

        // calls backend DriveStorageControllerBase.GetPayloadStream
        // val uri = "https://${authenticated.identity}/api/$appOrOwner/v1/drive/files/payload?alias=$alias&type=$type&fileId=$fileId&key=$payloadKey&xfst=128"

        //val uri =   "https://${authenticated.identity}/api/v2/drives/$alias/files/$fileId/payload/?key=$payloadKey"
        //val uri = "https://${authenticated.identity}/api/v2/drives/$alias/files/$fileId/payloads/$payloadKey"

        val uri = "https://${authenticated.identity}/api/$appOrOwner/v1/drive/files/payload/$alias/$type/$fileId/$payloadKey"

        return uri
    }

    //

    suspend fun getEncryptedPayloadUri(appOrOwner: AppOrOwner): String {
        val plain = getPayloadUri(appOrOwner)
        Logger.d("PayloadPlayground") { "Plain uri: $plain" }

        val cipher = CryptoHelper.uriWithEncryptedQueryString(plain, authenticated.sharedSecret)
        Logger.d("PayloadPlayground") { "Cipher uri: $cipher" }

        return cipher
    }

    //

    suspend fun getPayloadBytes(appOrOwner: AppOrOwner): ByteArray {

        val encryptedUri = getEncryptedPayloadUri(appOrOwner)

        Logger.d("PayloadPlayground") { "Making GET request to: $encryptedUri" }

        val client = createHttpClient()
        val response = client.get(encryptedUri) {
            if (false && encryptedUri.contains("/api/v2/", ignoreCase = true)) {
                bearerAuth(authenticated.clientAuthToken)
            } else {
                headers {
                    append("Cookie", "${cookieNameFrom(appOrOwner)}=${authenticated.clientAuthToken}")
                }
            }
            headers {
                append("X-ODIN-FILE-SYSTEM-TYPE", "128")
            }
        }
        Logger.d("PayloadPlayground") { "Response length: ${response.contentLength()}" }

        val bytes = response.body<ByteArray>()
        Logger.d("PayloadPlayground") { "Payload length: ${bytes.size}" }

        if (!isEncrypted) {
            return bytes
        } else {
            val payloadIvBase64 = payloadDescriptor.iv ?: throw Exception("No IV found in payload descriptor")
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

    fun getVideoMetaData(appOrOwner: AppOrOwner): VideoMetaData {
        val playlistContent = payloadDescriptor.descriptorContent
            ?: throw Exception("No descriptor content found in payload")

        val videoMetaData = OdinSystemSerializer.deserialize<VideoMetaData>(playlistContent)
        return videoMetaData
    }

    //

}
