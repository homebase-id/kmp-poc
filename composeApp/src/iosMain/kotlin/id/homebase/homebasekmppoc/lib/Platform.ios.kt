package id.homebase.homebasekmppoc.lib

import id.homebase.homebasekmppoc.AuthPresentationContextProvider
import id.homebase.homebasekmppoc.prototype.lib.youauth.YouAuthCallbackRouter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice

// SEB:TODO this file is a mess of all sorts of platform abstractions and common code. Clean it up!

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun isAndroid(): Boolean = false

actual fun getRedirectScheme(): String = "youauth"

actual fun getRedirectUri(clientId: String): String {
    // Mobile uses custom URL scheme
    return "youauth://$clientId/authorization-code-callback"
}

actual fun launchCustomTabs(url: String, scope: CoroutineScope) {
    val session = ASWebAuthenticationSession(
        uRL = NSURL.URLWithString(url)!!,
        callbackURLScheme = "youauth",
        completionHandler = { callbackURL: NSURL?, error: NSError? ->
            if (callbackURL != null) {
                val urlString = callbackURL.absoluteString!!
                scope.launch(Dispatchers.Main) {
                    YouAuthCallbackRouter.handleCallback(urlString)
                }
            } else if (error != null) {
                println("Auth error: $error")
            }
        }
    )
    session.setPresentationContextProvider(AuthPresentationContextProvider())
    session.start()
}

actual fun showMessage(title: String, message: String) {
    val alertController = UIAlertController.alertControllerWithTitle(
        title = title,
        message = message,
        preferredStyle = UIAlertControllerStyleAlert
    )

    val okAction = UIAlertAction.actionWithTitle(
        title = "OK",
        style = UIAlertActionStyleDefault,
        handler = null
    )

    alertController.addAction(okAction)

    UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
        alertController,
        animated = true,
        completion = null
    )
}