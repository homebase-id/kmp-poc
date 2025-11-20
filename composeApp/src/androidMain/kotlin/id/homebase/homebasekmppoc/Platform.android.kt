package id.homebase.homebasekmppoc

import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.CoroutineScope
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri

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
        .setStartAnimations(MainActivity.instance, android.R.anim.fade_in, android.R.anim.fade_out)
        .setExitAnimations(MainActivity.instance, android.R.anim.fade_in, android.R.anim.fade_out)
        .setShareState(CustomTabsIntent.SHARE_STATE_OFF) // Disable share button for auth flow
        .build()

    customTabsIntent.launchUrl(MainActivity.instance, url.toUri())
}

actual fun showMessage(title: String, message: String) {
    val builder = android.app.AlertDialog.Builder(MainActivity.instance)
    builder.setTitle(title)
        .setMessage(message)
        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        .show()
}