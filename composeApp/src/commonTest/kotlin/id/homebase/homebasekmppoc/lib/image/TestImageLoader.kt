package id.homebase.homebasekmppoc.lib.image

import kotlin.test.fail

/**
 * Helper object for loading test images from resources.
 * This is common code - platform-specific implementations will provide the actual file loading.
 */
expect object TestImageLoader {
    /**
     * Loads a test image from the test-images resources directory
     * @param filename Name of the image file (e.g., "sample.jpg")
     * @return Image data as ByteArray
     * @throws IllegalArgumentException if file not found
     */
    fun loadTestImage(filename: String): ByteArray

    /**
     * Checks if a test image exists
     * @param filename Name of the image file
     * @return true if the file exists, false otherwise
     */
    fun testImageExists(filename: String): Boolean
}

/**
 * Gets all available test image filenames from the test-images directory
 */
expect fun getAvailableTestImages(): List<String>

/**
 * Checks if a file is a supported image format
 */
fun isImageFile(filename: String): Boolean {
    val extension = filename.substringAfterLast('.', "").lowercase()
    return extension in setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg")
}

/**
 * Helper to load test image or skip test if not available
 */
fun loadTestImageOrSkip(filename: String): ByteArray {
    return if (TestImageLoader.testImageExists(filename)) {
        TestImageLoader.loadTestImage(filename)
    } else {
        fail("Test image not found: $filename. Please add sample images to composeApp/src/commonTest/resources/test-images/")
    }
}

