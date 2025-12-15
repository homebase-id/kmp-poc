package id.homebase.homebasekmppoc.lib.core

import androidx.activity.ComponentActivity
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Unit tests for ActivityProvider using Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class ActivityProviderTest {

    @Test
    fun testInitialize_storesActivity() {
        // Clear any previous state
        ActivityProvider.clear()

        val activity = Robolectric.buildActivity(ComponentActivity::class.java).create().get()
        ActivityProvider.initialize(activity)

        val retrieved = ActivityProvider.getActivity()
        assertNotNull(retrieved, "Activity should be retrievable after initialization")
        assertEquals(activity, retrieved, "Retrieved activity should match initialized activity")
    }

    @Test
    fun testGetActivity_beforeInitialize_returnsNull() {
        ActivityProvider.clear()

        val activity = ActivityProvider.getActivity()
        assertNull(activity, "getActivity should return null before initialization")
    }

    @Test
    fun testRequireActivity_afterInitialize_returnsActivity() {
        ActivityProvider.clear()

        val activity = Robolectric.buildActivity(ComponentActivity::class.java).create().get()
        ActivityProvider.initialize(activity)

        val retrieved = ActivityProvider.requireActivity()
        assertEquals(activity, retrieved, "requireActivity should return the initialized activity")
    }

    @Test
    fun testRequireActivity_beforeInitialize_throws() {
        ActivityProvider.clear()

        assertFailsWith<IllegalStateException>(
                message = "requireActivity should throw when not initialized"
        ) { ActivityProvider.requireActivity() }
    }

    @Test
    fun testClear_removesActivity() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).create().get()
        ActivityProvider.initialize(activity)

        ActivityProvider.clear()

        val retrieved = ActivityProvider.getActivity()
        assertNull(retrieved, "getActivity should return null after clear")
    }
}
