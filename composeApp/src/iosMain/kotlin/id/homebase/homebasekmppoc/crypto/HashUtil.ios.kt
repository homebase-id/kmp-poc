package id.homebase.homebasekmppoc.crypto

import kotlinx.cinterop.*
import platform.CoreCrypto.*

/**
 * iOS-specific HKDF implementation using CommonCrypto
 */
@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
internal actual fun platformHkdf(sharedEccSecret: ByteArray, salt: ByteArray, outputKeySize: Int): ByteArray {
    // Step 1: Extract - HMAC(salt, input)
    val prk = ByteArray(CC_SHA256_DIGEST_LENGTH)

    sharedEccSecret.usePinned { secretPinned ->
        salt.usePinned { saltPinned ->
            prk.usePinned { prkPinned ->
                CCHmac(
                    kCCHmacAlgSHA256.convert(),
                    saltPinned.addressOf(0),
                    salt.size.convert(),
                    secretPinned.addressOf(0),
                    sharedEccSecret.size.convert(),
                    prkPinned.addressOf(0)
                )
            }
        }
    }

    // Step 2: Expand
    val hashLen = CC_SHA256_DIGEST_LENGTH
    val n = (outputKeySize + hashLen - 1) / hashLen

    val result = ByteArray(outputKeySize)
    var offset = 0
    var t = ByteArray(0)

    for (i in 1..n) {
        // T(i) = HMAC(PRK, T(i-1) | info | i)
        val tNew = ByteArray(CC_SHA256_DIGEST_LENGTH)
        val dataToSign = t + byteArrayOf(i.toByte())  // info is empty in our case

        dataToSign.usePinned { dataPinned ->
            prk.usePinned { prkPinned ->
                tNew.usePinned { tNewPinned ->
                    CCHmac(
                        kCCHmacAlgSHA256.convert(),
                        prkPinned.addressOf(0),
                        prk.size.convert(),
                        dataPinned.addressOf(0),
                        dataToSign.size.convert(),
                        tNewPinned.addressOf(0)
                    )
                }
            }
        }

        val bytesToCopy = minOf(hashLen, outputKeySize - offset)
        tNew.copyInto(result, offset, 0, bytesToCopy)
        offset += bytesToCopy
        t = tNew
    }

    return result
}
