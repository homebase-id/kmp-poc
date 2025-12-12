package id.homebase.homebasekmppoc.lib.youAuth

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.lib.browser.BrowserLauncher
import id.homebase.homebasekmppoc.lib.browser.RedirectConfig
import id.homebase.homebasekmppoc.prototype.decodeUrl
import id.homebase.homebasekmppoc.prototype.generateUuidBytes
import id.homebase.homebasekmppoc.prototype.generateUuidString
import id.homebase.homebasekmppoc.prototype.lib.core.SecureByteArray
import id.homebase.homebasekmppoc.prototype.lib.crypto.EccKeyPair
import id.homebase.homebasekmppoc.prototype.lib.crypto.EccKeySize
import id.homebase.homebasekmppoc.prototype.lib.crypto.generateEccKeyPair
import id.homebase.homebasekmppoc.prototype.lib.crypto.publicKeyToJwkBase64Url
import id.homebase.homebasekmppoc.prototype.lib.http.UriBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.io.encoding.Base64

/** Authentication state for the YouAuth flow. */
sealed class YouAuthState {
    /** User is not authenticated */
    data object Unauthenticated : YouAuthState()

    /** Authentication flow is in progress */
    data object Authenticating : YouAuthState()

    /** User is authenticated with valid tokens */
    data class Authenticated(
            val identity: String,
            val clientAuthToken: String,
            val sharedSecret: String
    ) : YouAuthState()

    /** Authentication failed with an error */
    data class Error(val message: String) : YouAuthState()
}

/** Internal state for the auth code flow. */
private data class AuthCodeFlowState(
        val identity: String,
        val password: SecureByteArray,
        val keyPair: EccKeyPair
)

/**
 * Manages the complete YouAuth authentication flow with state management. Uses YouAuthProvider for
 * HTTP operations.
 *
 * This is the recommended entry point for UI components like LoginViewModel.
 */
class YouAuthFlowManager {
    private val _authState = MutableStateFlow<YouAuthState>(YouAuthState.Unauthenticated)
    val authState: StateFlow<YouAuthState> = _authState.asStateFlow()

    private var authCodeFlowState: AuthCodeFlowState? = null
    private val registeredStates = mutableMapOf<String, YouAuthFlowManager>()

    companion object {
        private const val TAG = "YouAuthFlowManager"

        // Global registry for callback routing
        private val callbackRegistry = mutableMapOf<String, YouAuthFlowManager>()

        /** Handle an authorization callback URL. */
        suspend fun handleCallback(url: String) {
            try {
                Logger.d(TAG) { "Received callback: $url" }

                val query = url.substringAfter("?", "")
                if (query.isEmpty()) {
                    Logger.e(TAG) { "Missing query params in callback URL" }
                    return
                }

                val params =
                        query.split("&").associate {
                            val parts = it.split("=", limit = 2)
                            parts[0] to (parts.getOrNull(1) ?: "")
                        }

                val state = decodeUrl(params["state"] ?: "")
                if (state.isEmpty()) {
                    Logger.e(TAG) { "Missing state parameter in callback URL" }
                    return
                }

                val manager = callbackRegistry[state]
                if (manager == null) {
                    Logger.e(TAG) { "No manager registered for state: $state" }
                    return
                }

                manager.completeAuth(url, state, params)
            } catch (e: Exception) {
                Logger.e(TAG, e) { "Error handling callback" }
            }
        }
    }

    /** Check if there are stored credentials and restore session. */
    fun restoreSession(): Boolean {
        if (OdinClientFactory.hasStoredCredentials()) {
            val client = OdinClientFactory.createFromStorage()
            if (client != null) {
                val identity = client.getHostIdentity()
                // We don't have the raw tokens here, but we know we're authenticated
                _authState.value =
                        YouAuthState.Authenticated(
                                identity = identity,
                                clientAuthToken = "", // Not needed since OdinClient is configured
                                sharedSecret = ""
                        )
                Logger.i(TAG) { "Session restored for $identity" }
                return true
            }
        }
        return false
    }

    /**
     * Start the authentication flow.
     *
     * @param identity The user's identity (e.g., "user.homebase.id")
     * @param scope CoroutineScope for launching browser
     * @param appId Application ID
     * @param appName Application name
     * @param drives List of drive access requests
     */
    suspend fun authorize(
            identity: String,
            scope: CoroutineScope,
            appId: String,
            appName: String,
            drives: List<TargetDriveAccessRequest> = emptyList(),
            permissions: List<Int>? = null,
            circlePermissions: List<Int>? = null,
            circleDrives: List<TargetDriveAccessRequest>? = null,
            circles: List<String>? = null,
            clientFriendlyName: String? = null
    ) {
        if (_authState.value == YouAuthState.Authenticating ||
                        _authState.value is YouAuthState.Authenticated
        ) {
            Logger.e(TAG) { "Already authenticating or authenticated" }
            return
        }

        _authState.value = YouAuthState.Authenticating
        try {
            // Generate key pair for ECDH
            val password = SecureByteArray(generateUuidBytes())
            val keyPair = generateEccKeyPair(password, EccKeySize.P384, 1)

            // Generate unique state for CSRF protection and callback routing
            val state = generateUuidString()
            authCodeFlowState = AuthCodeFlowState(identity, password, keyPair)

            // Register for callback
            callbackRegistry[state] = this

            // Build redirect URI
            val redirectUri = RedirectConfig.buildRedirectUri(appId)

            // Build permission request
            val permissionRequest =
                    AppAuthorizationParams.create(
                            appName = appName,
                            appId = appId,
                            friendlyName = clientFriendlyName ?: "Homebase KMP App",
                            drives = drives,
                            circleDrives = circleDrives,
                            circles = circles,
                            permissions = permissions,
                            circlePermissions = circlePermissions,
                            returnUrl = redirectUri
                    )

            // Build authorization request
            val authRequest =
                    YouAuthorizationParams(
                            clientId = appId,
                            clientType = ClientType.app,
                            clientInfo = clientFriendlyName ?: "Homebase KMP App",
                            publicKey = publicKeyToJwkBase64Url(keyPair.publicKey),
                            permissionRequest = permissionRequest.toJson(),
                            state = state,
                            redirectUri = redirectUri
                    )

            // Build authorization URL
            val authorizeUrl =
                    UriBuilder("https://$identity/api/owner/v1/youauth/authorize")
                            .apply { query = authRequest.toQueryString() }
                            .toString()

            // Launch browser
            BrowserLauncher.launchAuthBrowser(authorizeUrl, scope)
        } catch (e: Exception) {
            Logger.e(TAG, e) { "Error starting authorization" }
            _authState.value = YouAuthState.Error(e.message ?: "Unknown error")
        }
    }

