package id.homebase.homebasekmppoc

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Odin KMP",
        state = WindowState(size = DpSize(800.dp, 900.dp))
    ) {
        App()
        MessageDialogHandler()
    }
}
