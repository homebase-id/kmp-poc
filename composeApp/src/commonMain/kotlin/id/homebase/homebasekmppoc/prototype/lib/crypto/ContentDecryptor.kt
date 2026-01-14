package id.homebase.homebasekmppoc.prototype.lib.crypto

import id.homebase.homebasekmppoc.prototype.lib.core.SecureByteArray
import id.homebase.homebasekmppoc.prototype.lib.drives.SharedSecretEncryptedFileHeader
import kotlin.io.encoding.Base64

/**
 * Utility for decrypting content from SharedSecretEncryptedFileHeader.
 *
 * Usage in a component:
 * ```kotlin
 * val decryptedContent = ContentDecryptor.decryptContent(
 *     header = sharedSecretEncryptedFileHeader,
 *     sharedSecret = authenticatedState.sharedSecret  // Base64 encoded
 * )
 * ```
 */
object ContentDecryptor {

    /**
     * Decrypts the appData.content from a SharedSecretEncryptedFileHeader.
     *
     * @param header The encrypted file header containing sharedSecretEncryptedKeyHeader
     * @param sharedSecret Base64-encoded shared secret from YouAuthState.Authenticated
     * @return Decrypted content as String, or null if content is empty or decryption fails
     */
    suspend fun decryptContent(
            header: SharedSecretEncryptedFileHeader,
            sharedSecret: String
    ): String? {
        val encryptedContent = header.fileMetadata.appData.content
        if (encryptedContent.isNullOrEmpty()) {
            return null
        }

        // If not encrypted, return as-is
        if (!header.fileMetadata.isEncrypted) {
            return encryptedContent
        }

        return try {
            // Step 1: Decode the shared secret from Base64
            val sharedSecretBytes = Base64.decode(sharedSecret)

            // Step 2: Decrypt the EncryptedKeyHeader to get the KeyHeader
            val keyHeader =
                    header.sharedSecretEncryptedKeyHeader.decryptAesToKeyHeader(
                            SecureByteArray(sharedSecretBytes)
                    )

            // Step 3: Decode the encrypted content from Base64 and decrypt
            val encryptedBytes = Base64.decode(encryptedContent)
            val decryptedBytes = keyHeader.decrypt(encryptedBytes)

            // Step 4: Convert to String
            decryptedBytes.decodeToString()
        } catch (e: Exception) {
            // Log error but return null to allow graceful degradation
            println("ContentDecryptor: Failed to decrypt content: ${e.message}")
            null
        }
    }

    /**
     * Decrypts arbitrary encrypted data using the shared secret and encrypted key header.
     *
     * @param encryptedData Base64-encoded encrypted data
     * @param header The file header containing the encrypted key header
     * @param sharedSecret Base64-encoded shared secret
     * @return Decrypted bytes, or null if decryption fails
     */
    suspend fun decryptData(
            encryptedData: String,
            header: SharedSecretEncryptedFileHeader,
            sharedSecret: String
    ): ByteArray? {
        return try {
            val sharedSecretBytes = Base64.decode(sharedSecret)
            val keyHeader =
                    header.sharedSecretEncryptedKeyHeader.decryptAesToKeyHeader(
                            SecureByteArray(sharedSecretBytes)
                    )
            val encryptedBytes = Base64.decode(encryptedData)
            keyHeader.decrypt(encryptedBytes)
        } catch (e: Exception) {
            println("ContentDecryptor: Failed to decrypt data: ${e.message}")
            null
        }
    }

    /**
     * Gets the KeyHeader from a SharedSecretEncryptedFileHeader for manual decryption.
     *
     * @param header The encrypted file header
     * @param sharedSecret Base64-encoded shared secret
     * @return KeyHeader for decryption, or null if decryption fails
     */
    suspend fun getKeyHeader(
            header: SharedSecretEncryptedFileHeader,
            sharedSecret: String
    ): KeyHeader? {
        return try {
            val sharedSecretBytes = Base64.decode(sharedSecret)
            header.sharedSecretEncryptedKeyHeader.decryptAesToKeyHeader(
                    SecureByteArray(sharedSecretBytes)
            )
        } catch (e: Exception) {
            println("ContentDecryptor: Failed to get key header: ${e.message}")
            null
        }
    }
}
