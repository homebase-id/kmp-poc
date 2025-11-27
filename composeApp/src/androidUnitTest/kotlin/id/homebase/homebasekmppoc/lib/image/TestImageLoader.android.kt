package id.homebase.homebasekmppoc.lib.image

import kotlinx.io.Buffer
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

/**
 * Android implementation of TestImageLoader for JVM unit tests
 * Uses Kotlin IO for resource loading to avoid Java bridging overhead
 */
actual object TestImageLoader {
    actual fun loadTestImage(filename: String): ByteArray {
        // Use Kotlin IO to load from file system
        // In unit tests, we need to reference the source directory directly
        val testResourcesPath = Path("composeApp/src/commonTest/resources/test-images/$filename")

        if (SystemFileSystem.exists(testResourcesPath)) {
            val buffer = Buffer()
            SystemFileSystem.source(testResourcesPath).use { source ->
                buffer.transferFrom(source)
            }
            return buffer.readByteArray()
        }

        // Try alternative paths (relative to project root)
        val altPath = Path("src/commonTest/resources/test-images/$filename")
        if (SystemFileSystem.exists(altPath)) {
            val buffer = Buffer()
            SystemFileSystem.source(altPath).use { source ->
                buffer.transferFrom(source)
            }
            return buffer.readByteArray()
        }

        throw IllegalArgumentException(
            "Test image not found: $filename. " +
            "Searched paths:\n" +
            "  - $testResourcesPath\n" +
            "  - $altPath\n" +
            "Please add it to composeApp/src/commonTest/resources/test-images/"
        )
    }

    actual fun testImageExists(filename: String): Boolean {
        val testResourcesPath = Path("composeApp/src/commonTest/resources/test-images/$filename")
        if (SystemFileSystem.exists(testResourcesPath)) return true

        val altPath = Path("src/commonTest/resources/test-images/$filename")
        return SystemFileSystem.exists(altPath)
    }
}

/**
 * Gets all available test image filenames from resources
 */
actual fun getAvailableTestImages(): List<String> {
    val testResourcesPath = Path("composeApp/src/commonTest/resources/test-images")

    return try {
        if (SystemFileSystem.exists(testResourcesPath) &&
            SystemFileSystem.metadataOrNull(testResourcesPath)?.isDirectory == true) {
            SystemFileSystem.list(testResourcesPath)
                .filter { path ->
                    SystemFileSystem.metadataOrNull(path)?.isRegularFile == true &&
                    isImageFile(path.name)
                }
                .map { it.name }
        } else {
            // Fallback: try common image names
            listOf(
                "sample.jpg",
                "sample.png",
                "sample.webp",
                "sample.gif",
                "sample.bmp"
            ).filter { TestImageLoader.testImageExists(it) }
        }
    } catch (_: Exception) {
        // Fallback: try common image names
        listOf(
            "sample.jpg",
            "sample.png",
            "sample.webp",
            "sample.gif",
            "sample.bmp"
        ).filter { TestImageLoader.testImageExists(it) }
    }
}

