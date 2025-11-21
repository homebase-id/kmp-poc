package id.homebase.homebasekmppoc.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import co.touchlab.kermit.Logger
import kotlinx.datetime.LocalDate
import org.jetbrains.skia.Image

/**
 * Desktop/JVM implementation: Convert ByteArray to ImageBitmap using Skia
 */
actual fun ByteArray.toImageBitmap(): ImageBitmap? {
    Logger.d("toImageBitmap") { "toImageBitmap starting" }
    return try {
        Image.makeFromEncoded(this).toComposeImageBitmap()
    } catch (e: Exception) {
        Logger.e("toImageBitmap", e) { "toImageBitmap failed: ${e.message}" }
        null
    }
}
