package id.homebase.homebasekmppoc

import android.net.Uri
import android.os.Build
import androidx.browser.customtabs.CustomTabsIntent

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun isAndroid(): Boolean = true

actual fun launchCustomTabs(url: String) {
    val customTabsIntent = CustomTabsIntent.Builder().build()
    customTabsIntent.launchUrl(MainActivity.instance, Uri.parse(url))
}

actual fun showAuthDialog(title: String, message: String) {
    val builder = android.app.AlertDialog.Builder(MainActivity.instance)
    builder.setTitle(title)
        .setMessage(message)
        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        .show()
}