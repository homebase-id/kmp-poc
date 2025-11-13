package id.homebase.homebasekmppoc.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlinx.io.Source
import kotlinx.io.readByteArray

/**
 * Cryptographic hashing utilities using cryptography-kotlin
 */
object HashUtil {
    const val SHA256_ALGORITHM = "SHA-256"

    private val crypto = CryptographyProvider.Default
    private val sha256Algo = crypto.get(SHA256)

    /**
     * Compute SHA-256 hash of input
     */
    suspend fun sha256(input: ByteArray): ByteArray {
        return sha256Algo.hasher().hash(input)
    }

    /**
     * Compute SHA-256 hash of a stream with optional nonce
     * Returns hash and stream length
     */
    suspend fun streamSha256(inputStream: Source, optionalNonce: ByteArray? = null): Pair<ByteArray, Long> {
        // Read all data from the stream
        val streamData = inputStream.readByteArray()
        val totalBytes = streamData.size.toLong()

        // Combine nonce (if provided) with stream data
        val dataToHash = if (optionalNonce != null) {
            optionalNonce + streamData
        } else {
            streamData
        }

        // Hash the combined data
        val hash = sha256Algo.hasher().hash(dataToHash)

        return Pair(hash, totalBytes)
    }

    /**
     * HKDF (HMAC-based Extract-and-Expand Key Derivation Function) using SHA-256
     * RFC 5869
     */
    suspend fun hkdf(sharedEccSecret: ByteArray, salt: ByteArray, outputKeySize: Int): ByteArray {
        require(outputKeySize >= 16) { "Output key size cannot be less than 16" }

        // For HKDF, we'll use platform-specific implementations since HMAC in cryptography-kotlin
        // doesn't have the right API structure for our use case
        return platformHkdf(sharedEccSecret, salt, outputKeySize)
    }
}

/**
 * Platform-specific HKDF implementation (cryptography-kotlin doesn't have ideal HKDF support)
 */
internal expect fun platformHkdf(sharedEccSecret: ByteArray, salt: ByteArray, outputKeySize: Int): ByteArray
