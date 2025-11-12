package id.homebase.homebasekmppoc.youauth

import id.homebase.homebasekmppoc.generateUuidBytes
import id.homebase.homebasekmppoc.generateUuidString
import id.homebase.homebasekmppoc.showMessage
import kotlin.uuid.Uuid

// Global storage for states (not thread-safe in multiplatform context)
// This should really be ctor injected. In this POC it's just a global
//val globalStates: MutableMap<String, State> = mutableMapOf()

//

fun buildAuthorizeUrl(identity: String): String {

    return "TODO"

//    //
//    // YouAuth [010]
//    //
//    val privateKey = SensitiveByteArray(generateUuidBytes())
//    val keyPair = EccFullKeyData(privateKey, EccKeySize.P384, 1)
//
//    //
//    // YouAuth [030]
//    //
//
//    val state = generateUuidString()
//    globalStates[state] = State(identity, privateKey, keyPair)
//
//    var clientId = "thirdparty.dotyou.cloud"
//    val payload = YouAuthAuthorizeRequest (
//        clientId = clientId,
//        clientInfo = "",
//        clientType = ClientType.domain,
//        permissionRequest = "",
//        publicKey = keyPair.publicKeyJwkBase64Url(),
////        redirectUri = "https://$identity/{controllerRoute}/authorization-code-callback",
//        redirectUri = "youauth://$clientId/authorization-code-callback",
//        state = state,
//    )
//
//    val uri = UriBuilder("https://$identity/api/owner/v1/youauth/authorize")
//        .apply {
//            query = payload.toQueryString()
//        }
//        .toString()
//
//    return uri;
}