package id.homebase.homebasekmppoc.prototype

import android.app.AlertDialog
import android.os.Build
import id.homebase.homebasekmppoc.lib.core.ActivityProvider

class AndroidPlatform : Platform {
    override val name: PlatformType = PlatformType.ANDROID
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun isAndroid(): Boolean = true

actual fun showMessage(title: String, message: String) {
    try {
        val activity = ActivityProvider.requireActivity()
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
    } catch (e: IllegalStateException) {
        println("ERROR: Activity not available - Cannot show message: $title - $message")
    }
}
