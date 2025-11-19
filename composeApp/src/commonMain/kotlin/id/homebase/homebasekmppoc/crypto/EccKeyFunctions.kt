package id.homebase.homebasekmppoc.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.EC
import dev.whyoleg.cryptography.algorithms.ECDH
import id.homebase.homebasekmppoc.core.SensitiveByteArray
import id.homebase.homebasekmppoc.core.UnixTimeUtc
import id.homebase.homebasekmppoc.core.toSensitiveByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Serializable ECC public key data
 */
@Serializable
data class EccPublicKey(
    val publicKeyDer: ByteArray,
    val keySize: EccKeySize,
    val crc32c: UInt,
    val expiration: UnixTimeUtc
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as EccPublicKey
        return publicKeyDer.contentEquals(other.publicKeyDer) &&
                keySize == other.keySize &&
                crc32c == other.crc32c &&
                expiration == other.expiration
    }

    override fun hashCode(): Int {
        var result = publicKeyDer.contentHashCode()
        result = 31 * result + keySize.hashCode()
        result = 31 * result + crc32c.hashCode()
        result = 31 * result + expiration.hashCode()
        return result
    }
}

/**
 * Serializable ECC private key data (encrypted)
 */
@Serializable
data class EccPrivateKey(
    val encryptedKey: ByteArray,
    val iv: ByteArray,
    val keyHash: ByteArray,
    val createdTimeStamp: UnixTimeUtc
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as EccPrivateKey
        return encryptedKey.contentEquals(other.encryptedKey) &&
                iv.contentEquals(other.iv) &&
                keyHash.contentEquals(other.keyHash) &&
                createdTimeStamp == other.createdTimeStamp
    }

    override fun hashCode(): Int {
        var result = encryptedKey.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + keyHash.contentHashCode()
        result = 31 * result + createdTimeStamp.hashCode()
        return result
    }
}

/**
 * Serializable ECC key pair (public + private)
 */
@Serializable
data class EccKeyPair(
    val publicKey: EccPublicKey,
    val privateKey: EccPrivateKey
)

// ============================================================================
// Key Generation
// ============================================================================

/**
 * Generate a new ECC key pair
 *
 * @param password Password to encrypt the private key
 * @param keySize Curve size (P-256 or P-384)
 * @param expirationHours Hours until key expires
 * @return Generated key pair
 */
suspend fun generateEccKeyPair(
    password: SensitiveByteArray,
    keySize: EccKeySize,
    expirationHours: Int
): EccKeyPair {
    val createdAt = UnixTimeUtc.now()
    val expiration = createdAt.addHours(expirationHours.toLong())

    // Generate key pair using platform crypto
    val (privateKeyDer, publicKeyDer) = generatePlatformEccKeyPair(keySize)

    // Encrypt private key
    val iv = ByteArrayUtil.getRndByteArray(16)
    val keyHash = ByteArrayUtil.reduceSha256Hash(password.getKey())
    val encryptedKey = AesCbc.encrypt(privateKeyDer, keyHash, iv)

    // Calculate CRC for public key
    val crc32c = Crc32c.calculateCrc32c(0u, publicKeyDer)

    return EccKeyPair(
        publicKey = EccPublicKey(
            publicKeyDer = publicKeyDer,
            keySize = keySize,
            crc32c = crc32c,
            expiration = expiration
        ),
        privateKey = EccPrivateKey(
            encryptedKey = encryptedKey,
            iv = iv,
            keyHash = keyHash,
            createdTimeStamp = createdAt
        )
    )
}

// ============================================================================
// JWK Conversion
// ============================================================================

/**
 * Convert public key to JWK (JSON Web Key) format
 *
 * @param publicKey Public key to convert
 * @return JWK as JSON string
 */
