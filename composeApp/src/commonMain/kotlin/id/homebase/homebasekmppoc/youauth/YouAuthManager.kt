package id.homebase.homebasekmppoc.youauth

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.crypto.AesCbc
import id.homebase.homebasekmppoc.crypto.EccFullKeyData
import id.homebase.homebasekmppoc.crypto.EccKeySize
import id.homebase.homebasekmppoc.crypto.EccPublicKeyData
import id.homebase.homebasekmppoc.crypto.HashUtil
import id.homebase.homebasekmppoc.core.SensitiveByteArray
import id.homebase.homebasekmppoc.decodeUrl
import id.homebase.homebasekmppoc.generateUuidBytes
import id.homebase.homebasekmppoc.generateUuidString
import id.homebase.homebasekmppoc.getEccKeySize
import id.homebase.homebasekmppoc.getRedirectUri
import id.homebase.homebasekmppoc.http.UriBuilder
import id.homebase.homebasekmppoc.http.createHttpClient
import id.homebase.homebasekmppoc.launchCustomTabs
import id.homebase.homebasekmppoc.serialization.OdinSystemSerializer
import id.homebase.homebasekmppoc.toBase64
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.io.encoding.Base64

/**
 * Authentication state for the YouAuth flow
 */
sealed class YouAuthState {
    /** User is not authenticated */
    data object Unauthenticated : YouAuthState()

    /** Authentication flow is in progress */
    data object Authenticating : YouAuthState()

    /** User is authenticated with valid tokens */
    data class Authenticated(
        val identity: String,
        val clientAuthToken: String,  // Base64 encoded
        val sharedSecret: String      // Base64 encoded
    ) : YouAuthState()

    /** Authentication failed with an error */
    data class Error(val message: String) : YouAuthState()
}

/**
 * Internal state for flow
 */
private data class AuthCodeFlowState(
    val identity: String,
    val privateKey: SensitiveByteArray,
    val keyPair: EccFullKeyData
)

/**
 * Manages YouAuth authentication state and flow.
 * Create instances to manage independent authentication flows.
 */
class YouAuthManager {
    private val _youAuthState = MutableStateFlow<YouAuthState>(YouAuthState.Unauthenticated)
    val youAuthState: StateFlow<YouAuthState> = _youAuthState.asStateFlow()

    private var authCodeFlowState: AuthCodeFlowState? = null

    /**
     * Start the authentication flow
     * Returns the authorization URL to open in browser
     */
    suspend fun authorize(
        identity: String,
        scope: CoroutineScope,
        appParameters: YouAuthAppParameters? = null) {

        // Sanity
        if (_youAuthState.value == YouAuthState.Authenticating || _youAuthState.value is YouAuthState.Authenticated) {
            Logger.e("YouAuthManager") { "Already authenticated" }
            return
        }

        _youAuthState.value = YouAuthState.Authenticating
        try {

            //
            // YouAuth [010]
            //

            val privateKey = SensitiveByteArray(generateUuidBytes())
            val keyPair = EccFullKeyData.create(privateKey, getEccKeySize(), 1)

            //
            // YouAuth [030]
            //

            val state = generateUuidString()
            authCodeFlowState = AuthCodeFlowState(identity, privateKey, keyPair)

            // Register this instance with the router to receive callbacks for this state
            YouAuthCallbackRouter.register(state, this)

            // Domain or app specifics
            var clientId = "thirdparty.dotyou.cloud"
            var clientType = ClientType.domain
            var permissionRequest = ""
            if (appParameters != null) {
                clientId =  appParameters.appId
                clientType = ClientType.app
                permissionRequest = OdinSystemSerializer.serialize(appParameters)
            }

            // Build platform-specific redirect URI
            val redirectUri = getRedirectUri(clientId)

            val payload = YouAuthAuthorizeRequest(
                clientId = clientId,
                clientInfo = "",
                clientType = clientType,
                permissionRequest = permissionRequest,
                publicKey = keyPair.publicKeyJwkBase64Url(),
                redirectUri = redirectUri,
                state = state,
            )

            val authorizeUrl = UriBuilder("https://$identity/api/owner/v1/youauth/authorize")
                .apply {
                    query = payload.toQueryString()
                }
                .toString()

            launchCustomTabs(authorizeUrl, scope)

        } catch (e: Exception) {
            Logger.e("YouAuthManager") { "Error starting authorization code flow: ${e.message}" }
            _youAuthState.value = YouAuthState.Error(e.message ?: "Unknown error")
        }
    }

    //

    suspend fun completeAuth(url: String, state: String, queryParams: Map<String, String>) {

        // Sanity
        if (authCodeFlowState == null) {
            Logger.e("YouAuthManager") { "No pending auth code flow state" }
            _youAuthState.value = YouAuthState.Error("No pending auth code flow")
            return
        }

        try {
            if (!url.contains("/authorization-code-callback")) {
                throw Exception("Missing /authorization-code-callback")
            }

            val identity = decodeUrl(queryParams["identity"] ?: "")
            if (identity == "") {
                throw Exception("Missing query param: identity")
            }

            val publicKey = decodeUrl(queryParams["public_key"] ?: "")
            if (publicKey == "") {
                throw Exception("Missing query param: public_key")
            }

            val salt = decodeUrl(queryParams["salt"] ?: "")
            if (salt == "") {
                throw Exception("Missing query param: salt")
            }

            //
            // YouAuth [090]
            //

            val privateKey = authCodeFlowState!!.privateKey
            val keyPair = authCodeFlowState!!.keyPair
            val remotePublicKey = publicKey
            val remoteSalt = Base64.decode(salt)

            val remotePublicKeyJwk = EccPublicKeyData.fromJwkBase64UrlPublicKey(remotePublicKey)
            val exchangeSecret = keyPair.getEcdhSharedSecret(privateKey, remotePublicKeyJwk, remoteSalt)
            val exchangeSecretDigest = HashUtil.sha256(exchangeSecret.getKey()).toBase64()

            //
            // YouAuth [100]
            //
            // SEB:TODO this will sometimes return 404. Investigate if exchangeSecretDigest
            // can sometimes have incompatible encoding across platforms.
            //
            Logger.d("YouAuth") { "exchangeSecretDigest: $exchangeSecretDigest" }

            val uri = UriBuilder("https://${authCodeFlowState!!.identity}/api/owner/v1/youauth/token")
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
            // Post YouAuth [400] - Store authentication state
            //

            val identityValue = authCodeFlowState!!.identity
            val catValue = Base64.encode(clientAuthToken)
            val sharedSecretValue = Base64.encode(sharedSecret)

            // Update state to authenticated
            _youAuthState.value = YouAuthState.Authenticated(
                identity = identityValue,
                clientAuthToken = catValue,
                sharedSecret = sharedSecretValue
            )

            Logger.i("YouAuthManager") { "Authentication completed successfully for $identityValue" }

        } catch (e: Exception) {
            Logger.e("YouAuthManager") { "Error completing auth: ${e.message}" }
            _youAuthState.value = YouAuthState.Error(e.message ?: "Unknown error")
        } finally {
            authCodeFlowState = null
            YouAuthCallbackRouter.unregister(state)
        }
    }

    //

    /**
     * Logout and clear authentication state
     */
    fun logout() {
        _youAuthState.value = YouAuthState.Unauthenticated
        Logger.i("YouAuthManager") { "User logged out" }
    }

    //

}
