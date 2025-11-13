package id.homebase.homebasekmppoc.crypto

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Android-specific HKDF implementation using javax.crypto
 */
internal actual fun platformHkdf(sharedEccSecret: ByteArray, salt: ByteArray, outputKeySize: Int): ByteArray {
    // Step 1: Extract - HMAC(salt, input)
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(salt, "HmacSHA256"))
    val prk = mac.doFinal(sharedEccSecret)

    // Step 2: Expand
    val hashLen = 32 // SHA-256 produces 32 bytes
    val n = (outputKeySize + hashLen - 1) / hashLen

    val result = ByteArray(outputKeySize)
    var offset = 0
    var t = ByteArray(0)

    for (i in 1..n) {
        // T(i) = HMAC(PRK, T(i-1) | info | i)
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        mac.update(t)
        // info is empty in our case
        mac.update(byteArrayOf(i.toByte()))
        t = mac.doFinal()

        val bytesToCopy = minOf(hashLen, outputKeySize - offset)
        t.copyInto(result, offset, 0, bytesToCopy)
        offset += bytesToCopy
    }

    return result
}
