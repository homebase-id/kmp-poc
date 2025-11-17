package id.homebase.homebasekmppoc

import androidx.compose.ui.window.ComposeUIViewController
import id.homebase.homebasekmppoc.pages.app.App
import platform.UIKit.UIViewController
import platform.darwin.NSObject

object MainViewControllerRef {
    lateinit var instance: UIViewController
}

class AuthPresentationContextProvider : NSObject(), platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol {
    override fun presentationAnchorForWebAuthenticationSession(session: platform.AuthenticationServices.ASWebAuthenticationSession): platform.UIKit.UIWindow? {
        return MainViewControllerRef.instance.view.window
    }
}

fun MainViewController(): UIViewController {
    val controller = ComposeUIViewController { App() }
    MainViewControllerRef.instance = controller
    return controller
}

