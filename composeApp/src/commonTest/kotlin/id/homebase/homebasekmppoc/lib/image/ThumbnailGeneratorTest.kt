package id.homebase.homebasekmppoc.lib.image

import id.homebase.homebasekmppoc.prototype.lib.drives.files.ThumbnailFile
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.EmbeddedThumb
import kotlin.test.*
import kotlinx.coroutines.test.runTest

/**
 * Common tests for ThumbnailGenerator that run on all platforms (Android, iOS, Desktop)
 *
 * The class is abstract and platform-specific subclasses provide the test runners, otherwise
 * Robolectric would try to run this directly and fail.
 */
abstract class ThumbnailGeneratorTest {

    // ========== GetRevisedThumbs Tests ==========

    @Test
    fun getRevisedThumbs_source2500px_keepsAllThumbs() {
        // Arrange
        val sourceSize = ImageSize(pixelWidth = 2500, pixelHeight = 800)

        // Act
        val result = getRevisedThumbs(sourceSize, baseThumbSizes)

        // Assert
        assertEquals(4, result.size, "Should keep all 4 thumbnails")
        assertTrue(
                result.any {
                    it.maxPixelDimension == 320 && it.maxBytes == 26 * 1024 && it.quality == 84
                }
        )
        assertTrue(
                result.any {
                    it.maxPixelDimension == 640 && it.maxBytes == 102 * 1024 && it.quality == 84
                }
        )
        assertTrue(
                result.any {
                    it.maxPixelDimension == 1080 && it.maxBytes == 291 * 1024 && it.quality == 76
                }
        )
        assertTrue(
                result.any {
                    it.maxPixelDimension == 1600 && it.maxBytes == 640 * 1024 && it.quality == 76
                }
        )
    }

    @Test
    fun getRevisedThumbs_source200px_keepsNoneAdds200() {
        // Arrange
        val sourceSize = ImageSize(pixelWidth = 200, pixelHeight = 150)

        // Act
        val result = getRevisedThumbs(sourceSize, baseThumbSizes)

        // Assert
        assertEquals(1, result.size, "Should have only the source size")
        assertTrue(result.any { it.maxPixelDimension == 200 })
        assertTrue(result.any { it.quality == 84 })
        assertEquals(16640, result[0].maxBytes)
    }

    @Test
    fun getRevisedThumbs_source50px_minimum_keepsNoneAdds50() {
        // Arrange
        val sourceSize = ImageSize(pixelWidth = 40, pixelHeight = 50)

        // Act
        val result = getRevisedThumbs(sourceSize, baseThumbSizes)

        // Assert
        assertEquals(1, result.size, "Should have only the source size")
        assertTrue(result.any { it.maxPixelDimension == 50 && it.quality == 84 })
        assertEquals(10 * 1024, result[0].maxBytes) // minimum threshold
    }

    @Test
    fun getRevisedThumbs_source1660px_removes1600Adds1660() {
        // Arrange
        val sourceSize = ImageSize(pixelWidth = 1660, pixelHeight = 1200)

        // Act
        val result = getRevisedThumbs(sourceSize, baseThumbSizes)

        // Assert
        assertEquals(4, result.size, "Should have 4 thumbnails")
        assertTrue(result.any { it.maxPixelDimension == 320 })
        assertTrue(result.any { it.maxPixelDimension == 640 })
        assertTrue(result.any { it.maxPixelDimension == 1080 })
        assertTrue(result.any { it.maxPixelDimension == 1660 })
        assertFalse(result.any { it.maxPixelDimension == 1600 }, "1600px thumb should be removed")
    }

    @Test
    fun getRevisedThumbs_source1540px_removes1600Adds1540() {
        // Arrange
        val sourceSize = ImageSize(pixelWidth = 1540, pixelHeight = 1200)

        // Act
        val result = getRevisedThumbs(sourceSize, baseThumbSizes)

        // Assert
        assertEquals(4, result.size, "Should have 4 thumbnails")
        assertTrue(result.any { it.maxPixelDimension == 320 })
        assertTrue(result.any { it.maxPixelDimension == 640 })
        assertTrue(result.any { it.maxPixelDimension == 1080 })
        assertTrue(result.any { it.maxPixelDimension == 1540 })
        assertFalse(result.any { it.maxPixelDimension == 1600 }, "1600px thumb should be removed")
    }

