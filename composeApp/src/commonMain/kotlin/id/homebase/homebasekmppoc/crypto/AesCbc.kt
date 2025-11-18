package id.homebase.homebasekmppoc.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import id.homebase.homebasekmppoc.core.SensitiveByteArray

/**
 * AES-CBC encryption/decryption utilities using cryptography-kotlin
 */
@OptIn(DelicateCryptographyApi::class)
object AesCbc {

    private val crypto = CryptographyProvider.Default
    private val aes = crypto.get(AES.CBC)

    /**
     * Encrypt data with AES-CBC using the provided key and IV
     * Use this when you need to reencrypt with the same IV (e.g., transforming headers)
     */
    suspend fun encrypt(data: ByteArray, key: SensitiveByteArray, iv: ByteArray): ByteArray {
        return encrypt(data, key.getKey(), iv)
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
    suspend fun encrypt(data: ByteArray, key: SensitiveByteArray): Pair<ByteArray, ByteArray> {
        require(data.isNotEmpty()) { "Data cannot be empty" }

        val iv = ByteArrayUtil.getRndByteArray(16)
        val ciphertext = encrypt(data, key.getKey(), iv)

        return Pair(iv, ciphertext)
    }

    /**
     * Decrypt data with AES-CBC using the provided key and IV
     */
    suspend fun decrypt(cipherText: ByteArray, key: SensitiveByteArray, iv: ByteArray): ByteArray {
        return decrypt(cipherText, key.getKey(), iv)
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
