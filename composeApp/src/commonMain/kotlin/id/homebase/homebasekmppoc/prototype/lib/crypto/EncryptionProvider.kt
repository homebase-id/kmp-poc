package id.homebase.homebasekmppoc.prototype.lib.crypto

import id.homebase.homebasekmppoc.prototype.lib.core.OdinClientErrorCode
import id.homebase.homebasekmppoc.prototype.lib.core.OdinClientException
import id.homebase.homebasekmppoc.prototype.lib.core.SecureByteArray
import id.homebase.homebasekmppoc.prototype.lib.http.OdinClient


data class EncryptedData(
    val bytes: ByteArray,
    val iv: ByteArray?
)

class EncryptionProvider(
    private val client: OdinClient
) {

    suspend fun encryptUsingEncryptedKeyHeader(
        plaintext: ByteArray,
        encryptedKeyHeader: EncryptedKeyHeader,
        existingIv: ByteArray? = null
    ): EncryptedData {
        val sharedSecret =
            client.getSharedSecret()
                ?: throw OdinClientException(
                    "Missing shared secret",
                    OdinClientErrorCode.SharedSecretEncryptionIsInvalid
                )

        return encryptedKeyHeader.encryptUsingEncryptedKeyHeader(
            plaintext = plaintext,
            sharedSecret = SecureByteArray(sharedSecret),
            existingIv = existingIv
        )
    }
}

/**
 * Encrypts plaintext using the AES key represented by this EncryptedKeyHeader.
 *
 * Ensures the resulting ciphertext is compatible with the file
 * that originally produced this header.
 */
suspend fun EncryptedKeyHeader.encryptUsingEncryptedKeyHeader(
    plaintext: ByteArray,
    sharedSecret: SecureByteArray,
    existingIv: ByteArray? = null
): EncryptedData {
    val decryptedKeyHeader =
        decryptAesToKeyHeader(sharedSecret)

    val keyHeader = KeyHeader(
        iv = existingIv ?: ByteArrayUtil.getRndByteArray(16),
        aesKey = decryptedKeyHeader.aesKey
    )

    val encryptedBytes =
        keyHeader.encryptDataAes(plaintext)

    return EncryptedData(
        bytes = encryptedBytes,
        iv = keyHeader.iv
    )
}
