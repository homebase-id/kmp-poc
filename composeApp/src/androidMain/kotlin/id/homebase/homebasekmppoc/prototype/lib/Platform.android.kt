package id.homebase.homebasekmppoc.prototype.lib

import android.R
import android.app.AlertDialog
import android.os.Build
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.CoroutineScope
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import id.homebase.homebasekmppoc.MainActivity

// SEB:TODO this file is a mess of all sorts of platform abstractions and common code. Clean it up!

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun isAndroid(): Boolean = true

actual fun getRedirectScheme(): String = "youauth"

actual fun getRedirectUri(clientId: String): String {
    // Mobile uses custom URL scheme
    return "youauth://$clientId/authorization-code-callback"
}

actual fun launchCustomTabs(url: String, scope: CoroutineScope) {
    // Create custom color scheme to match app theme
    val colorSchemeParams = CustomTabColorSchemeParams.Builder()
        .setToolbarColor("#6750A4".toColorInt()) // Material You primary color
        .setSecondaryToolbarColor("#E7E0EC".toColorInt()) // Light background
        .build()

    val customTabsIntent = CustomTabsIntent.Builder()
        .setDefaultColorSchemeParams(colorSchemeParams)
        .setColorScheme(CustomTabsIntent.COLOR_SCHEME_SYSTEM) // Follow system theme
        .setShowTitle(true)
        .setUrlBarHidingEnabled(true)
        .setCloseButtonPosition(CustomTabsIntent.CLOSE_BUTTON_POSITION_END)
        .setStartAnimations(MainActivity.instance, R.anim.fade_in, R.anim.fade_out)
        .setExitAnimations(MainActivity.instance, R.anim.fade_in, R.anim.fade_out)
        .setShareState(CustomTabsIntent.SHARE_STATE_OFF) // Disable share button for auth flow
        .build()

    customTabsIntent.launchUrl(MainActivity.instance, url.toUri())
}

actual fun showMessage(title: String, message: String) {
    val builder = AlertDialog.Builder(MainActivity.instance)
    builder.setTitle(title)
        .setMessage(message)
        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        .show()
}