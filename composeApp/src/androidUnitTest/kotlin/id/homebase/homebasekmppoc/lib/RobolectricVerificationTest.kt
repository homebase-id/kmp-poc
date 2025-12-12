package id.homebase.homebasekmppoc.lib

import android.os.Build
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verification test to ensure Robolectric is properly configured and running.
 * This test will FAIL if Robolectric is not available or not properly set up.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class RobolectricVerificationTest {

    @Test
    fun verifyRobolectricIsRunning() {
        // This test verifies that Robolectric is actually running by checking Android SDK properties
        // that would not be available in a standard JVM test

        // Verify we can access Android Build information
        assertNotNull(Build.VERSION.SDK_INT, "Build.VERSION.SDK_INT should not be null with Robolectric")
        assertEquals(33, Build.VERSION.SDK_INT, "SDK version should be 33 as configured in @Config")

        // Verify Build.MANUFACTURER is set (Robolectric default is "robolectric")
        assertNotNull(Build.MANUFACTURER, "Build.MANUFACTURER should not be null with Robolectric")
        assertTrue(
            Build.MANUFACTURER.isNotEmpty(),
            "Build.MANUFACTURER should not be empty with Robolectric"
        )

        println("✅ Robolectric verification PASSED")
        println("   SDK Version: ${Build.VERSION.SDK_INT}")
        println("   Manufacturer: ${Build.MANUFACTURER}")
        println("   Model: ${Build.MODEL}")
    }

    @Test
    fun verifyAndroidFrameworkAvailable() {
        // Verify that Android framework classes are available
        // This would fail without Robolectric in a standard JVM test
        try {
            val context = RuntimeEnvironment.getApplication()
            assertNotNull(context, "Application context should be available with Robolectric")
            assertNotNull(context.packageName, "Package name should be available")

            println("✅ Android framework verification PASSED")
            println("   Application context available: true")
            println("   Package name: ${context.packageName}")
        } catch (e: Exception) {
            throw AssertionError("Failed to access Android framework - Robolectric may not be running", e)
        }
    }
}