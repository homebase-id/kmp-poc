package id.homebase.homebasekmppoc.lib.browser

import id.homebase.homebasekmppoc.AuthPresentationContextProvider
import id.homebase.homebasekmppoc.lib.youauth.YouAuthFlowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/** iOS implementation of BrowserLauncher using ASWebAuthenticationSession. */
actual object BrowserLauncher {

    actual fun launchAuthBrowser(url: String, scope: CoroutineScope) {
        val session =
                ASWebAuthenticationSession(
                        uRL = NSURL.URLWithString(url)!!,
                        callbackURLScheme = RedirectConfig.scheme,
                        completionHandler = { callbackURL: NSURL?, error: NSError? ->
                            if (callbackURL != null) {
                                val urlString = callbackURL.absoluteString!!
                                scope.launch(Dispatchers.Main) {
                                    YouAuthFlowManager.handleCallback(urlString)
                                }
                            } else if (error != null) {
                                println("Auth error: $error")
                            }
                        }
                )
        session.setPresentationContextProvider(AuthPresentationContextProvider())
        session.start()
    }

    actual fun openUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(nsUrl)
    }
}
