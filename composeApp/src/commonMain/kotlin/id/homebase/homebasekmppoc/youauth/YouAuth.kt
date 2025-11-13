package id.homebase.homebasekmppoc.youauth

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.crypto.EccFullKeyData
import id.homebase.homebasekmppoc.crypto.EccKeySize
import id.homebase.homebasekmppoc.crypto.SensitiveByteArray
import id.homebase.homebasekmppoc.decodeUrl
import id.homebase.homebasekmppoc.generateUuidBytes
import id.homebase.homebasekmppoc.generateUuidString
import id.homebase.homebasekmppoc.showMessage
import kotlin.io.encoding.Base64
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// Global storage for states (not thread-safe in multiplatform context)
// This should really be ctor injected. In this POC it's just a global
val globalStates: MutableMap<String, State> = mutableMapOf()

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
    globalStates[state] = State(identity, privateKey, keyPair)

    var clientId = "thirdparty.dotyou.cloud"
    val payload = YouAuthAuthorizeRequest (
        clientId = clientId,
        clientInfo = "",
        clientType = ClientType.domain,
        permissionRequest = "",
        publicKey = keyPair.publicKeyJwkBase64Url(),
//        redirectUri = "https://$identity/{controllerRoute}/authorization-code-callback",
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
fun handleAuthorizeCallback(url: String) {
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

@OptIn(ExperimentalUuidApi::class)
fun authorizeFromCallback(url: String) {

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
    Logger.i("YouAuth") { "Salt: $salt" }
    val saltyBytes = Base64.Default.decode(salt)

    val stateKey = decodeUrl(params["state"]  ?: "")
    if (stateKey == "") {
        throw Exception("Missing query param: state")
    }
    val stateUuid = Uuid.parse(stateKey!!)
    val state = globalStates[stateKey] ?: throw Exception("State not found in map")

}

//

