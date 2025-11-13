package id.homebase.homebasekmppoc.crypto

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*
import platform.posix.memcpy

/**
 * iOS implementation of JWK to DER conversion
 */
@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
internal actual fun platformJwkToDer(x: ByteArray, y: ByteArray, keySize: EccKeySize): ByteArray {
    // Create uncompressed point format: 0x04 || x || y
    val uncompressedPoint = ByteArray(1 + x.size + y.size)
    uncompressedPoint[0] = 0x04.toByte()
    x.copyInto(uncompressedPoint, 1)
    y.copyInto(uncompressedPoint, 1 + x.size)

    val keySizeInBits = when (keySize) {
        EccKeySize.P256 -> 256
        EccKeySize.P384 -> 384
    }

    memScoped {
        val attributes = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionarySetValue(attributes, kSecAttrKeyType, kSecAttrKeyTypeECSECPrimeRandom)
        CFDictionarySetValue(attributes, kSecAttrKeyClass, kSecAttrKeyClassPublic)
        CFDictionarySetValue(attributes, kSecAttrKeySizeInBits, CFNumberCreate(null, kCFNumberIntType, cValuesOf(keySizeInBits).ptr))

        val data = uncompressedPoint.toNSData()
        val error = alloc<CFErrorRefVar>()

        val secKey = SecKeyCreateWithData(data.toCFData(), attributes, error.ptr)

        if (secKey == null) {
            throw IllegalStateException("Failed to create public key from JWK coordinates")
        }

        // Export as X.509 DER (this is what SecKeyCopyExternalRepresentation returns for public keys)
        val exportedData = SecKeyCopyExternalRepresentation(secKey, error.ptr)
        if (exportedData == null) {
            throw IllegalStateException("Failed to export public key")
        }

        return exportedData.toByteArray()
    }
}

/**
 * iOS implementation of DER to JWK coordinates extraction
 */
@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
internal actual fun platformDerToJwkCoordinates(derKey: ByteArray): Pair<ByteArray, ByteArray> {
    memScoped {
        val attributes = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionarySetValue(attributes, kSecAttrKeyType, kSecAttrKeyTypeECSECPrimeRandom)
        CFDictionarySetValue(attributes, kSecAttrKeyClass, kSecAttrKeyClassPublic)

        val data = derKey.toNSData()
        val error = alloc<CFErrorRefVar>()

        val secKey = SecKeyCreateWithData(data.toCFData(), attributes, error.ptr)
        if (secKey == null) {
            throw IllegalStateException("Failed to create public key from DER")
        }

        // Export as uncompressed point
        val exportedData = SecKeyCopyExternalRepresentation(secKey, error.ptr)
        if (exportedData == null) {
            throw IllegalStateException("Failed to export public key")
        }

        val uncompressedPoint = exportedData.toByteArray()

        // Uncompressed format is: 0x04 || x || y
        require(uncompressedPoint[0] == 0x04.toByte()) { "Invalid uncompressed point format" }

        val coordinateSize = (uncompressedPoint.size - 1) / 2
        val x = uncompressedPoint.copyOfRange(1, 1 + coordinateSize)
        val y = uncompressedPoint.copyOfRange(1 + coordinateSize, uncompressedPoint.size)

        return Pair(x, y)
    }
}

/**
 * iOS implementation of curve detection from DER key
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun platformGetCurveFromKey(derKey: ByteArray): EccKeySize {
    memScoped {
        val attributes = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionarySetValue(attributes, kSecAttrKeyType, kSecAttrKeyTypeECSECPrimeRandom)
        CFDictionarySetValue(attributes, kSecAttrKeyClass, kSecAttrKeyClassPublic)

        val data = derKey.toNSData()
        val error = alloc<CFErrorRefVar>()

        val secKey = SecKeyCreateWithData(data.toCFData(), attributes, error.ptr)
        if (secKey == null) {
            throw IllegalStateException("Failed to create public key from DER")
        }

        val blockSize = SecKeyGetBlockSize(secKey).toInt()

        return when (blockSize) {
            32 -> EccKeySize.P256
            48 -> EccKeySize.P384
            else -> throw IllegalArgumentException("Unsupported ECC key size with block size: $blockSize")
        }
    }
}

/**
 * iOS implementation of ECC key pair generation
 */
