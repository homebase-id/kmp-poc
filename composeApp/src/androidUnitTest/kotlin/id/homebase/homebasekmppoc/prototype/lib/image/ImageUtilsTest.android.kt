package id.homebase.homebasekmppoc.prototype.lib.image

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Android-specific test runner using Robolectric
 * This allows Android framework APIs (like BitmapFactory) to work in JVM unit tests
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ImageUtilsAndroidTest : ImageUtilsTest()


