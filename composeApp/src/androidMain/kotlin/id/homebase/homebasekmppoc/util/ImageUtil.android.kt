package id.homebase.homebasekmppoc.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import co.touchlab.kermit.Logger

/**
 * Android implementation: Convert ByteArray to ImageBitmap using Android's BitmapFactory
 *
 * Note: Hardware bitmaps (HARDWARE config) cannot be used with Compose ImageBitmap.
 * We configure BitmapFactory to use ARGB_8888 instead to ensure compatibility.
 *
 * Image format detection and validation should be done before calling this function
 * using ImageFormatDetector in common code.
 */
actual fun ByteArray.toImageBitmap(): ImageBitmap? {
    Logger.d("toImageBitmap") { "Android: Converting ${this.size} bytes to ImageBitmap" }

    return try {
        // Configure BitmapFactory to avoid hardware bitmaps
        val options = BitmapFactory.Options().apply {
            // Prevent hardware bitmap allocation (not compatible with Compose)
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = false
        }

        val bitmap = BitmapFactory.decodeByteArray(this, 0, this.size, options)
        if (bitmap == null) {
            Logger.e("toImageBitmap") { "Android: BitmapFactory.decodeByteArray returned null" }
            Logger.e("toImageBitmap") {
                "First 16 bytes: ${this.take(16).joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }}"
            }
            return null
        }

        Logger.d("toImageBitmap") { "Android: Successfully decoded ${bitmap.width}x${bitmap.height}, config=${bitmap.config}" }
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        Logger.e("toImageBitmap", e) { "Android: Decoding failed - ${e.message}" }
        null
    }
}
