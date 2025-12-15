package id.homebase.homebasekmppoc.lib.browser

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.prototype.lib.youauth.LocalCallbackServer
import java.awt.Desktop
import java.net.URI
import kotlinx.coroutines.CoroutineScope

/** Desktop implementation of BrowserLauncher using system browser and LocalCallbackServer. */
actual object BrowserLauncher {
    private const val TAG = "BrowserLauncher.desktop"

    actual fun launchAuthBrowser(url: String, scope: CoroutineScope) {
        try {
            // Start the local callback server before opening the browser
            if (!LocalCallbackServer.isRunning()) {
                // Extract the port from the redirect URI in the URL to ensure consistency
                val portMatch = Regex("localhost%3A(\\d+)").find(url)
                val preferredPort = portMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

                val actualPort = LocalCallbackServer.start(scope, preferredPort)
                if (actualPort < 0) {
                    Logger.e(TAG) {
                        "Failed to start OAuth callback server. No available ports found."
                    }
                    return
                }

                if (preferredPort > 0 && actualPort != preferredPort) {
                    Logger.w(TAG) {
                        "Server started on port $actualPort instead of preferred $preferredPort"
                    }
                }
            }

            // Open the authorization URL in the system browser
            if (Desktop.isDesktopSupported() &&
                            Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
            ) {
                Desktop.getDesktop().browse(URI(url))
            } else {
                Logger.e(TAG) { "Desktop browsing is not supported on this system" }
            }
        } catch (e: Exception) {
            Logger.e(TAG, e) { "Failed to launch browser: ${e.message}" }
        }
    }
}
