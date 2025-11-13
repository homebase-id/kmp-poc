package id.homebase.homebasekmppoc.youauth

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.crypto.EccFullKeyData
import id.homebase.homebasekmppoc.crypto.EccKeySize
import id.homebase.homebasekmppoc.crypto.SensitiveByteArray
import id.homebase.homebasekmppoc.generateUuidBytes
import id.homebase.homebasekmppoc.generateUuidString
import id.homebase.homebasekmppoc.showMessage
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.Uuid

// Global storage for states (not thread-safe in multiplatform context)
// This should really be ctor injected. In this POC it's just a global
val globalStates: MutableMap<String, State> = mutableMapOf()

//

suspend fun buildAuthorizeUrl(identity: String): String {

    //    @OptIn(ExperimentalTime::class)
    //    val currentTimeMillis = Clock.System.now().toEpochMilliseconds()

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
fun handleAuthCallback(url: String) {
    showMessage("Auth Callback", "Received: $url")
    Logger.i("YouAuth") { "Callback: $url" }

    // Parse the callback URL to extract parameters
    // Expected format: youauth://thirdparty.dotyou.cloud/authorization-code-callback?code=...&state=...
////    if (url.contains("/authorization-code-callback")) {
////        val query = url.substringAfter("?", "")
////        if (query.isNotEmpty()) {
////            val params = query.split("&").associate {
////                val parts = it.split("=", limit = 2)
////                parts[0] to (parts.getOrNull(1) ?: "")
////            }
////
////            val code = params["code"]
////            val state = params["state"]
////
////            if (code != null && code.isNotEmpty() && state != null && state.isNotEmpty()) {
////                // Look up the stored state
////                val storedState = globalStates[state]
////                if (storedState != null) {
////                    showMessage("Success", "Authorization code received!\nCode: ${code.take(10)}...\nState: $state")
////                    // TODO: Continue with token exchange using the code and storedState.keyPair
////                } else {
////                    showMessage("Error", "Invalid state parameter - state not found in storage")
////                }
////            } else {
////                showMessage("Error", "Missing code or state parameter in callback")
////            }
//        }
//    }
}