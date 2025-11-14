package id.homebase.homebasekmppoc.youauth

import co.touchlab.kermit.Logger
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256
import id.homebase.homebasekmppoc.crypto.AesCbc
import id.homebase.homebasekmppoc.crypto.Base64UrlEncoder
import id.homebase.homebasekmppoc.crypto.EccFullKeyData
import id.homebase.homebasekmppoc.crypto.EccKeySize
import id.homebase.homebasekmppoc.crypto.EccPublicKeyData
import id.homebase.homebasekmppoc.crypto.HashUtil
import id.homebase.homebasekmppoc.crypto.SensitiveByteArray
import id.homebase.homebasekmppoc.decodeUrl
import id.homebase.homebasekmppoc.generateUuidBytes
import id.homebase.homebasekmppoc.generateUuidString
import id.homebase.homebasekmppoc.serialization.OdinSystemSerializer
import id.homebase.homebasekmppoc.showMessage
import id.homebase.homebasekmppoc.toBase64
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64

// Global storage for states (not thread-safe in multiplatform context)
// This should really be ctor injected. In this POC it's just a global
val stateMap: MutableMap<String, State> = mutableMapOf()

//

suspend fun buildAuthorizeUrl(identity: String): String {

    //
    // YouAuth [010]
    //
    val privateKey = SensitiveByteArray(generateUuidBytes())
    val keyPair = EccFullKeyData.create(privateKey, EccKeySize.P384, 1)

    //
    // YouAuth [030]
    //

    val state = generateUuidString()
    stateMap[state] = State(identity, privateKey, keyPair)

    val clientId = "thirdparty.dotyou.cloud"
    val payload = YouAuthAuthorizeRequest (
        clientId = clientId,
        clientInfo = "",
        clientType = ClientType.domain,
        permissionRequest = "",
        publicKey = keyPair.publicKeyJwkBase64Url(),
        redirectUri = "youauth://$clientId/authorization-code-callback",
        state = state,
    )

    val uri = UriBuilder("https://$identity/api/owner/v1/youauth/authorize")
        .apply {
            query = payload.toQueryString()
        }
        .toString()

    return uri;
}

/**
 * Called when the app receives a deeplink redirect after authorization
 */
suspend fun handleAuthorizeCallback(url: String) {
    //showMessage("Auth Callback", url)
    Logger.i("YouAuth") { "Callback: $url" }

    try {
        authorizeFromCallback(url)
        showMessage("Very nice", "Very nice")
    } catch (e: Exception) {
        Logger.e("YouAuth") { "Error: ${e.message}" }
        showMessage("Error", "${e.message}")
    }
}

//

suspend fun authorizeFromCallback(url: String) {

    if (!url.contains("/authorization-code-callback")) {
        throw Exception("Missing /authorization-code-callback")
    }

    val query = url.substringAfter("?", "")
    if (query.isEmpty()) {
        throw Exception("Missing query params")
    }

    val params = query.split("&").associate {
        val parts = it.split("=", limit = 2)
        parts[0] to (parts.getOrNull(1) ?: "")
    }

    val identity = decodeUrl(params["identity"] ?: "")
    if (identity == "") {
        throw Exception("Missing query param: identity")
    }

    val publicKey = decodeUrl(params["public_key"] ?: "")
    if (publicKey == "") {
        throw Exception("Missing query param: public_key")
    }

    val salt = decodeUrl(params["salt"] ?: "")
    if (salt == "") {
        throw Exception("Missing query param: salt")
    }

    val stateKey = decodeUrl(params["state"]  ?: "")
    if (stateKey == "") {
        throw Exception("Missing query param: state")
    }
    val state = stateMap[stateKey] ?: throw Exception("State not found in map")

    //
    // YouAuth [090]
    //

    val privateKey = state.privateKey
    val keyPair = state.keyPair
    val remotePublicKey = publicKey
    val remoteSalt = Base64.decode(salt)

    val remotePublicKeyJwk = EccPublicKeyData.fromJwkBase64UrlPublicKey(remotePublicKey)
    val exchangeSecret = keyPair.getEcdhSharedSecret(privateKey, remotePublicKeyJwk, remoteSalt)
    val exchangeSecretDigest = HashUtil.sha256(exchangeSecret.getKey()).toBase64()

    //
    // YouAuth [100]
    //

    val uri = UriBuilder("https://${state.identity}/api/owner/v1/youauth/token")
    val tokenRequest = YouAuthTokenRequest(exchangeSecretDigest)

    val client = createHttpClient()
    val response = client.post(uri.toString()) {
        contentType(ContentType.Application.Json)
        setBody(tokenRequest)
    }

    if (response.status.value != 200) {
        val responseText = response.body<String>()
        throw Exception("NO! It's a ${response.status.value}: $responseText")
    }

    //
    // YouAuth [150]
    //

    val token = response.body<YouAuthTokenResponse>()

    val sharedSecretCipher = Base64.decode(token.base64SharedSecretCipher)
    val sharedSecretIv = Base64.decode(token.base64SharedSecretIv)
    val sharedSecret = AesCbc.decrypt(sharedSecretCipher, exchangeSecret.getKey(), sharedSecretIv)

    val clientAuthTokenCipher = Base64.decode(token.base64ClientAuthTokenCipher)
    val clientAuthTokenIv = Base64.decode(token.base64ClientAuthTokenIv)
    val clientAuthToken = AesCbc.decrypt(clientAuthTokenCipher, exchangeSecret.getKey(), clientAuthTokenIv)

    //
    // Post YouAuth [400]
    // Store thirdparty cookies
    //

    val identityCookieValue = state.identity
    val catCookieValue = Base64.encode(clientAuthToken)
    val sharedSecretCookieValue = Base64.encode(sharedSecret)


}

//

/**
 * Create HTTP client with JSON serialization support
 */
private fun createHttpClient() = HttpClient {
    install(ContentNegotiation) {
        json(OdinSystemSerializer.json)
    }
}
