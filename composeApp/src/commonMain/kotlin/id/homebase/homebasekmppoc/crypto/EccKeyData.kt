package id.homebase.homebasekmppoc.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.EC
import dev.whyoleg.cryptography.algorithms.ECDH
import id.homebase.homebasekmppoc.core.SensitiveByteArray
import id.homebase.homebasekmppoc.core.UnixTimeUtc
import id.homebase.homebasekmppoc.core.toSensitiveByteArray
import kotlinx.serialization.json.Json

/**
 * ECC key sizes supported
 */
enum class EccKeySize {
    P256,
    P384
}

/**
 * ECC public key data
 */
open class EccPublicKeyData(
    var publicKey: ByteArray = ByteArray(0),  // DER encoded public key
    var crc32c: UInt = 0u,                     // CRC32C of the public key
    var expiration: UnixTimeUtc = UnixTimeUtc.ZeroTime,  // Expiration time
    var keySize: EccKeySize? = null            // Curve size (P-256 or P-384)
) {
    companion object {
        val eccSignatureAlgorithmNames = arrayOf("SHA256withECDSA", "SHA384withECDSA")
        val eccKeyTypeNames = arrayOf("P-256", "P-384")
        val eccCurveIdentifiers = arrayOf("secp256r1", "secp384r1")

        /**
         * Create EccPublicKeyData from JWK (JSON Web Key) format
         */
        suspend fun fromJwkPublicKey(jwk: String, hours: Int = 1): EccPublicKeyData {
            val jwkMap = Json.decodeFromString<Map<String, String>>(jwk)

            require(jwkMap["kty"] == "EC") { "Invalid key type, kty must be EC" }

            val curveName = jwkMap["crv"] ?: throw IllegalArgumentException("Missing crv field")
            require(curveName == "P-384" || curveName == "P-256") {
                "Invalid curve, crv must be P-384 OR P-256"
            }

            val x = Base64UrlEncoder.decode(jwkMap["x"] ?: throw IllegalArgumentException("Missing x coordinate"))
            val y = Base64UrlEncoder.decode(jwkMap["y"] ?: throw IllegalArgumentException("Missing y coordinate"))

            val keySize = if (curveName == "P-384") EccKeySize.P384 else EccKeySize.P256
            val derEncodedPublicKey = platformJwkToDer(x, y, keySize)

            return EccPublicKeyData(
                publicKey = derEncodedPublicKey,
                crc32c = keyCrc(derEncodedPublicKey),
                expiration = UnixTimeUtc.now().addHours(hours.toLong()),
                keySize = keySize
            )
        }

        /**
         * Create EccPublicKeyData from base64url-encoded JWK
         */
        suspend fun fromJwkBase64UrlPublicKey(jwkBase64Url: String, hours: Int = 1): EccPublicKeyData {
            return fromJwkPublicKey(Base64UrlEncoder.decodeString(jwkBase64Url), hours)
        }

        /**
         * Calculate CRC32C of a key
         */
        fun keyCrc(keyDerEncoded: ByteArray): UInt {
            return Crc32c.calculateCrc32c(0u, keyDerEncoded)
        }
    }

    /**
     * Get the curve type from the public key
     */
    protected suspend fun getCurveEnum(): EccKeySize {
        // Use stored keySize if available, otherwise detect from key
        return keySize ?: platformGetCurveFromKey(publicKey)
    }

    /**
     * Convert public key to JWK format
     */
    suspend fun publicKeyJwk(): String {
        val curveSize = getCurveEnum()
        val (x, y) = platformDerToJwkCoordinates(publicKey, curveSize)

        val expectedBytes = if (curveSize == EccKeySize.P384) 48 else 32

        // Ensure coordinates are the correct length (pad with zeros if needed)
        val xPadded = ensureLength(x, expectedBytes)
        val yPadded = ensureLength(y, expectedBytes)

        val curveName = eccKeyTypeNames[curveSize.ordinal]

        val jwk = mapOf(
            "kty" to "EC",
            "crv" to curveName,
            "x" to Base64UrlEncoder.encode(xPadded),
            "y" to Base64UrlEncoder.encode(yPadded)
        )

        return Json.encodeToString(kotlinx.serialization.serializer(), jwk)
    }

    /**
     * Convert public key to base64url-encoded JWK
     */
    suspend fun publicKeyJwkBase64Url(): String {
        return Base64UrlEncoder.encode(publicKeyJwk())
    }

    /**
     * Calculate CRC32C of this key
     */
    fun keyCrc(): UInt {
        return keyCrc(publicKey)
    }

    /**
     * Ensure byte array is the specified length (pad with zeros at the beginning if needed)
     */
    private fun ensureLength(bytes: ByteArray, length: Int): ByteArray {
        if (bytes.size >= length) return bytes

        val padded = ByteArray(length)
        bytes.copyInto(padded, length - bytes.size)
        return padded
    }
}