suspend fun publicKeyToJwk(publicKey: EccPublicKey): String {
    val (x, y) = derToJwkCoordinates(publicKey.publicKeyDer, publicKey.keySize)

    val expectedBytes = if (publicKey.keySize == EccKeySize.P384) 48 else 32
    val xPadded = ensureLength(x, expectedBytes)
    val yPadded = ensureLength(y, expectedBytes)

    val curveName = if (publicKey.keySize == EccKeySize.P384) "P-384" else "P-256"

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
 *
 * @param publicKey Public key to convert
 * @return Base64url-encoded JWK
 */
suspend fun publicKeyToJwkBase64Url(publicKey: EccPublicKey): String {
    return Base64UrlEncoder.encode(publicKeyToJwk(publicKey))
}

/**
 * Create public key from JWK (JSON Web Key) format
 *
 * @param jwk JWK as JSON string
 * @param expirationHours Hours until key expires (default 1)
 * @return Public key
 */
suspend fun publicKeyFromJwk(jwk: String, expirationHours: Int = 1): EccPublicKey {
    val jwkMap = Json.decodeFromString<Map<String, String>>(jwk)

    require(jwkMap["kty"] == "EC") { "Invalid key type, kty must be EC" }

    val curveName = jwkMap["crv"] ?: throw IllegalArgumentException("Missing crv field")
    require(curveName == "P-384" || curveName == "P-256") {
        "Invalid curve, crv must be P-384 OR P-256"
    }

    val x = Base64UrlEncoder.decode(jwkMap["x"] ?: throw IllegalArgumentException("Missing x coordinate"))
    val y = Base64UrlEncoder.decode(jwkMap["y"] ?: throw IllegalArgumentException("Missing y coordinate"))

    val keySize = if (curveName == "P-384") EccKeySize.P384 else EccKeySize.P256
    val derEncodedPublicKey = jwkToDer(x, y, keySize)

    return EccPublicKey(
        publicKeyDer = derEncodedPublicKey,
        keySize = keySize,
        crc32c = Crc32c.calculateCrc32c(0u, derEncodedPublicKey),
        expiration = UnixTimeUtc.now().addHours(expirationHours.toLong())
    )
}

/**
 * Create public key from base64url-encoded JWK
 *
 * @param jwkBase64Url Base64url-encoded JWK
 * @param expirationHours Hours until key expires (default 1)
 * @return Public key
 */
suspend fun publicKeyFromJwkBase64Url(jwkBase64Url: String, expirationHours: Int = 1): EccPublicKey {
    return publicKeyFromJwk(Base64UrlEncoder.decodeString(jwkBase64Url), expirationHours)
}

// ============================================================================
// Private Key Operations
// ============================================================================

/**
 * Decrypt a private key
 *
 * @param privateKey Encrypted private key
 * @param password Password to decrypt
 * @return Decrypted private key bytes (DER format)
 */
suspend fun decryptPrivateKey(privateKey: EccPrivateKey, password: SensitiveByteArray): ByteArray {
    // Verify password
    val keyHash = ByteArrayUtil.reduceSha256Hash(password.getKey())
    if (!ByteArrayUtil.equiByteArrayCompare(privateKey.keyHash, keyHash)) {
        throw IllegalStateException("Incorrect password")
    }

    // Decrypt private key using derived key
    return AesCbc.decrypt(privateKey.encryptedKey, keyHash, privateKey.iv)
}

// ============================================================================
// ECDH Key Agreement
// ============================================================================

/**
 * Perform ECDH key agreement to derive a shared secret
 *
 * @param keyPair Local key pair
 * @param password Password to decrypt local private key
 * @param remotePublicKey Remote party's public key
 * @param salt Random salt for HKDF (must be at least 16 bytes)
 * @return Derived symmetric key (16 bytes)
 */
suspend fun performEcdhKeyAgreement(
    keyPair: EccKeyPair,
    password: SensitiveByteArray,
    remotePublicKey: EccPublicKey,
    salt: ByteArray
): SensitiveByteArray {
    require(salt.size >= 16) { "Salt must be at least 16 bytes" }

    // Decrypt local private key
    val privateKeyDer = decryptPrivateKey(keyPair.privateKey, password)

    // Perform ECDH
    val sharedSecret = performPlatformEcdhKeyAgreement(privateKeyDer, remotePublicKey.publicKeyDer)

    // Apply HKDF to derive symmetric key
    val derivedKey = HashUtil.hkdf(sharedSecret, salt, 16)

    return derivedKey.toSensitiveByteArray()
}

// ============================================================================
// Internal Helper Functions
// ============================================================================

/**
 * Ensure byte array is the specified length (pad with zeros at the beginning if needed)
 */
private fun ensureLength(bytes: ByteArray, length: Int): ByteArray {
    if (bytes.size >= length) return bytes

    val padded = ByteArray(length)
    bytes.copyInto(padded, length - bytes.size)
    return padded
}

/**
 * Convert JWK coordinates to DER-encoded public key
 */
@OptIn(DelicateCryptographyApi::class)
private suspend fun jwkToDer(x: ByteArray, y: ByteArray, keySize: EccKeySize): ByteArray {
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
 * Extract JWK coordinates from DER-encoded public key
 */
@OptIn(DelicateCryptographyApi::class)
private suspend fun derToJwkCoordinates(derKey: ByteArray, keySize: EccKeySize): Pair<ByteArray, ByteArray> {
    val crypto = CryptographyProvider.Default
    val ecdh = crypto.get(ECDH)

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
 * Generate an ECC key pair using platform crypto
 * Returns (privateKeyDer, publicKeyDer)
 */
@OptIn(DelicateCryptographyApi::class)
private suspend fun generatePlatformEccKeyPair(keySize: EccKeySize): Pair<ByteArray, ByteArray> {
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
 * Perform ECDH key agreement using platform crypto
 * Returns the raw shared secret (before HKDF)
 */
@OptIn(DelicateCryptographyApi::class)
private suspend fun performPlatformEcdhKeyAgreement(privateKeyDer: ByteArray, publicKeyDer: ByteArray): ByteArray {
    val crypto = CryptographyProvider.Default
    val ecdh = crypto.get(ECDH)

    // Detect curve from public key
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
