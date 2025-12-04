package id.homebase.homebasekmppoc.prototype.lib.image

import kotlinx.io.Buffer
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import java.io.File

/**
 * Android implementation of TestImageLoader for JVM unit tests
 * Uses Kotlin IO for resource loading to avoid Java bridging overhead
 */
actual object TestImageLoader {
    actual fun loadTestImage(filename: String): ByteArray {
        val envPath = System.getenv("TEST_IMAGES_DIR")
            ?: error("TEST_IMAGES_DIR env var not set. Check build.gradle.kts")

        val file = File(envPath, filename)

        if (!file.exists()) {
            throw IllegalArgumentException("File not found at path: ${file.absolutePath}")
        }

        // Simple Java IO to read bytes
        return file.readBytes()
    }

    //

    actual fun testImageExists(filename: String): Boolean {
        val envPath = System.getenv("TEST_IMAGES_DIR")
            ?: error("TEST_IMAGES_DIR not set. Check build.gradle.kts")

        val file = File(envPath, filename)
        return file.exists()
    }

}

/**
 * Gets all available test image filenames from resources
 */
// actual fun getAvailableTestImages(): List<String> {
//     val testResourcesPath = Path("composeApp/src/commonTest/resources/test-images")
//
//     return try {
//         if (SystemFileSystem.exists(testResourcesPath) &&
//             SystemFileSystem.metadataOrNull(testResourcesPath)?.isDirectory == true) {
//             SystemFileSystem.list(testResourcesPath)
//                 .filter { path ->
//                     SystemFileSystem.metadataOrNull(path)?.isRegularFile == true &&
//                     isImageFile(path.name)
//                 }
//                 .map { it.name }
//         } else {
//             // Fallback: try common image names
//             listOf(
//                 "sample.jpg",
//                 "sample.png",
//                 "sample.webp",
//                 "sample.gif",
//                 "sample.bmp"
//             ).filter { TestImageLoader.testImageExists(it) }
//         }
//     } catch (_: Exception) {
//         // Fallback: try common image names
//         listOf(
//             "sample.jpg",
//             "sample.png",
//             "sample.webp",
//             "sample.gif",
//             "sample.bmp"
//         ).filter { TestImageLoader.testImageExists(it) }
//     }
// }

