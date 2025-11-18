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
import kotlinx.coroutines.CoroutineScope
import java.awt.Desktop
import java.net.URI

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun isAndroid(): Boolean = false

actual fun launchCustomTabs(url: String, scope: CoroutineScope) {
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        } else {
            println("Desktop browsing not supported")
        }
    } catch (e: Exception) {
        println("Error launching browser: ${e.message}")
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
