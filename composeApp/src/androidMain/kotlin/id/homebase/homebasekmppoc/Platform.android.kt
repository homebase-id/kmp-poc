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