package id.homebase.homebasekmppoc.prototype.lib.image

import kotlin.test.*

/**
 * Common tests for ImageUtils that run on all platforms (Android, iOS, Desktop)
 *
 * Note: Android tests require Robolectric to provide Android framework APIs in JVM tests.
 * This is configured in the platform-specific test source sets.
 *
 * The class is abstract and platform-specific subclasses provide the test runners,
 * otherwise Robolectric would try to run this directly and fail.
 */
abstract class ImageUtilsTest {

    @Test
    fun getNaturalSize_withValidJpegImage_returnsCorrectSize() {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.jpg")

        // Act
        val size = ImageUtils.getNaturalSize(imageData)

        // Assert
        assertTrue(size.pixelWidth > 0, "Width should be greater than 0")
        assertTrue(size.pixelHeight > 0, "Height should be greater than 0")
    }

    @Test
    fun getNaturalSize_withValidPngImage_returnsCorrectSize() {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.png")

        // Act
        val size = ImageUtils.getNaturalSize(imageData)

        // Assert
        assertTrue(size.pixelWidth > 0, "Width should be greater than 0")
        assertTrue(size.pixelHeight > 0, "Height should be greater than 0")
    }

    @Test
    fun getNaturalSize_withValidWebpImage_returnsCorrectSize() {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.webp")

        // Act
        val size = ImageUtils.getNaturalSize(imageData)

        // Assert
        assertTrue(size.pixelWidth > 0, "Width should be greater than 0")
        assertTrue(size.pixelHeight > 0, "Height should be greater than 0")
    }

    @Test
    fun resizePreserveAspect_withValidImage_returnsResult() {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.jpg")

        // Act
        val result = ImageUtils.resizePreserveAspect(
            srcBytes = imageData,
            maxWidth = 100,
            maxHeight = 100,
            outputFormat = ImageFormat.JPEG,
            quality = 76
        )

        // Assert
        assertNotNull(result)
        assertNotNull(result.naturalSize)
        assertNotNull(result.size)
        assertNotNull(result.bytes)
        assertTrue(result.bytes.isNotEmpty(), "Result bytes should not be empty")
    }

    @Test
    fun resizePreserveAspect_withSpecificDimensions_resizesCorrectly() {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.png")
        val maxWidth = 50
        val maxHeight = 50

        // Act
        val result = ImageUtils.resizePreserveAspect(
            srcBytes = imageData,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            outputFormat = ImageFormat.PNG,
            quality = 90
        )

        // Assert
        assertNotNull(result)
        assertTrue(result.size.pixelWidth <= maxWidth, "Width should be <= $maxWidth")
        assertTrue(result.size.pixelHeight <= maxHeight, "Height should be <= $maxHeight")
        assertTrue(result.bytes.isNotEmpty(), "Result bytes should not be empty")
    }

    @Test
    fun resizePreserveAspect_maintainsAspectRatio() {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.jpg")
        val maxWidth = 100
        val maxHeight = 200

        // Act
        val result = ImageUtils.resizePreserveAspect(
            srcBytes = imageData,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            outputFormat = ImageFormat.JPEG,
            quality = 80
        )

        // Assert
        assertNotNull(result)
        assertTrue(result.size.pixelWidth <= maxWidth, "Width should be <= $maxWidth")
        assertTrue(result.size.pixelHeight <= maxHeight, "Height should be <= $maxHeight")

        // Check that at least one dimension uses the full constraint
        val usesFullConstraint = result.size.pixelWidth == maxWidth || result.size.pixelHeight == maxHeight
        assertTrue(usesFullConstraint, "At least one dimension should use the full constraint to maintain aspect ratio")
    }

    @Test
    fun resizePreserveAspect_withDifferentFormats_producesCorrectOutput() {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.jpg")
        val formats = listOf(ImageFormat.JPEG, ImageFormat.PNG, ImageFormat.WEBP)

        formats.forEach { format ->
            // Act
            val result = ImageUtils.resizePreserveAspect(
                srcBytes = imageData,
                maxWidth = 100,
                maxHeight = 100,
                outputFormat = format,
                quality = 76
            )

            // Assert
            assertNotNull(result, "Result should not be null for format: $format")
            assertNotNull(result.bytes, "Bytes should not be null for format: $format")
            assertTrue(result.bytes.isNotEmpty(), "Bytes should not be empty for format: $format")
        }
    }

