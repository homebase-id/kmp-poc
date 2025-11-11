package id.homebase.homebasekmppoc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlatformTest {

    @Test
    fun example() {
        assertEquals(3, 1 + 2)
    }

    @Test
    fun testGenerateUuidBytes() {
        val uuidBytes = generateUuidBytes()

        // UUID byte array should be 16 bytes
        assertEquals(16, uuidBytes.size)

        // Should not be null
        assertNotNull(uuidBytes)

        // Basic check that it's not all zeros (unlikely for random UUID)
        assertTrue(uuidBytes.any { it != 0.toByte() })
    }
}