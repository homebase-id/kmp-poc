package id.homebase.homebasekmppoc.prototype.lib.http

import id.homebase.homebasekmppoc.prototype.lib.authentication.AuthState
import id.homebase.homebasekmppoc.prototype.lib.core.SecureByteArray
import id.homebase.homebasekmppoc.prototype.lib.crypto.KeyHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.PayloadDescriptor
import id.homebase.homebasekmppoc.prototype.lib.drives.SharedSecretEncryptedFileHeader
import kotlin.io.encoding.Base64

class HeaderWrapper(
    val authenticated: AuthState.Authenticated,
    val header: SharedSecretEncryptedFileHeader) {

    val payloads = header.fileMetadata.payloads ?: emptyList()

    val isEncrypted: Boolean get() = header.fileMetadata.isEncrypted

    val payloadDescriptors: List<PayloadDescriptor> get() = payloads

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

    fun getPayloadDescriptor(key: String): PayloadDescriptor {
        val payload = payloads.find { it.key == key }
        if (payload == null) {
            throw Exception("Payload with key $key not found")
        }
        return payload
    }

    //

    fun getVideoPayloadDescriptors(): List<PayloadDescriptor> {
        return payloads.filter { it.contentType?.contains("video") == true }
    }

    //



}