    @Test
    fun resizePreserveAspect_withDifferentQualityLevels_producesValidResults() {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.jpg")
        val qualityLevels = listOf(1, 25, 50, 75, 100)

        qualityLevels.forEach { quality ->
            // Act
            val result = ImageUtils.resizePreserveAspect(
                srcBytes = imageData,
                maxWidth = 100,
                maxHeight = 100,
                outputFormat = ImageFormat.JPEG,
                quality = quality
            )

            // Assert
            assertNotNull(result, "Result should not be null for quality: $quality")
            assertNotNull(result.bytes, "Bytes should not be null for quality: $quality")
            assertTrue(result.bytes.isNotEmpty(), "Bytes should not be empty for quality: $quality")
        }
    }

    @Test
    fun compressOnly_withValidImage_returnsCompressedResult() {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.jpg")

        // Act
        val result = ImageUtils.compressOnly(
            srcBytes = imageData,
            outputFormat = ImageFormat.JPEG,
            quality = 50
        )

        // Assert
        assertNotNull(result)
        assertTrue(result.bytes.isNotEmpty(), "Compressed bytes should not be empty")
        assertEquals(result.naturalSize.pixelWidth, result.size.pixelWidth, "Width should remain the same")
        assertEquals(result.naturalSize.pixelHeight, result.size.pixelHeight, "Height should remain the same")
    }

    @Test
    fun compressOnly_lowerQuality_producesSmallerFile() {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.jpg")

        // Act
        val highQuality = ImageUtils.compressOnly(imageData, ImageFormat.JPEG, 100)
        val lowQuality = ImageUtils.compressOnly(imageData, ImageFormat.JPEG, 10)

        // Assert
        assertTrue(
            lowQuality.bytes.size < highQuality.bytes.size,
            "Lower quality should produce smaller file size. High: ${highQuality.bytes.size}, Low: ${lowQuality.bytes.size}"
        )
    }

    @Test
    fun crop_withValidCoordinates_cropsProperly() {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.jpg")
        val naturalSize = ImageUtils.getNaturalSize(imageData)

        // Ensure we have enough space to crop
        val cropWidth = minOf(50, naturalSize.pixelWidth / 2)
        val cropHeight = minOf(50, naturalSize.pixelHeight / 2)

        // Act
        val result = ImageUtils.crop(
            srcBytes = imageData,
            x = 0,
            y = 0,
            width = cropWidth,
            height = cropHeight,
            outputFormat = ImageFormat.JPEG,
            quality = 90
        )

        // Assert
        assertNotNull(result)
        assertEquals(cropWidth, result.size.pixelWidth, "Cropped width should match requested width")
        assertEquals(cropHeight, result.size.pixelHeight, "Cropped height should match requested height")
        assertTrue(result.bytes.isNotEmpty(), "Cropped image bytes should not be empty")
    }

    @Test
    fun rotate_by90Degrees_rotatesCorrectly() {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.jpg")
        val originalSize = ImageUtils.getNaturalSize(imageData)

        // Act
        val result = ImageUtils.rotate(
            srcBytes = imageData,
            degrees = 90,
            outputFormat = ImageFormat.JPEG,
            quality = 90
        )

        // Assert
        assertNotNull(result)
        assertTrue(result.bytes.isNotEmpty(), "Rotated image bytes should not be empty")
        // After 90-degree rotation, width and height should swap (if not square)
        if (originalSize.pixelWidth != originalSize.pixelHeight) {
            assertEquals(originalSize.pixelWidth, result.size.pixelHeight, "Width should become height after 90° rotation")
            assertEquals(originalSize.pixelHeight, result.size.pixelWidth, "Height should become width after 90° rotation")
        }
    }

