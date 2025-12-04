package id.homebase.homebasekmppoc.prototype.lib.image

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Android-specific test runner for ThumbnailGenerator using Robolectric
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ThumbnailGeneratorAndroidTest : ThumbnailGeneratorTest()


