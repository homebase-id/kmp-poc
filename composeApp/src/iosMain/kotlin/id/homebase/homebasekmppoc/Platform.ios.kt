package id.homebase.homebasekmppoc

import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.Foundation.NSURL
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun isAndroid(): Boolean = false

actual fun launchCustomTabs(url: String) {
    val session = ASWebAuthenticationSession(
        uRL = NSURL.URLWithString(url)!!,
        callbackURLScheme = "youauth",
        completionHandler = { callbackURL: NSURL?, error: platform.Foundation.NSError? ->
            if (callbackURL != null) {
                val urlString = callbackURL.absoluteString!!
                handleAuthCallback(urlString)
            } else if (error != null) {
                println("Auth error: $error")
            }
        }
    )
    session.setPresentationContextProvider(AuthPresentationContextProvider())
    session.start()
}

actual fun showMessage(title: String, message: String) {
    val alertController = platform.UIKit.UIAlertController.alertControllerWithTitle(
        title = title,
        message = message,
        preferredStyle = platform.UIKit.UIAlertControllerStyleAlert
    )

    val okAction = platform.UIKit.UIAlertAction.actionWithTitle(
        title = "OK",
        style = platform.UIKit.UIAlertActionStyleDefault,
        handler = null
    )

    alertController.addAction(okAction)

    platform.UIKit.UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
        alertController,
        animated = true,
        completion = null
    )
}