    @Test
    fun rotate_by180Degrees_maintainsDimensions() {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.png")
        val originalSize = ImageUtils.getNaturalSize(imageData)

        // Act
        val result = ImageUtils.rotate(
            srcBytes = imageData,
            degrees = 180,
            outputFormat = ImageFormat.PNG,
            quality = 90
        )

        // Assert
        assertNotNull(result)
        assertTrue(result.bytes.isNotEmpty(), "Rotated image bytes should not be empty")
        assertEquals(originalSize.pixelWidth, result.size.pixelWidth, "Width should remain the same after 180° rotation")
        assertEquals(originalSize.pixelHeight, result.size.pixelHeight, "Height should remain the same after 180° rotation")
    }

    @Test
    fun rotate_by270Degrees_rotatesCorrectly() {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.jpg")
        val originalSize = ImageUtils.getNaturalSize(imageData)

        // Act
        val result = ImageUtils.rotate(
            srcBytes = imageData,
            degrees = 270,
            outputFormat = ImageFormat.JPEG,
            quality = 85
        )

        // Assert
        assertNotNull(result)
        assertTrue(result.bytes.isNotEmpty(), "Rotated image bytes should not be empty")
        // After 270-degree rotation, width and height should swap (if not square)
        if (originalSize.pixelWidth != originalSize.pixelHeight) {
            assertEquals(originalSize.pixelWidth, result.size.pixelHeight, "Width should become height after 270° rotation")
            assertEquals(originalSize.pixelHeight, result.size.pixelWidth, "Height should become width after 270° rotation")
        }
    }

    @Test
    fun imageSize_recordProperties_areCorrectlySet() {
        // Arrange & Act
        val size = ImageSize(pixelWidth = 100, pixelHeight = 200)

        // Assert
        assertEquals(100, size.pixelWidth)
        assertEquals(200, size.pixelHeight)
    }

    @Test
    fun imageResult_recordProperties_areCorrectlySet() {
        // Arrange
        val naturalSize = ImageSize(200, 300)
        val targetSize = ImageSize(100, 150)
        val data = byteArrayOf(1, 2, 3, 4, 5)

        // Act
        val result = ImageResult(
            bytes = data,
            naturalSize = naturalSize,
            size = targetSize
        )

        // Assert
        assertEquals(naturalSize, result.naturalSize)
        assertEquals(targetSize, result.size)
        assertContentEquals(data, result.bytes)
    }

    @Test
    fun calculateTargetDimensions_preservesAspectRatio() {
        // Test landscape image
        val (targetW1, targetH1) = calculateTargetDimensions(
            naturalW = 800,
            naturalH = 600,
            maxWidth = 400,
            maxHeight = 400
        )
        assertEquals(400, targetW1)
        assertEquals(300, targetH1)

        // Test portrait image
        val (targetW2, targetH2) = calculateTargetDimensions(
            naturalW = 600,
            naturalH = 800,
            maxWidth = 400,
            maxHeight = 400
        )
        assertEquals(300, targetW2)
        assertEquals(400, targetH2)
    }

    @Test
    fun calculateTargetDimensions_noResizeWhenAlreadySmaller() {
        // Arrange
        val naturalW = 50
        val naturalH = 50

        // Act
        val (targetW, targetH) = calculateTargetDimensions(
            naturalW = naturalW,
            naturalH = naturalH,
            maxWidth = 100,
            maxHeight = 100
        )

        // Assert
        assertEquals(naturalW, targetW)
        assertEquals(naturalH, targetH)
    }

    @Test
    fun toImageBitmap_withValidJpeg_returnsImageBitmap() {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.jpg")

        // Act
        val bitmap = imageData.toImageBitmap()

        // Assert
        assertNotNull(bitmap, "Bitmap should not be null for valid JPEG")
    }

    @Test
    fun toImageBitmap_withValidPng_returnsImageBitmap() {
        // Arrange
        val imageData = loadTestImageOrSkip("sample.png")

        // Act
        val bitmap = imageData.toImageBitmap()

        // Assert
        assertNotNull(bitmap, "Bitmap should not be null for valid PNG")
    }
}

