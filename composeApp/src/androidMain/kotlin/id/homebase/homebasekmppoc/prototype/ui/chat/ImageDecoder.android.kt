package id.homebase.homebasekmppoc.prototype.ui.chat

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** Android implementation: Decodes image bytes to ImageBitmap using BitmapFactory. */
actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? {
    return try {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}
