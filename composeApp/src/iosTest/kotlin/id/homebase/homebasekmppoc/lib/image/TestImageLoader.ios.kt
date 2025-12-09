package id.homebase.homebasekmppoc.lib.image

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.dataWithContentsOfFile
import platform.posix.getenv

/**
 * iOS implementation of TestImageLoader using NSBundle
 */
@OptIn(ExperimentalForeignApi::class)
actual object TestImageLoader {
    actual fun loadTestImage(filename: String): ByteArray {
        val envPath = getenv("TEST_IMAGES_DIR")?.toKString()
            ?: error("TEST_IMAGES_DIR env var not set. Check build.gradle.kts")

        val fullPath = "$envPath/$filename"

        // 1. Load NSData options (returns null if file doesn't exist)
        val data = NSData.dataWithContentsOfFile(fullPath)
            ?: throw IllegalArgumentException("File not found at path: $fullPath")

        // 2. Convert NSData to Kotlin ByteArray
        return data.toByteArray()
    }

    //

    actual fun testImageExists(filename: String): Boolean {
        val envPath = getenv("TEST_IMAGES_DIR")?.toKString()
            ?: error("TEST_IMAGES_DIR not set. Check build.gradle.kts")

        val fullPath = "$envPath/$filename"

        // Use NSFileManager for robust file checking on iOS/macOS
        return NSFileManager.defaultManager.fileExistsAtPath(fullPath)
    }
}

/**
 * Gets all available test image filenames from bundle
 */
// actual fun getAvailableTestImages(): List<String> {
//     // For iOS, we'll need to manually list the resources
//     // This is a simplified version - you might need to adjust based on your setup
//     val possibleImages = listOf(
//         "sample.jpg",
//         "sample.png",
//         "sample.webp",
//         "sample.gif",
//         "sample.bmp"
//     )
//     return possibleImages.filter { TestImageLoader.testImageExists(it) }
// }

/**
 * Helper to convert NSData to ByteArray
 */
@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)

    // 'bytes' property returns a raw C pointer (COpaquePointer?)
    // We reinterpret it as a ByteVar pointer to read bytes from it
    val rawPointer = this.bytes ?: return ByteArray(0)

    return rawPointer.reinterpret<ByteVar>().readBytes(length)
}


