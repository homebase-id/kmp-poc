package id.homebase.homebasekmppoc.lib.image

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

/**
 * iOS implementation of TestImageLoader using NSBundle
 */
@OptIn(ExperimentalForeignApi::class)
actual object TestImageLoader {
    actual fun loadTestImage(filename: String): ByteArray {
        val bundle = NSBundle.mainBundle
        val path = bundle.pathForResource(
            name = filename.substringBeforeLast('.'),
            ofType = filename.substringAfterLast('.')
        ) ?: throw IllegalArgumentException("Test image not found in bundle: $filename")

        val data = NSData.dataWithContentsOfFile(path)
            ?: throw IllegalArgumentException("Failed to load test image: $filename")

        return data.toByteArray()
    }

    actual fun testImageExists(filename: String): Boolean {
        val bundle = NSBundle.mainBundle
        val path = bundle.pathForResource(
            name = filename.substringBeforeLast('.'),
            ofType = filename.substringAfterLast('.')
        )
        return path != null
    }
}

/**
 * Gets all available test image filenames from bundle
 */
actual fun getAvailableTestImages(): List<String> {
    // For iOS, we'll need to manually list the resources
    // This is a simplified version - you might need to adjust based on your setup
    val possibleImages = listOf(
        "sample.jpg",
        "sample.png",
        "sample.webp",
        "sample.gif",
        "sample.bmp"
    )
    return possibleImages.filter { TestImageLoader.testImageExists(it) }
}

/**
 * Helper to convert NSData to ByteArray
 */
@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    return ByteArray(this.length.toInt()).apply {
        usePinned {
            memcpy(it.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
        }
    }
}



