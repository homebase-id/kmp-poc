package id.homebase.homebasekmppoc

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import id.homebase.homebasekmppoc.youauth.YouAuthCallbackRouter
import kotlinx.browser.document
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)
fun main() {
    // Capture callback URL before rendering
    val currentUrl = window.location.href
    val hasCallback = currentUrl.contains("identity=") && currentUrl.contains("public_key=")

    ComposeViewport(document.body!!) {
        // Process callback after UI is initialized
        if (hasCallback) {
            LaunchedEffect(Unit) {
                YouAuthCallbackRouter.handleCallback(currentUrl)
                // Clean URL to remove sensitive params from browser history
                window.history.replaceState(null, "", window.location.pathname)
            }
        }

        App()
        MessageDialogHandler()
    }
}
