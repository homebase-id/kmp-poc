package id.homebase.homebasekmppoc.crypto

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
    var expiration: UnixTimeUtc = UnixTimeUtc.ZeroTime  // Expiration time
) {
    companion object {
        val eccSignatureAlgorithmNames = arrayOf("SHA256withECDSA", "SHA384withECDSA")
        val eccKeyTypeNames = arrayOf("P-256", "P-384")
        val eccCurveIdentifiers = arrayOf("secp256r1", "secp384r1")

        /**
         * Create EccPublicKeyData from JWK (JSON Web Key) format
         */
        fun fromJwkPublicKey(jwk: String, hours: Int = 1): EccPublicKeyData {
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
                expiration = UnixTimeUtc.now().addHours(hours.toLong())
            )
        }

        /**
         * Create EccPublicKeyData from base64url-encoded JWK
         */
        fun fromJwkBase64UrlPublicKey(jwkBase64Url: String, hours: Int = 1): EccPublicKeyData {
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
    protected fun getCurveEnum(): EccKeySize {
        return platformGetCurveFromKey(publicKey)
    }

    /**
     * Convert public key to JWK format
     */
    fun publicKeyJwk(): String {
        val (x, y) = platformDerToJwkCoordinates(publicKey)
        val curveSize = getCurveEnum()

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
    fun publicKeyJwkBase64Url(): String {
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
 * Platform-specific function to convert JWK coordinates to DER-encoded public key
 */
internal expect fun platformJwkToDer(x: ByteArray, y: ByteArray, keySize: EccKeySize): ByteArray

/**
 * Platform-specific function to extract JWK coordinates from DER-encoded public key
 */
internal expect fun platformDerToJwkCoordinates(derKey: ByteArray): Pair<ByteArray, ByteArray>

/**
 * Platform-specific function to get the curve type from a DER-encoded key
 */
internal expect fun platformGetCurveFromKey(derKey: ByteArray): EccKeySize

/**
 * Platform-specific function to generate an ECC key pair
 * Returns (privateKeyDer, publicKeyDer)
 */
internal expect fun platformGenerateEccKeyPair(keySize: EccKeySize): Pair<ByteArray, ByteArray>

/**
 * Platform-specific function to perform ECDH key agreement
 * Returns the raw shared secret (before HKDF)
 */
internal expect fun platformEcdhKeyAgreement(privateKeyDer: ByteArray, publicKeyDer: ByteArray): ByteArray
