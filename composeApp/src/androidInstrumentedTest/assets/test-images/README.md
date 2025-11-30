# Test Images Directory

This directory contains sample images used for testing ImageUtils across all platforms.

## Required Test Images

Please add the following test images to this directory:

1. **sample.jpg** - A JPEG image (any resolution, recommended: 800x600 or similar)
2. **sample.png** - A PNG image with transparency
3. **sample.webp** - A WebP image
4. **sample.gif** - An animated or static GIF
5. **sample.bmp** - A BMP image (optional, as fallback)

## Image Requirements

- Images should be reasonably sized (not too large, e.g., under 2MB)
- Include images with different aspect ratios for better testing
- Recommended to have at least one landscape and one portrait image

## How to Add Images

Simply copy your test images into this directory with the exact names listed above, or update the test filenames in `ImageUtilsTest.kt` to match your image names.

## Sharing Across Platforms

These images are located in `commonTest/resources` which means they are shared across:
- Android tests (`androidUnitTest`)
- iOS tests (`iosTest`)
- Desktop tests (`desktopTest`)

This avoids duplication while ensuring all platform tests use the same test data.