/**
 * ECC full key data (includes private key)
 */
class EccFullKeyData private constructor() : EccPublicKeyData() {
    private var _privateKey: SensitiveByteArray? = null  // Cached decrypted private key

    var storedKey: ByteArray = ByteArray(0)  // Encrypted private key
    var iv: ByteArray = ByteArray(0)          // IV for encryption
    var keyHash: ByteArray = ByteArray(0)     // Hash of encryption key
    var createdTimeStamp: UnixTimeUtc = UnixTimeUtc.ZeroTime

    companion object {
        /**
         * Create a new ECC full key pair
         */
        suspend fun create(
            key: SensitiveByteArray,
            keySize: EccKeySize,
            hours: Int,
            minutes: Int = 0,
            seconds: Int = 0
        ): EccFullKeyData {
            val keyData = EccFullKeyData()

            // Generate ECC key pair
            val (privateKeyDer, publicKeyDer) = platformGenerateEccKeyPair(keySize)

            // Set timestamps
            keyData.createdTimeStamp = UnixTimeUtc.now()
            keyData.expiration = keyData.createdTimeStamp
                .addSeconds((hours * 3600 + minutes * 60 + seconds).toLong())

            require(keyData.expiration > keyData.createdTimeStamp) { "Expiration must be > 0" }

            // Store the private key encrypted
            keyData.createPrivate(key, privateKeyDer)

            // Store the public key
            keyData.publicKey = publicKeyDer
            keyData.crc32c = keyCrc(publicKeyDer)
            keyData.keySize = keySize  // Store the key size

            return keyData
        }
    }

    private suspend fun createPrivate(key: SensitiveByteArray, fullDerKey: ByteArray) {
        iv = ByteArrayUtil.getRndByteArray(16)
        keyHash = ByteArrayUtil.reduceSha256Hash(key.getKey())
        _privateKey = SensitiveByteArray(fullDerKey)
        storedKey = AesCbc.encrypt(fullDerKey, key, iv)
    }

    private suspend fun getFullKey(key: SensitiveByteArray): SensitiveByteArray {
        if (!ByteArrayUtil.equiByteArrayCompare(keyHash, ByteArrayUtil.reduceSha256Hash(key.getKey()))) {
            throw IllegalStateException("Incorrect key")
        }

        if (_privateKey == null) {
            _privateKey = SensitiveByteArray(AesCbc.decrypt(storedKey, key, iv))
        }

        return _privateKey!!
    }

    /**
     * Perform ECDH key agreement to derive a shared secret
     */
    suspend fun getEcdhSharedSecret(pwd: SensitiveByteArray, remotePublicKey: EccPublicKeyData, randomSalt: ByteArray): SensitiveByteArray {
        require(randomSalt.size >= 16) { "Salt must be at least 16 bytes" }

        // Get the private key
        val privateKeyBytes = getFullKey(pwd).getKey()

        // Perform ECDH and derive shared secret
        val sharedSecret = platformEcdhKeyAgreement(privateKeyBytes, remotePublicKey.publicKey)

        // Apply HKDF to derive a symmetric key from the shared secret
        val derivedKey = HashUtil.hkdf(sharedSecret, randomSalt, 16)

        return derivedKey.toSensitiveByteArray()
    }
}

/**
 * Convert JWK coordinates to DER-encoded public key using cryptography-kotlin
 */
@OptIn(DelicateCryptographyApi::class)
internal suspend fun platformJwkToDer(x: ByteArray, y: ByteArray, keySize: EccKeySize): ByteArray {
    val crypto = CryptographyProvider.Default
    val ecdh = crypto.get(ECDH)

    val curve = when (keySize) {
        EccKeySize.P256 -> EC.Curve.P256
        EccKeySize.P384 -> EC.Curve.P384
    }

    // Create uncompressed point format: 0x04 || x || y
    val uncompressedPoint = ByteArray(1 + x.size + y.size)
    uncompressedPoint[0] = 0x04.toByte()
    x.copyInto(uncompressedPoint, 1)
    y.copyInto(uncompressedPoint, 1 + x.size)

    // Decode from RAW (uncompressed) format
    val publicKey = ecdh.publicKeyDecoder(curve).decodeFromByteArray(EC.PublicKey.Format.RAW.Uncompressed, uncompressedPoint)

    // Encode to DER format (SPKI)
    return publicKey.encodeToByteArray(EC.PublicKey.Format.DER)
}

