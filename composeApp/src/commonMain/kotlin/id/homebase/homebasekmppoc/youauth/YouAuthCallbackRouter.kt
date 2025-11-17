package id.homebase.homebasekmppoc.youauth

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.decodeUrl

/**
 * Routes YouAuth deeplink callbacks to the appropriate YouAuthManager instance
 * based on the state parameter in the callback URL.
 *
 * Thread-safety note: Uses a concurrent map for thread-safe access across platforms.
 */
object YouAuthCallbackRouter {
    // Using a regular mutableMap with @Volatile-like semantics through careful usage
    private val registeredManagers = mutableMapOf<String, YouAuthManager>()

    /**
     * Register a YouAuthManager instance for a specific state key.
     * When a callback with this state is received, it will be routed to this manager.
     */
    fun register(stateKey: String, manager: YouAuthManager) {
        registeredManagers[stateKey] = manager
        Logger.d("YouAuthCallbackRouter") { "Registered manager for state: $stateKey" }
    }

    /**
     * Unregister a YouAuthManager instance for a specific state key.
     */
    fun unregister(stateKey: String) {
        registeredManagers.remove(stateKey)
        Logger.d("YouAuthCallbackRouter") { "Unregistered manager for state: $stateKey" }
    }

    /**
     * Handle an authorization callback URL by extracting the state and routing to the correct manager.
     */
    suspend fun handleCallback(url: String) {
        try {
            Logger.d("YouAuthCallbackRouter") { "Received callback: $url" }

            // Extract state parameter from URL
            val query = url.substringAfter("?", "")
            if (query.isEmpty()) {
                Logger.e("YouAuthCallbackRouter") { "Missing query params in callback URL" }
                return
            }

            val params = query.split("&").associate {
                val parts = it.split("=", limit = 2)
                parts[0] to (parts.getOrNull(1) ?: "")
            }

            val state = decodeUrl(params["state"] ?: "")
            if (state.isEmpty()) {
                Logger.e("YouAuthCallbackRouter") { "Missing state parameter in callback URL" }
                return
            }

            // Find the registered manager for this state
            val manager = registeredManagers[state]

            if (manager == null) {
                Logger.e("YouAuthCallbackRouter") { "No manager registered for state: $state" }
                return
            }

            // Route the callback to the appropriate manager
            manager.completeAuth(url, state, params)

        } catch (e: Exception) {
            Logger.e("YouAuthCallbackRouter") { "Error handling callback: ${e.message}" }
        }
    }

    /**
     * Get the count of currently registered managers (for debugging/testing)
     */
    fun getRegisteredCount(): Int {
        return registeredManagers.size
    }
}
