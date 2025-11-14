package id.homebase.homebasekmppoc.youauth

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.crypto.AesCbc
import id.homebase.homebasekmppoc.crypto.EccFullKeyData
import id.homebase.homebasekmppoc.crypto.EccKeySize
import id.homebase.homebasekmppoc.crypto.EccPublicKeyData
import id.homebase.homebasekmppoc.crypto.HashUtil
import id.homebase.homebasekmppoc.crypto.SensitiveByteArray
import id.homebase.homebasekmppoc.decodeUrl
import id.homebase.homebasekmppoc.generateUuidBytes
import id.homebase.homebasekmppoc.generateUuidString
import id.homebase.homebasekmppoc.launchCustomTabs
import id.homebase.homebasekmppoc.serialization.OdinSystemSerializer
import id.homebase.homebasekmppoc.toBase64
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
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
 * Manages YouAuth authentication state and flow
 */
object YouAuthManager {
    private val _youAuthState = MutableStateFlow<YouAuthState>(YouAuthState.Unauthenticated)
    val youAuthState: StateFlow<YouAuthState> = _youAuthState.asStateFlow()

    private val authCodeFlowStateMap: MutableMap<String, AuthCodeFlowState> = mutableMapOf()

    /**
     * Start the authentication flow
     * Returns the authorization URL to open in browser
     */
    suspend fun authorize(identity: String, scope: CoroutineScope) {
        _youAuthState.value = YouAuthState.Authenticating

        try {

            //
            // YouAuth [010]
            //

            val privateKey = SensitiveByteArray(generateUuidBytes())
            val keyPair = EccFullKeyData.create(privateKey, EccKeySize.P384, 1)

            //
            // YouAuth [030]
            //

            val state = generateUuidString()
            authCodeFlowStateMap[state] = AuthCodeFlowState(identity, privateKey, keyPair)

            val clientId = "thirdparty.dotyou.cloud"
            val payload = YouAuthAuthorizeRequest(
                clientId = clientId,
                clientInfo = "",
                clientType = ClientType.domain,
                permissionRequest = "",
                publicKey = keyPair.publicKeyJwkBase64Url(),
                redirectUri = "youauth://$clientId/authorization-code-callback",
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

    suspend fun handleAuthorizeCallback(url: String) {
        Logger.d("YouAuth") { "Callback: $url" }
        completeAuth(url)
    }

    //

    private suspend fun completeAuth(url: String) {
        try {
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

            val stateKey = decodeUrl(params["state"] ?: "")
            if (stateKey == "") {
                throw Exception("Missing query param: state")
            }
            val authState = authCodeFlowStateMap[stateKey] ?: throw Exception("State not found in map")

            //
            // YouAuth [090]
            //

            val privateKey = authState.privateKey
            val keyPair = authState.keyPair
            val remotePublicKey = publicKey
            val remoteSalt = Base64.decode(salt)

            val remotePublicKeyJwk = EccPublicKeyData.fromJwkBase64UrlPublicKey(remotePublicKey)
            val exchangeSecret = keyPair.getEcdhSharedSecret(privateKey, remotePublicKeyJwk, remoteSalt)
            val exchangeSecretDigest = HashUtil.sha256(exchangeSecret.getKey()).toBase64()

            //
            // YouAuth [100]
            //
            // SEB:NOTE this will sometimes return 404. Investigate if exchangeSecretDigest
            // can sometimes have incompatible encoding across platforms.
            //
            Logger.d("YouAuth") { "exchangeSecretDigest: $exchangeSecretDigest" }

            val uri = UriBuilder("https://${authState.identity}/api/owner/v1/youauth/token")
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

            val identityValue = authState.identity
            val catValue = Base64.encode(clientAuthToken)
            val sharedSecretValue = Base64.encode(sharedSecret)

            // Update state to authenticated
            _youAuthState.value = YouAuthState.Authenticated(
                identity = identityValue,
                clientAuthToken = catValue,
                sharedSecret = sharedSecretValue
            )

            // Clean up temporary state
            authCodeFlowStateMap.remove(stateKey)

            Logger.i("YouAuthManager") { "Authentication completed successfully for $identityValue" }

        } catch (e: Exception) {
            Logger.e("YouAuthManager") { "Error completing auth: ${e.message}" }
            _youAuthState.value = YouAuthState.Error(e.message ?: "Unknown error")
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

    /**
     * Create HTTP client with JSON serialization support
     */
    private fun createHttpClient() = HttpClient {
        install(ContentNegotiation) {
            json(OdinSystemSerializer.json)
        }
    }
}
