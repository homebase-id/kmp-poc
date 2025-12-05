package id.homebase.homebasekmppoc.prototype.lib.image

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.homebase.homebasekmppoc.lib.image.ImageSize
import id.homebase.homebasekmppoc.lib.image.ThumbnailInstruction
import id.homebase.homebasekmppoc.lib.image.baseThumbSizes
import id.homebase.homebasekmppoc.lib.image.createThumbnails
import id.homebase.homebasekmppoc.lib.image.getRevisedThumbs
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Android instrumented test for ThumbnailGenerator.
 * Runs on actual Android device/emulator with full BitmapFactory support.
 *
 * To run these tests:
 * 1. Start an Android emulator or connect a device
 * 2. Run: ./gradlew :composeApp:connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class ThumbnailGeneratorInstrumentedTest {

    private fun loadTestImage(filename: String): ByteArray {
        val context = InstrumentationRegistry.getInstrumentation().context
        return context.assets.open("test-images/$filename").use { it.readBytes() }
    }

    @Test
    fun testGetRevisedThumbs_source2500px_keepsAllThumbs() {
        val sourceSize = ImageSize(pixelWidth = 2500, pixelHeight = 800)
        val result = getRevisedThumbs(sourceSize, baseThumbSizes)

        assertEquals(4, result.size, "Should keep all 4 thumbnails")
        assertTrue(result.any { it.maxPixelDimension == 320 && it.maxBytes == 26 * 1024 && it.quality == 84 })
        assertTrue(result.any { it.maxPixelDimension == 640 && it.maxBytes == 102 * 1024 && it.quality == 84 })
        assertTrue(result.any { it.maxPixelDimension == 1080 && it.maxBytes == 291 * 1024 && it.quality == 76 })
        assertTrue(result.any { it.maxPixelDimension == 1600 && it.maxBytes == 640 * 1024 && it.quality == 76 })
    }

    @Test
    fun testCreateThumbnails_withValidJpegData_returnsResult() = runTest {
        val imageData = loadTestImage("sample.jpg")
        val payloadKey = "test-jpeg-key"

        val (naturalSize, tinyThumb, additionalThumbnails) = createThumbnails(imageData, payloadKey)

        assertNotNull(naturalSize)
        assertNotNull(tinyThumb)
        assertNotNull(additionalThumbnails)
        assertTrue(additionalThumbnails.isNotEmpty())

        // Verify tiny thumb properties
        assertTrue(tinyThumb.contentType.startsWith("image/"))
        assertTrue(tinyThumb.contentBase64.isNotEmpty())

        // Verify additional thumbnails
        assertTrue(additionalThumbnails.all { it.key == payloadKey })
        assertTrue(additionalThumbnails.all { it.payload.isNotEmpty() })
        assertTrue(additionalThumbnails.all { it.quality in 1..100 })

        // Verify size constraints
        additionalThumbnails.forEachIndexed { index, thumb ->
            if (index < baseThumbSizes.size) {
                assertTrue(
                    thumb.payload.size <= baseThumbSizes[index].maxBytes,
                    "Thumbnail $index size ${thumb.payload.size} exceeds max ${baseThumbSizes[index].maxBytes}"
                )
            }
        }
    }

    @Test
    fun testCreateThumbnails_withValidPngData_returnsResult() = runTest {
        val imageData = loadTestImage("sample.png")
        val payloadKey = "test-png-key"

        val (naturalSize, tinyThumb, additionalThumbnails) = createThumbnails(imageData, payloadKey)

        assertNotNull(naturalSize)
        assertNotNull(tinyThumb)
        assertNotNull(additionalThumbnails)
        assertEquals(3, additionalThumbnails.size, "Should be 3")

        for (i in 0..2) {
            assertTrue(
                additionalThumbnails[i].payload.size <= baseThumbSizes[i].maxBytes,
                "Thumbnail $i size ${additionalThumbnails[i].payload.size} exceeds max ${baseThumbSizes[i].maxBytes}"
            )
            assertTrue(additionalThumbnails[i].quality in 1..100, "Quality for thumbnail $i is ${additionalThumbnails[i].quality}")
        }
    }

    @Test
    fun testCreateThumbnails_withValidWebpData_returnsResult() = runTest {
        val imageData = loadTestImage("sample.webp")
        val payloadKey = "test-webp-key"

        val (naturalSize, tinyThumb, additionalThumbnails) = createThumbnails(imageData, payloadKey)

        assertNotNull(naturalSize)
        assertNotNull(tinyThumb)
        assertNotNull(additionalThumbnails)
        assertTrue(additionalThumbnails.isNotEmpty())

        assertTrue(additionalThumbnails.all { it.key == payloadKey })
        assertTrue(additionalThumbnails.all { it.payload.isNotEmpty() })
        assertTrue(additionalThumbnails.all { it.quality in 1..100 })
    }

    @Test
    fun testCreateThumbnails_withSvgData_handlesVectorImages() = runTest {
        val imageData = loadTestImage("sample.svg")
        val payloadKey = "test-svg-key"

        val (naturalSize, tinyThumb, additionalThumbnails) = createThumbnails(imageData, payloadKey)

        assertNotNull(naturalSize)
        assertNotNull(tinyThumb)
        assertNotNull(additionalThumbnails)
        assertEquals(1, additionalThumbnails.size)

        val svgThumbnail = additionalThumbnails.first()
        assertEquals("image/svg+xml", svgThumbnail.contentType)
        assertTrue(svgThumbnail.payload.contentEquals(imageData))
        assertEquals(100, svgThumbnail.quality)

        assertEquals(800, naturalSize.pixelWidth)
        assertEquals(800, naturalSize.pixelHeight)
    }

    @Test
    fun testCreateThumbnails_withGifData_handlesAnimatedImages() = runTest {
        val imageData = loadTestImage("sample.gif")
        val payloadKey = "test-gif-key"

        val (naturalSize, tinyThumb, additionalThumbnails) = createThumbnails(imageData, payloadKey)

        assertNotNull(naturalSize)
        assertNotNull(tinyThumb)
        assertNotNull(additionalThumbnails)
        assertEquals(0, additionalThumbnails.size)
    }

    @Test
    fun testCreateThumbnails_aspectScaling() = runTest {
        val imageData = loadTestImage("Sample800x400.webp")
        val payloadKey = "test-webp-key"

        val customSizes = listOf(
            ThumbnailInstruction(quality = 76, maxPixelDimension = 400, maxBytes = Int.MAX_VALUE),
            ThumbnailInstruction(quality = 76, maxPixelDimension = 600, maxBytes = Int.MAX_VALUE)
        )
        val (_, _, additionalThumbnails) = createThumbnails(imageData, payloadKey, customSizes)

        assertEquals(2, additionalThumbnails.size)

        assertEquals(76, additionalThumbnails[0].quality)
        assertEquals(400, additionalThumbnails[0].imageSize.pixelWidth)
        assertEquals(200, additionalThumbnails[0].imageSize.pixelHeight)
        assertEquals(76, additionalThumbnails[1].quality)
        assertEquals(600, additionalThumbnails[1].imageSize.pixelWidth)
        assertEquals(300, additionalThumbnails[1].imageSize.pixelHeight)
    }

    @Test
    fun testCreateThumbnails_onePreciseUnscaled() = runTest {
        val imageData = loadTestImage("sample640x640.webp")
        val payloadKey = "test"

        val customSizes = listOf(
            ThumbnailInstruction(quality = 76, maxPixelDimension = 640, maxBytes = 150 * 1024)
        )
        val (_, _, additionalThumbnails) = createThumbnails(imageData, payloadKey, customSizes)

        assertEquals(1, additionalThumbnails.size)
        assertEquals(imageData.size, additionalThumbnails[0].payload.size)
        assertTrue(imageData.contentEquals(additionalThumbnails[0].payload))
    }
}

