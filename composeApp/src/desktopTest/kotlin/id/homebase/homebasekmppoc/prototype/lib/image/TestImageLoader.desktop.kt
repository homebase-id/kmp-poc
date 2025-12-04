package id.homebase.homebasekmppoc.prototype.lib.image

import java.io.File

/**
 * Desktop/JVM implementation of TestImageLoader using Kotlin IO
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
//     // Try to get from file system using Kotlin IO
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