@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
internal actual fun platformGenerateEccKeyPair(keySize: EccKeySize): Pair<ByteArray, ByteArray> {
    val keySizeInBits = when (keySize) {
        EccKeySize.P256 -> 256
        EccKeySize.P384 -> 384
    }

    memScoped {
        val attributes = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionarySetValue(attributes, kSecAttrKeyType, kSecAttrKeyTypeECSECPrimeRandom)
        CFDictionarySetValue(attributes, kSecAttrKeySizeInBits, CFNumberCreate(null, kCFNumberIntType, cValuesOf(keySizeInBits).ptr))

        val error = alloc<CFErrorRefVar>()

        val privateKey = SecKeyCreateRandomKey(attributes, error.ptr)
        if (privateKey == null) {
            throw IllegalStateException("Failed to generate key pair")
        }

        val publicKey = SecKeyCopyPublicKey(privateKey)
        if (publicKey == null) {
            throw IllegalStateException("Failed to extract public key")
        }

        // Export private key
        val privateKeyData = SecKeyCopyExternalRepresentation(privateKey, error.ptr)
        if (privateKeyData == null) {
            throw IllegalStateException("Failed to export private key")
        }

        // Export public key
        val publicKeyData = SecKeyCopyExternalRepresentation(publicKey, error.ptr)
        if (publicKeyData == null) {
            throw IllegalStateException("Failed to export public key")
        }

        return Pair(
            privateKeyData.toByteArray(),
            publicKeyData.toByteArray()
        )
    }
}

/**
 * iOS implementation of ECDH key agreement
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal actual fun platformEcdhKeyAgreement(privateKeyDer: ByteArray, publicKeyDer: ByteArray): ByteArray {
    memScoped {
        // Create private key
        val privateAttributes = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionarySetValue(privateAttributes, kSecAttrKeyType, kSecAttrKeyTypeECSECPrimeRandom)
        CFDictionarySetValue(privateAttributes, kSecAttrKeyClass, kSecAttrKeyClassPrivate)

        val privateData = privateKeyDer.toNSData()
        val privateError = alloc<CFErrorRefVar>()

        val privateKey = SecKeyCreateWithData(privateData.toCFData(), privateAttributes, privateError.ptr)
        if (privateKey == null) {
            throw IllegalStateException("Failed to create private key")
        }

        // Create public key
        val publicAttributes = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionarySetValue(publicAttributes, kSecAttrKeyType, kSecAttrKeyTypeECSECPrimeRandom)
        CFDictionarySetValue(publicAttributes, kSecAttrKeyClass, kSecAttrKeyClassPublic)

        val publicData = publicKeyDer.toNSData()
        val publicError = alloc<CFErrorRefVar>()

        val publicKey = SecKeyCreateWithData(publicData.toCFData(), publicAttributes, publicError.ptr)
        if (publicKey == null) {
            throw IllegalStateException("Failed to create public key")
        }

        // Perform ECDH using standard algorithm
        val parameters = CFDictionaryCreateMutable(null, 0, null, null)

        val error = alloc<CFErrorRefVar>()
        val sharedSecret = SecKeyCopyKeyExchangeResult(
            privateKey,
            kSecKeyAlgorithmECDHKeyExchangeStandard,
            publicKey,
            parameters,
            error.ptr
        )

        if (sharedSecret == null) {
            throw IllegalStateException("ECDH key agreement failed")
        }

        return sharedSecret.toByteArray()
    }
}

/**
 * Helper extension to convert ByteArray to NSData
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData {
    return this.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
    }
}

/**
 * Helper extension to convert NSData to CFDataRef
 * Uses toll-free bridging between NSData and CFDataRef
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun NSData.toCFData(): CFDataRef {
    // Use CFBridgingRetain to get a CFDataRef from NSData
    // NSData and CFDataRef are toll-free bridged, so we can use interpretCPointer
    return interpretCPointer(objcPtr())!!
}

/**
 * Helper extension to convert CFDataRef to ByteArray
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun CFDataRef.toByteArray(): ByteArray {
    val length = CFDataGetLength(this).toInt()
    val bytes = CFDataGetBytePtr(this)
    return ByteArray(length).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length.toULong())
        }
    }
}
