package id.homebase.homebasekmppoc.lib.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

/**
 * iOS implementation: Convert ByteArray to ImageBitmap using Skia
 */
actual fun ByteArray.toImageBitmap(): ImageBitmap? {
    return try {
        Image.makeFromEncoded(this).toComposeImageBitmap()
    } catch (e: Exception) {
        null
    }
}
