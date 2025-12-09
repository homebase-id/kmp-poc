package id.homebase.homebasekmppoc

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import id.homebase.homebasekmppoc.lib.MessageDialogHandler
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseDriverFactory
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager

fun main() = application {
    // Initialize database
    DatabaseManager.initialize(DatabaseDriverFactory())

    Window(
        onCloseRequest = ::exitApplication,
        title = "Odin KMP",
        state = WindowState(size = DpSize(800.dp, 900.dp))
    ) {
        App()
        MessageDialogHandler()
    }
}
