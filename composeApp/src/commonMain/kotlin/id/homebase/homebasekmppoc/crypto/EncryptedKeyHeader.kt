package id.homebase.homebasekmppoc.crypto

import id.homebase.homebasekmppoc.core.SensitiveByteArray
import id.homebase.homebasekmppoc.toBase64
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64

/**
 * Encrypted key header for secure key storage
 * Port of C# EncryptedKeyHeader class from Odin.Services.Peer.Encryption
 */
@Serializable
class EncryptedKeyHeader(
    var encryptionVersion: Int = 1,
    var type: EncryptionType = EncryptionType.Aes,
    var iv: ByteArray,
    var encryptedAesKey: ByteArray
) {
    /**
     * Decrypts this Encrypted Key header
     * @param key The decryption key
     * @return Decrypted KeyHeader
     * @throws Exception if unsupported encryption version
     */
    suspend fun decryptAesToKeyHeader(key: SensitiveByteArray): KeyHeader {
        if (encryptionVersion == 1) {
            val bytes = AesCbc.decrypt(encryptedAesKey, key, iv)
            val kh = KeyHeader.fromCombinedBytes(bytes, 16, 16)
            SensitiveByteArray(bytes).wipe()
            return kh
        }

        throw Exception("Unsupported encryption version")
    }

    /**
     * Combines IV and encrypted AES key into a single byte array
     */
    fun combine(): SensitiveByteArray {
        // TODO: I don't know the length of encrypted AES Key so maybe base64 encode this instead?
        return SensitiveByteArray(ByteArrayUtil.combine(iv, encryptedAesKey))
    }

    /**
     * Converts this encrypted key header to Base64 string
     */
    fun toBase64(): String {
        val versionBytes = ByteArrayUtil.int32ToBytes(encryptionVersion)
        val combinedBytes = ByteArrayUtil.combine(iv, encryptedAesKey, versionBytes)
        val encryptedKeyHeader64 = combinedBytes.toBase64()
        SensitiveByteArray(combinedBytes).wipe()
        return encryptedKeyHeader64
    }

    companion object {
        /**
         * Encrypts a KeyHeader using AES
         * @param keyHeader The key header to encrypt
         * @param iv The initialization vector
         * @param key The encryption key
         * @return Encrypted key header
         */
        suspend fun encryptKeyHeaderAes(
            keyHeader: KeyHeader,
            iv: ByteArray,
            key: SensitiveByteArray
        ): EncryptedKeyHeader {
            val secureKeyHeader = keyHeader.combine()
            val data = AesCbc.encrypt(secureKeyHeader.getKey(), key, iv)
            secureKeyHeader.wipe()

            return EncryptedKeyHeader(
                encryptionVersion = 1,
                type = EncryptionType.Aes,
                iv = iv,
                encryptedAesKey = data
            )
        }

        /**
         * Creates an empty EncryptedKeyHeader
         */
        fun empty(): EncryptedKeyHeader {
            val empty = ByteArray(16) { 0 }
            return EncryptedKeyHeader(
                encryptionVersion = 1,
                type = EncryptionType.Aes,
                iv = empty,
                encryptedAesKey = ByteArrayUtil.combine(empty, empty, empty)
            )
        }

        /**
         * Creates an EncryptedKeyHeader from Base64 string
         * @param data64 Base64 encoded string
         * @return Decoded EncryptedKeyHeader
         */
        fun fromBase64(data64: String): EncryptedKeyHeader {
            val bytes = Base64.decode(data64)
            val parts = ByteArrayUtil.split(bytes, 16, 48, 4)
            val iv = parts[0]
            val encryptedAesKey = parts[1]
            val version = parts[2]

            return EncryptedKeyHeader(
                iv = iv,
                encryptedAesKey = encryptedAesKey,
                encryptionVersion = ByteArrayUtil.bytesToInt32(version)
            )
        }
    }
}
