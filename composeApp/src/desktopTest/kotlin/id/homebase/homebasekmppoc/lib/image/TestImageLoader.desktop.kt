package id.homebase.homebasekmppoc.lib.image

import kotlinx.io.Buffer
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

/**
 * Desktop/JVM implementation of TestImageLoader using Kotlin IO
 */
actual object TestImageLoader {
    actual fun loadTestImage(filename: String): ByteArray {
        // Try to load from classpath first (works for resources in build/test)
        val resourcePath = "test-images/$filename"
        val stream = this::class.java.classLoader?.getResourceAsStream(resourcePath)
        if (stream != null) {
            return stream.use { it.readBytes() }
        }

        // Fallback: try loading from file system using Kotlin IO
        val testResourcesPath = Path("composeApp/src/commonTest/resources/test-images/$filename")
        if (SystemFileSystem.exists(testResourcesPath)) {
            val buffer = Buffer()
            SystemFileSystem.source(testResourcesPath).use { source ->
                buffer.transferFrom(source)
            }
            return buffer.readByteArray()
        }

        throw IllegalArgumentException("Test image not found: $filename")
    }

    actual fun testImageExists(filename: String): Boolean {
        val resourcePath = "test-images/$filename"

        // Check classpath first
        if (this::class.java.classLoader?.getResource(resourcePath) != null) {
            return true
        }

        // Check file system as fallback
        val testResourcesPath = Path("composeApp/src/commonTest/resources/test-images/$filename")
        return SystemFileSystem.exists(testResourcesPath)
    }
}

/**
 * Gets all available test image filenames from resources
 */
actual fun getAvailableTestImages(): List<String> {
    // Try to get from file system using Kotlin IO
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






