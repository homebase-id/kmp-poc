package id.homebase.homebasekmppoc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import java.awt.Desktop
import java.net.URI

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun isAndroid(): Boolean = false

actual fun getRedirectScheme(): String = "http"

actual fun getRedirectUri(clientId: String): String {
    // Get the current port if server is already running
    var port = LocalCallbackServer.getPort()

    // If not running, find an available port for when we start
    if (port == 0) {
        port = LocalCallbackServer.findAvailablePort()
        if (port < 0) {
            throw IllegalStateException("No available ports found for OAuth callback server")
        }
    }

    return "http://localhost:$port/authorization-code-callback"
}

actual fun getEccKeySize(): id.homebase.homebasekmppoc.crypto.EccKeySize {
    // Desktop uses P-384
    return id.homebase.homebasekmppoc.crypto.EccKeySize.P384
}

actual fun launchCustomTabs(url: String, scope: CoroutineScope) {
    try {
        // Start the local callback server before opening the browser
        if (!LocalCallbackServer.isRunning()) {
            // Extract the port from the redirect URI in the URL to ensure consistency
            val portMatch = Regex("localhost%3A(\\d+)").find(url)
            val preferredPort = portMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

            val actualPort = LocalCallbackServer.start(scope, preferredPort)
            if (actualPort < 0) {
                showMessage("Error", "Failed to start OAuth callback server. No available ports found.")
                return
            }

            if (preferredPort > 0 && actualPort != preferredPort) {
                Logger.w("Platform.desktop") { "Server started on port $actualPort instead of preferred $preferredPort" }
            }
        }

        // Open the authorization URL in the system browser
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        } else {
            showMessage("Error", "Desktop browsing is not supported on this system")
        }
    } catch (e: Exception) {
        showMessage("Error", "Failed to launch browser: ${e.message}")
    }
}

private var dialogState: Pair<String, String>? by mutableStateOf(null)

actual fun showMessage(title: String, message: String) {
    dialogState = Pair(title, message)
}

@Composable
fun MessageDialogHandler() {
    val currentDialog = dialogState
    if (currentDialog != null) {
        AlertDialog(
            onDismissRequest = { dialogState = null },
            title = { Text(currentDialog.first) },
            text = { Text(currentDialog.second) },
            confirmButton = {
                TextButton(onClick = { dialogState = null }) {
                    Text("OK")
                }
            }
        )
    }
}
