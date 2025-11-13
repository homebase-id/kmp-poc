package id.homebase.homebasekmppoc.crypto

import java.math.BigInteger
import java.security.*
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.*
import javax.crypto.KeyAgreement

/**
 * Android implementation of JWK to DER conversion
 */
internal actual fun platformJwkToDer(x: ByteArray, y: ByteArray, keySize: EccKeySize): ByteArray {
    val curveName = when (keySize) {
        EccKeySize.P256 -> "secp256r1"
        EccKeySize.P384 -> "secp384r1"
    }

    // Get the EC parameter spec for the curve
    val parameterSpec = AlgorithmParameters.getInstance("EC").apply {
        init(ECGenParameterSpec(curveName))
    }.getParameterSpec(ECParameterSpec::class.java)

    // Create EC point from coordinates
    val point = ECPoint(BigInteger(1, x), BigInteger(1, y))

    // Create public key
    val keySpec = ECPublicKeySpec(point, parameterSpec)
    val keyFactory = KeyFactory.getInstance("EC")
    val publicKey = keyFactory.generatePublic(keySpec) as ECPublicKey

    return publicKey.encoded
}

/**
 * Android implementation of DER to JWK coordinates extraction
 */
internal actual fun platformDerToJwkCoordinates(derKey: ByteArray): Pair<ByteArray, ByteArray> {
    val keyFactory = KeyFactory.getInstance("EC")
    val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(derKey)) as ECPublicKey

    val point = publicKey.w
    val x = point.affineX.toByteArray().let {
        // Remove leading zero byte if present (for positive BigInteger)
        if (it[0] == 0.toByte() && it.size > 1) it.copyOfRange(1, it.size) else it
    }
    val y = point.affineY.toByteArray().let {
        // Remove leading zero byte if present (for positive BigInteger)
        if (it[0] == 0.toByte() && it.size > 1) it.copyOfRange(1, it.size) else it
    }

    return Pair(x, y)
}

/**
 * Android implementation of curve detection from DER key
 */
internal actual fun platformGetCurveFromKey(derKey: ByteArray): EccKeySize {
    val keyFactory = KeyFactory.getInstance("EC")
    val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(derKey)) as ECPublicKey

    val params = publicKey.params
    val fieldSize = params.curve.field.fieldSize

    return when (fieldSize) {
        256 -> EccKeySize.P256
        384 -> EccKeySize.P384
        else -> throw IllegalArgumentException("Unsupported ECC key size with bit length: $fieldSize")
    }
}

/**
 * Android implementation of ECC key pair generation
 */
internal actual fun platformGenerateEccKeyPair(keySize: EccKeySize): Pair<ByteArray, ByteArray> {
    val curveName = when (keySize) {
        EccKeySize.P256 -> "secp256r1"
        EccKeySize.P384 -> "secp384r1"
    }

    val keyPairGenerator = KeyPairGenerator.getInstance("EC")
    keyPairGenerator.initialize(ECGenParameterSpec(curveName), SecureRandom())

    val keyPair = keyPairGenerator.generateKeyPair()

    val privateKeyDer = keyPair.private.encoded
    val publicKeyDer = keyPair.public.encoded

    return Pair(privateKeyDer, publicKeyDer)
}

/**
 * Android implementation of ECDH key agreement
 */
internal actual fun platformEcdhKeyAgreement(privateKeyDer: ByteArray, publicKeyDer: ByteArray): ByteArray {
    val keyFactory = KeyFactory.getInstance("EC")

    // Parse private key
    val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateKeyDer)) as ECPrivateKey

    // Parse public key
    val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicKeyDer)) as ECPublicKey

    // Perform ECDH
    val keyAgreement = KeyAgreement.getInstance("ECDH")
    keyAgreement.init(privateKey)
    keyAgreement.doPhase(publicKey, true)

    return keyAgreement.generateSecret()
}
