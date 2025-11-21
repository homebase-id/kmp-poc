package id.homebase.homebasekmppoc.util

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import co.touchlab.kermit.Logger

/**
 * Android implementation: Convert ByteArray to ImageBitmap using Android's BitmapFactory
 */
actual fun ByteArray.toImageBitmap(): ImageBitmap? {
    Logger.d("toImageBitmap") { "toImageBitmap starting" }
    return try {
        val bitmap = BitmapFactory.decodeByteArray(this, 0, this.size)
        bitmap?.asImageBitmap()
    } catch (e: Exception) {
        Logger.e("toImageBitmap", e) { "toImageBitmap failed: ${e.message}" }
        null
    }
}
