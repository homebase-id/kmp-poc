package id.homebase.homebasekmppoc.util

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Convert a ByteArray (containing encoded image data like PNG, JPEG) to an ImageBitmap.
 * Platform-specific implementations handle the decoding.
 */
expect fun ByteArray.toImageBitmap(): ImageBitmap?
