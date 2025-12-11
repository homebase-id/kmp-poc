package id.homebase.homebasekmppoc.prototype

import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String =
            UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun isAndroid(): Boolean = false

actual fun showMessage(title: String, message: String) {
    val alertController =
            UIAlertController.alertControllerWithTitle(
                    title = title,
                    message = message,
                    preferredStyle = UIAlertControllerStyleAlert
            )

    val okAction =
            UIAlertAction.actionWithTitle(
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