/**
 * Extract JWK coordinates from DER-encoded public key using cryptography-kotlin
 */
@OptIn(DelicateCryptographyApi::class)
internal suspend fun platformDerToJwkCoordinates(derKey: ByteArray, keySize: EccKeySize): Pair<ByteArray, ByteArray> {
    val crypto = CryptographyProvider.Default
    val ecdh = crypto.get(ECDH)

    // Use the provided keySize instead of detecting
    val curve = if (keySize == EccKeySize.P384) EC.Curve.P384 else EC.Curve.P256

    // Decode from DER
    val publicKey = ecdh.publicKeyDecoder(curve).decodeFromByteArray(EC.PublicKey.Format.DER, derKey)

    // Encode to RAW uncompressed format: 0x04 || x || y
    val uncompressedPoint = publicKey.encodeToByteArray(EC.PublicKey.Format.RAW.Uncompressed)

    // Uncompressed format is: 0x04 || x || y
    require(uncompressedPoint[0] == 0x04.toByte()) { "Invalid uncompressed point format" }

    val coordinateSize = (uncompressedPoint.size - 1) / 2
    val x = uncompressedPoint.copyOfRange(1, 1 + coordinateSize)
    val y = uncompressedPoint.copyOfRange(1 + coordinateSize, uncompressedPoint.size)

    return Pair(x, y)
}

/**
 * Get the curve type from a DER-encoded key using cryptography-kotlin
 */
@OptIn(DelicateCryptographyApi::class)
internal suspend fun platformGetCurveFromKey(derKey: ByteArray): EccKeySize {
    val crypto = CryptographyProvider.Default
    val ecdh = crypto.get(ECDH)

    // Try P-256 first
    return try {
        ecdh.publicKeyDecoder(EC.Curve.P256).decodeFromByteArray(EC.PublicKey.Format.DER, derKey)
        EccKeySize.P256
    } catch (e: Exception) {
        try {
            ecdh.publicKeyDecoder(EC.Curve.P384).decodeFromByteArray(EC.PublicKey.Format.DER, derKey)
            EccKeySize.P384
        } catch (e: Exception) {
            throw IllegalArgumentException("Unsupported ECC key size")
        }
    }
}

/**
 * Generate an ECC key pair using cryptography-kotlin
 * Returns (privateKeyDer, publicKeyDer)
 */
@OptIn(DelicateCryptographyApi::class)
internal suspend fun platformGenerateEccKeyPair(keySize: EccKeySize): Pair<ByteArray, ByteArray> {
    val crypto = CryptographyProvider.Default
    val ecdh = crypto.get(ECDH)

    val curve = when (keySize) {
        EccKeySize.P256 -> EC.Curve.P256
        EccKeySize.P384 -> EC.Curve.P384
    }

    // Generate key pair
    val keyPair = ecdh.keyPairGenerator(curve).generateKey()

    // Encode both keys to DER format
    val privateKeyDer = keyPair.privateKey.encodeToByteArray(EC.PrivateKey.Format.DER.Generic)
    val publicKeyDer = keyPair.publicKey.encodeToByteArray(EC.PublicKey.Format.DER)

    return Pair(privateKeyDer, publicKeyDer)
}

/**
 * Perform ECDH key agreement using cryptography-kotlin
 * Returns the raw shared secret (before HKDF)
 */
@OptIn(DelicateCryptographyApi::class)
internal suspend fun platformEcdhKeyAgreement(privateKeyDer: ByteArray, publicKeyDer: ByteArray): ByteArray {
    val crypto = CryptographyProvider.Default
    val ecdh = crypto.get(ECDH)

    // First, detect the curve from the public key
    val curve = try {
        ecdh.publicKeyDecoder(EC.Curve.P256).decodeFromByteArray(EC.PublicKey.Format.DER, publicKeyDer)
        EC.Curve.P256
    } catch (e: Exception) {
        try {
            ecdh.publicKeyDecoder(EC.Curve.P384).decodeFromByteArray(EC.PublicKey.Format.DER, publicKeyDer)
            EC.Curve.P384
        } catch (e: Exception) {
            throw IllegalArgumentException("Unsupported ECC curve")
        }
    }

    // Decode keys
    val privateKey = ecdh.privateKeyDecoder(curve).decodeFromByteArray(EC.PrivateKey.Format.DER.Generic, privateKeyDer)
    val publicKey = ecdh.publicKeyDecoder(curve).decodeFromByteArray(EC.PublicKey.Format.DER, publicKeyDer)

    // Perform ECDH
    val sharedSecretGenerator = privateKey.sharedSecretGenerator()
    return sharedSecretGenerator.generateSharedSecretToByteArray(publicKey)
}