    /** Complete the authentication flow after browser callback. */
    private suspend fun completeAuth(url: String, state: String, queryParams: Map<String, String>) {
        if (authCodeFlowState == null) {
            Logger.e(TAG) { "No pending auth code flow state" }
            _authState.value = YouAuthState.Error("No pending auth code flow")
            return
        }

        try {
            if (!url.contains("/authorization-code-callback")) {
                throw Exception("Missing /authorization-code-callback")
            }

            val identity = decodeUrl(queryParams["identity"] ?: "")
            if (identity.isEmpty()) throw Exception("Missing query param: identity")

            val publicKey = decodeUrl(queryParams["public_key"] ?: "")
            if (publicKey.isEmpty()) throw Exception("Missing query param: public_key")

            val salt = decodeUrl(queryParams["salt"] ?: "")
            if (salt.isEmpty()) throw Exception("Missing query param: salt")

            // Create unauthenticated client for token exchange
            val unauthClient = OdinClientFactory.createUnauthenticated(authCodeFlowState!!.identity)
            val provider = YouAuthProvider(unauthClient)

            // Finalize authentication
            val result =
                    provider.finalizeAuthentication(
                            identity = identity,
                            keyPair = authCodeFlowState!!.keyPair,
                            password = authCodeFlowState!!.password,
                            publicKey = publicKey,
                            salt = salt
                    )

            // Save credentials
            OdinClientFactory.saveCredentials(
                    identity = result.identity,
                    clientAuthToken = result.clientAuthToken,
                    sharedSecret = Base64.decode(result.sharedSecret)
            )

            // Update state
            _authState.value =
                    YouAuthState.Authenticated(
                            identity = result.identity,
                            clientAuthToken = result.clientAuthToken,
                            sharedSecret = result.sharedSecret
                    )

            Logger.i(TAG) { "Authentication completed successfully for ${result.identity}" }
        } catch (e: Exception) {
            Logger.e(TAG, e) { "Error completing auth" }
            _authState.value = YouAuthState.Error(e.message ?: "Unknown error")
        } finally {
            authCodeFlowState = null
            callbackRegistry.remove(state)
        }
    }

    /** Logout and clear credentials. */
    suspend fun logout() {
        try {
            val client = OdinClientFactory.createFromStorage()
            if (client != null) {
                val provider = YouAuthProvider(client)
                provider.logout()
            }
        } catch (e: Exception) {
            Logger.e(TAG, e) { "Error during logout" }
        }

        OdinClientFactory.clearCredentials()
        _authState.value = YouAuthState.Unauthenticated
        Logger.i(TAG) { "User logged out" }
    }

    /** Check if authentication is in progress. */
    val isAuthenticating: Boolean
        get() = _authState.value == YouAuthState.Authenticating

    /**
     * Cancel the current authentication flow. Call this when the user cancels the browser or
     * navigates away.
     */
    fun cancelAuth() {
        if (_authState.value == YouAuthState.Authenticating) {
            Logger.i(TAG) { "Authentication cancelled by user" }

            // Clean up any pending state
            authCodeFlowState?.let { flowState ->
                // Find and remove from callback registry
                val stateToRemove = callbackRegistry.entries.find { it.value == this }?.key
                stateToRemove?.let { callbackRegistry.remove(it) }
            }
            authCodeFlowState = null

            _authState.value = YouAuthState.Unauthenticated
        }
    }

    /**
     * Called when the app resumes from background. If we were authenticating and come back without
     * a callback, the user likely cancelled.
     *
     * @param delayMs Optional delay to wait for callback before cancelling (default 500ms)
     */
    suspend fun onAppResumed(delayMs: Long = 500) {
        if (_authState.value == YouAuthState.Authenticating) {
            // Wait a short time for callback to potentially arrive
            kotlinx.coroutines.delay(delayMs)

            // If still authenticating, assume user cancelled
            if (_authState.value == YouAuthState.Authenticating) {
                Logger.i(TAG) { "App resumed without auth callback, assuming user cancelled" }
                cancelAuth()
            }
        }
    }
}
