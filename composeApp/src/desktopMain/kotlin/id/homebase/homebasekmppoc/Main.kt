package id.homebase.homebasekmppoc

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Odin KMP",
    ) {
        App()
        MessageDialogHandler()
    }
}
