package id.homebase.homebasekmppoc.prototype.lib.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import id.homebase.homebasekmppoc.prototype.lib.core.SecureByteArray

/**
 * AES-CBC encryption/decryption utilities using cryptography-kotlin
 */
object AesCbc {

    private val crypto = CryptographyProvider.Companion.Default
    private val aes = crypto.get(AES.CBC)

    /**
     * Encrypt data with AES-CBC using the provided key and IV
     * Use this when you need to reencrypt with the same IV (e.g., transforming headers)
     */
    suspend fun encrypt(data: ByteArray, key: SecureByteArray, iv: ByteArray): ByteArray {
        return encrypt(data, key.unsafeBytes, iv)
    }

    /**
     * Encrypt data with AES-CBC using the provided key and IV
     */
    suspend fun encrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        require(data.isNotEmpty()) { "Data cannot be empty" }
        require(key.isNotEmpty()) { "Key cannot be empty" }
        require(iv.size == 16) { "IV must be 16 bytes" }

        val aesKey = aes.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, key)
        val cipher = aesKey.cipher()
        return cipher.encryptWithIv(iv, data)
    }

    /**
     * Encrypt data with AES-CBC using the provided key and a randomly generated IV
     * Returns a pair of (IV, ciphertext)
     */
    suspend fun encrypt(data: ByteArray, key: SecureByteArray): Pair<ByteArray, ByteArray> {
        require(data.isNotEmpty()) { "Data cannot be empty" }

        val iv = ByteArrayUtil.getRndByteArray(16)
        val ciphertext = encrypt(data, key.unsafeBytes, iv)

        return Pair(iv, ciphertext)
    }

    /**
     * Decrypt data with AES-CBC using the provided key and IV
     */
    suspend fun decrypt(cipherText: ByteArray, key: SecureByteArray, iv: ByteArray): ByteArray {
        return decrypt(cipherText, key.unsafeBytes, iv)
    }

    /**
     * Decrypt data with AES-CBC using the provided key and IV
     */
    suspend fun decrypt(cipherText: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        require(cipherText.isNotEmpty()) { "CipherText cannot be empty" }
        require(key.isNotEmpty()) { "Key cannot be empty" }
        require(iv.size == 16) { "IV must be 16 bytes" }

        val aesKey = aes.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, key)
        val cipher = aesKey.cipher()
        return cipher.decryptWithIv(iv, cipherText)
    }
}