    @Test
    fun getRevisedThumbs_source500px_keeps320Adds500() {
        // Arrange
        val sourceSize = ImageSize(pixelWidth = 500, pixelHeight = 300)

        // Act
        val result = getRevisedThumbs(sourceSize, baseThumbSizes)

        // Assert
        assertEquals(2, result.size, "Should have 2 thumbnails")
        assertTrue(result.any { it.maxPixelDimension == 320 })
        assertTrue(result.any { it.maxPixelDimension == 500 && it.quality == 84 })
        assertEquals(81600, result[1].maxBytes)
    }

    // ========== CreateThumbnails Tests ==========

    @Test
    fun createThumbnails_withValidJpegData_returnsResult() = runTest {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.jpg")
        val payloadKey = "test-jpeg-key"

        // Act
        val (naturalSize, tinyThumb, additionalThumbnails) = createThumbnails(imageData, payloadKey)

        // Assert
        assertNotNull(naturalSize)
        assertNotNull(tinyThumb)
        assertNotNull(additionalThumbnails)
        assertTrue(additionalThumbnails.isNotEmpty())

        // Verify tiny thumb properties
        assertTrue(tinyThumb.contentType.startsWith("image/"))
        assertTrue(tinyThumb.content.isNotEmpty())

        // Verify additional thumbnails
        assertTrue(additionalThumbnails.all { it.key == payloadKey })
        assertTrue(additionalThumbnails.all { it.payload.isNotEmpty() })
        // Quality should be reasonable (between 1 and 100) and all thumbnails should fit within
        // maxBytes
        assertTrue(additionalThumbnails.all { it.quality in 1..100 })
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
    fun createThumbnails_withValidPngData_returnsResult() = runTest {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.png")
        val payloadKey = "test-png-key"

        // Act
        val (naturalSize, tinyThumb, additionalThumbnails) = createThumbnails(imageData, payloadKey)

        // Assert
        assertNotNull(naturalSize)
        assertNotNull(tinyThumb)
        assertNotNull(additionalThumbnails)
        assertEquals(3, additionalThumbnails.size, "Should be 3")

        for (i in 0..2) {
            assertTrue(
                    additionalThumbnails[i].payload.size <= baseThumbSizes[i].maxBytes,
                    "Thumbnail $i size ${additionalThumbnails[i].payload.size} exceeds max ${baseThumbSizes[i].maxBytes}"
            )
            // Quality should be reasonable
            assertTrue(
                    additionalThumbnails[i].quality in 1..100,
                    "Quality for thumbnail $i is ${additionalThumbnails[i].quality}"
            )
        }
    }

    @Test
    fun createTooLargeThumbnails_qualityDrops() = runTest {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.png")
        val payloadKey = "test"

        val customSizes =
                listOf(
                        ThumbnailInstruction(
                                quality = 100,
                                maxPixelDimension = 1024,
                                maxBytes = 70 * 1024
                        )
                )

        // Act
        val (naturalSize, tinyThumb, additionalThumbnails) =
                createThumbnails(imageData, payloadKey, customSizes)

        // Assert
        assertNotNull(naturalSize)
        assertNotNull(tinyThumb)
        assertNotNull(additionalThumbnails)
        assertEquals(1, additionalThumbnails.size, "Should be 1")

        assertTrue(additionalThumbnails[0].payload.size <= customSizes[0].maxBytes, "Too large")
        // Quality must change!
        assertNotEquals(
                baseThumbSizes[0].quality,
                additionalThumbnails[0].quality,
                "Quality unchanged"
        )
        assertTrue(additionalThumbnails[0].quality >= 1, "Quality too small")
    }

    @Test
    fun createTooLargeTinyThumbnails_reducesSize() = runTest {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.png")
        val payloadKey = "test"

        val customSize = ThumbnailInstruction(quality = 50, maxPixelDimension = 20, maxBytes = 100)

        // Act
        val result = createImageThumbnail(imageData, payloadKey, customSize, isTinyThumb = true)

        // Assert
        assertEquals(1, result.quality)

        // Make sure the tiny got reduced
        assertEquals(ImageSize(15, 15), result.imageSize)
    }

    @Test
    fun createThumbnails_withRealPhoto_returnsResult() = runTest {
        // Arrange
        val imageData = loadTestImageOrSkip("yummy.jpg")
        val payloadKey = "test"

        // Act
        val (naturalSize, tinyThumb, additionalThumbnails) = createThumbnails(imageData, payloadKey)

        // Assert
        assertNotNull(naturalSize)
        assertNotNull(tinyThumb)
        assertNotNull(additionalThumbnails)
        assertEquals(4, additionalThumbnails.size, "Should be 4")

        for (i in 0..2) {
            assertTrue(
                    additionalThumbnails[i].payload.size <= baseThumbSizes[i].maxBytes,
                    "Too large"
            )
            assertEquals(
                    baseThumbSizes[i].quality,
                    additionalThumbnails[i].quality,
                    "Quality changed"
            )
        }
    }

    @Test
    fun createThumbnails_scaling() = runTest {
        // Arrange
        val imageData = loadTestImageOrSkip("sample1024x1024.webp")
        val payloadKey = "test-webp-key"

        // Act
        val customSizes =
                listOf(
                        ThumbnailInstruction(
                                quality = 76,
                                maxPixelDimension = 400,
                                maxBytes = Int.MAX_VALUE
                        ),
                        ThumbnailInstruction(
                                quality = 76,
                                maxPixelDimension = 600,
                                maxBytes = Int.MAX_VALUE
                        )
                )
        val (_, _, additionalThumbnails) = createThumbnails(imageData, payloadKey, customSizes)

        // Assert
        assertEquals(2, additionalThumbnails.size)

        // Verify additional thumbnails
        assertEquals(76, additionalThumbnails[0].quality)
        assertEquals(400, additionalThumbnails[0].imageSize.pixelWidth)
        assertEquals(400, additionalThumbnails[0].imageSize.pixelHeight)
        assertEquals(76, additionalThumbnails[1].quality)
        assertEquals(600, additionalThumbnails[1].imageSize.pixelWidth)
        assertEquals(600, additionalThumbnails[1].imageSize.pixelHeight)
    }

    @Test
    fun createThumbnails_aspectScaling() = runTest {
        // Arrange
        val imageData = loadTestImageOrSkip("Sample800x400.webp")
        val payloadKey = "test-webp-key"

        // Act
        val customSizes =
                listOf(
                        ThumbnailInstruction(
                                quality = 76,
                                maxPixelDimension = 400,
                                maxBytes = Int.MAX_VALUE
                        ),
                        ThumbnailInstruction(
                                quality = 76,
                                maxPixelDimension = 600,
                                maxBytes = Int.MAX_VALUE
                        )
                )
        val (_, _, additionalThumbnails) = createThumbnails(imageData, payloadKey, customSizes)

        // Assert
        assertEquals(2, additionalThumbnails.size)

        // Verify additional thumbnails
        assertEquals(76, additionalThumbnails[0].quality)
        assertEquals(400, additionalThumbnails[0].imageSize.pixelWidth)
        assertEquals(200, additionalThumbnails[0].imageSize.pixelHeight)
        assertEquals(76, additionalThumbnails[1].quality)
        assertEquals(600, additionalThumbnails[1].imageSize.pixelWidth)
        assertEquals(300, additionalThumbnails[1].imageSize.pixelHeight)
    }

    @Test
    fun createThumbnails_onePreciseUnscaled() = runTest {
        // Arrange
        val imageData = loadTestImageOrSkip("sample640x640.webp")
        val payloadKey = "test"

        // Act
        val customSizes =
                listOf(
                        ThumbnailInstruction(
                                quality = 76,
                                maxPixelDimension = 640,
                                maxBytes = 150 * 1024
                        )
                )
        val (_, _, additionalThumbnails) = createThumbnails(imageData, payloadKey, customSizes)

        // Assert
        assertEquals(1, additionalThumbnails.size)

        // Verify it's the exact same size
        assertEquals(imageData.size, additionalThumbnails[0].payload.size)
        assertTrue(imageData.contentEquals(additionalThumbnails[0].payload))
    }

    @Test
    fun createThumbnails_aspectScalingVertical() = runTest {
        // Arrange
        val imageData = loadTestImageOrSkip("Sample400x800.webp")
        val payloadKey = "test-webp-key"

        // Act
        val customSizes =
                listOf(
                        ThumbnailInstruction(
                                quality = 76,
                                maxPixelDimension = 200,
                                maxBytes = Int.MAX_VALUE
                        ),
                        ThumbnailInstruction(
                                quality = 76,
                                maxPixelDimension = 100,
                                maxBytes = Int.MAX_VALUE
                        )
                )
        val (_, _, additionalThumbnails) = createThumbnails(imageData, payloadKey, customSizes)

        // Assert
        assertEquals(2, additionalThumbnails.size)

        // Verify additional thumbnails (note: order might be different due to sorting)
        val thumb100 = additionalThumbnails.find { it.imageSize.pixelHeight == 100 }!!
        val thumb200 = additionalThumbnails.find { it.imageSize.pixelHeight == 200 }!!

        assertEquals(50, thumb100.imageSize.pixelWidth)
        assertEquals(100, thumb100.imageSize.pixelHeight)
        assertEquals(76, thumb100.quality)
        assertEquals(100, thumb200.imageSize.pixelWidth)
        assertEquals(200, thumb200.imageSize.pixelHeight)
        assertEquals(76, thumb200.quality)
    }

    @Test
    fun createThumbnails_withValidWebpData_returnsResult() = runTest {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.webp")
        val payloadKey = "test-webp-key"

        // Act
        val (naturalSize, tinyThumb, additionalThumbnails) = createThumbnails(imageData, payloadKey)

        // Assert
        assertNotNull(naturalSize)
        assertNotNull(tinyThumb)
        assertNotNull(additionalThumbnails)
        assertTrue(additionalThumbnails.isNotEmpty())

        // Verify tiny thumb properties
        assertTrue(tinyThumb.contentType.startsWith("image/"))
        assertTrue(tinyThumb.content.isNotEmpty())

        // Verify additional thumbnails
        assertTrue(additionalThumbnails.all { it.key == payloadKey })
        assertTrue(additionalThumbnails.all { it.payload.isNotEmpty() })
        // Quality should be reasonable
        assertTrue(additionalThumbnails.all { it.quality in 1..100 })
    }

    @Test
    fun createThumbnails_withSvgData_handlesVectorImages() = runTest {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.svg")
        val payloadKey = "test-svg-key"

        // Act
        val (naturalSize, tinyThumb, additionalThumbnails) = createThumbnails(imageData, payloadKey)

        // Assert
        assertNotNull(naturalSize)
        assertNotNull(tinyThumb)
        assertNotNull(additionalThumbnails)
        assertEquals(1, additionalThumbnails.size)

        // SVG should maintain original content type
        val svgThumbnail = additionalThumbnails.first()
        assertEquals("image/svg+xml", svgThumbnail.contentType)
        assertTrue(svgThumbnail.payload.contentEquals(imageData))
        assertEquals(100, svgThumbnail.quality)

        // Natural size should be extracted from SVG
        assertEquals(800, naturalSize.pixelWidth)
        assertEquals(800, naturalSize.pixelHeight)
    }

    @Test
    fun createThumbnails_withGifData_handlesAnimatedImages() = runTest {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.gif")
        val payloadKey = "test-gif-key"

        // Act
        val (naturalSize, tinyThumb, additionalThumbnails) = createThumbnails(imageData, payloadKey)

        // Assert
        assertNotNull(naturalSize)
        assertNotNull(tinyThumb)
        assertNotNull(additionalThumbnails)
        assertEquals(0, additionalThumbnails.size)
    }

    @Test
    fun createThumbnails_withGifDataPacman_handlesAnimatedImages() = runTest {
        // Arrange
        val imageData = loadTestImageOrSkip("pacman.gif")
        val payloadKey = "test-gif-key"

        // Act
        val (naturalSize, tinyThumb, additionalThumbnails) = createThumbnails(imageData, payloadKey)

        // Assert
        assertNotNull(naturalSize)
        assertNotNull(tinyThumb)
        assertNotNull(additionalThumbnails)
        assertEquals(0, additionalThumbnails.size)
    }

    @Test
    fun createThumbnails_withOldGifData() = runTest {
        // Arrange
        val imageData = loadTestImageOrSkip("ValhallaLogo.gif")
        val payloadKey = "test-gif-key"

        // Act
        val (naturalSize, tinyThumb, additionalThumbnails) = createThumbnails(imageData, payloadKey)

        // Assert
        assertNotNull(naturalSize)
        assertNotNull(tinyThumb)
        assertNotNull(additionalThumbnails)
        assertEquals(0, additionalThumbnails.size)
    }

    @Test
    fun createThumbnails_withCustomSizes_usesProvidedSizes() = runTest {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.webp")
        val payloadKey = "test-custom-key"
        val customSizes =
                listOf(
                        ThumbnailInstruction(
                                quality = 80,
                                maxPixelDimension = 200,
                                maxBytes = Int.MAX_VALUE
                        ),
                        ThumbnailInstruction(
                                quality = 90,
                                maxPixelDimension = 800,
                                maxBytes = Int.MAX_VALUE
                        )
                )

        // Act
        val (naturalSize, tinyThumb, additionalThumbnails) =
                createThumbnails(imageData, payloadKey, customSizes)

        // Assert
        assertNotNull(naturalSize)
        assertNotNull(additionalThumbnails)

        // Should contain at least 1 thumbnail
        assertTrue(additionalThumbnails.size >= 1)
        assertTrue(additionalThumbnails.all { it.quality == 80 || it.quality == 90 })
        assertTrue(additionalThumbnails.all { it.key == payloadKey })
    }

    @Test
    fun createThumbnails_withDifferentImageFormats_returnsConsistentResults() = runTest {
        // Arrange
        val testCases =
                listOf(
                        "sample.gif" to "gif",
                        "sample.bmp" to "bmp",
                        "sample.jpg" to "jpeg",
                        "sample.png" to "png",
                        "sample.webp" to "webp"
                )

        testCases.forEach { (filename, format) ->
            if (TestImageLoader.testImageExists(filename)) {
                val imageData = loadTestImageOrSkip(filename)
                val payloadKey = "test-$format-key"

                // Act
                val (naturalSize, tinyThumb, additionalThumbnails) =
                        createThumbnails(imageData, payloadKey)

                // Assert
                assertNotNull(naturalSize, "NaturalSize should not be null for $format")
                assertNotNull(tinyThumb, "TinyThumb should not be null for $format")
                assertNotNull(
                        additionalThumbnails,
                        "AdditionalThumbnails should not be null for $format"
                )

                if (format == "gif") {
                    assertEquals(
                            0,
                            additionalThumbnails.size,
                            "Should have NO thumbnails for $format"
                    )
                } else {
                    assertEquals(
                            3,
                            additionalThumbnails.size,
                            "Should have 3 thumbnails for $format"
                    )
                    for (i in 0..2) {
                        assertTrue(
                                additionalThumbnails[i].payload.size <= baseThumbSizes[i].maxBytes,
                                "Thumbnail $i for $format exceeds size limit: ${additionalThumbnails[i].payload.size} > ${baseThumbSizes[i].maxBytes}"
                        )
                        assertTrue(
                                additionalThumbnails[i].quality in 1..100,
                                "Quality for $format thumbnail $i out of range: ${additionalThumbnails[i].quality}"
                        )
                    }
                }
            }
        }
    }

    // ========== Model Tests ==========

    @Test
    fun thumbnailInstruction_withDefaultType_hasCorrectProperties() {
        // Arrange & Act
        val instruction =
                ThumbnailInstruction(quality = 75, maxPixelDimension = 100, maxBytes = 10000)

        // Assert
        assertEquals(ImageFormat.WEBP, instruction.type)
        assertEquals(75, instruction.quality)
        assertEquals(100, instruction.maxPixelDimension)
    }

    @Test
    fun thumbnailInstruction_withSpecificType_preservesType() {
        // Arrange & Act
        val instruction =
                ThumbnailInstruction(
                        quality = 85,
                        maxPixelDimension = 200,
                        type = ImageFormat.PNG,
                        maxBytes = 10000
                )

        // Assert
        assertEquals(ImageFormat.PNG, instruction.type)
        assertEquals(85, instruction.quality)
        assertEquals(200, instruction.maxPixelDimension)
    }

    @Test
    fun thumbnailFile_properties_areCorrectlySet() {
        // Arrange
        val payload = byteArrayOf(1, 2, 3, 4, 5)
        val thumbnail =
                ThumbnailFile(
                        pixelWidth = 100,
                        pixelHeight = 200,
                        payload = payload,
                        contentType = "image/jpeg",
                        key = "test-key",
                        quality = 100
                )

        // Assert
        assertEquals(100, thumbnail.imageSize.pixelWidth)
        assertEquals(200, thumbnail.imageSize.pixelHeight)
        assertContentEquals(payload, thumbnail.payload)
        assertEquals("image/jpeg", thumbnail.contentType)
        assertEquals("test-key", thumbnail.key)
    }

    @Test
    fun embeddedThumb_properties_areCorrectlySet() {
        // Arrange
        val embeddedThumb =
                EmbeddedThumb(
                        pixelWidth = 200,
                        pixelHeight = 100,
                        contentType = "image/jpeg",
                        content = "base64content"
                )

        // Assert
        assertEquals(200, embeddedThumb.pixelWidth)
        assertEquals(100, embeddedThumb.pixelHeight)
        assertEquals("image/jpeg", embeddedThumb.contentType)
        assertEquals("base64content", embeddedThumb.content)
    }
}
