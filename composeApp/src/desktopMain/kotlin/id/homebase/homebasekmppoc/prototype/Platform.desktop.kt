package id.homebase.homebasekmppoc.prototype

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun isAndroid(): Boolean = false

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
                confirmButton = { TextButton(onClick = { dialogState = null }) { Text("OK") } }
        )
    }
}
