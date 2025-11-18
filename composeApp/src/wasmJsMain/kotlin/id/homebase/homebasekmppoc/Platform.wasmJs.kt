package id.homebase.homebasekmppoc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope

class WasmPlatform : Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

actual fun isAndroid(): Boolean = false

actual fun getRedirectScheme(): String = "https"

actual fun getRedirectUri(clientId: String): String {
    // For web, redirect back to the same origin
    return window.location.origin
}

actual fun getEccKeySize(): id.homebase.homebasekmppoc.crypto.EccKeySize {
    // Use P-384 like other platforms
    return id.homebase.homebasekmppoc.crypto.EccKeySize.P384
}

actual fun launchCustomTabs(url: String, scope: CoroutineScope) {
    // Navigate in same window so callback can be detected
    println("launchCustomTabs called with URL: $url")

    window.location.href = url
    println("window.location.href set successfully")
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
