package id.homebase.homebasekmppoc.prototype.lib.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SecureByteArrayTest {

    // ========================================================================
    // Basic Creation and Copying Tests
    // ========================================================================

    @Test
    fun testCreation_EmptyArray() {
        val sba = SecureByteArray(ByteArray(0))
        assertEquals(0, sba.unsafeBytes.size)
        assertEquals(0, sba.toByteArray().size)
    }

    @Test
    fun testCreation_WithData() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val sba = SecureByteArray(data)

        assertEquals(5, sba.unsafeBytes.size)
        assertEquals(1, sba.unsafeBytes[0])
        assertEquals(5, sba.unsafeBytes[4])
    }

    @Test
    fun testToByteArray_ReturnsCopy() {
        val original = byteArrayOf(10, 20, 30)
        val sba = SecureByteArray(original)

        val copy = sba.toByteArray()

        // Content should be equal
        assertTrue(copy.contentEquals(original))

        // But it should be a different array instance
        assertNotSame(copy, sba.unsafeBytes)

        // Modifying the copy should not affect the original
        copy[0] = 99
        assertEquals(10, sba.unsafeBytes[0])
    }

    @Test
    fun testUnsafeBytes_ReturnsSameReference() {
        val data = byteArrayOf(1, 2, 3)
        val sba = SecureByteArray(data)

        val ref1 = sba.unsafeBytes
        val ref2 = sba.unsafeBytes

        // Both should be the same reference
        assertSame(ref1, ref2)

        // Modifying through unsafeBytes affects the internal state
        ref1[0] = 99
        assertEquals(99, sba.unsafeBytes[0])
    }

    // ========================================================================
    // Clear Functionality Tests
    // ========================================================================

    @Test
    fun testClear_ZeroesOutAllBytes() {
        val sba = SecureByteArray(byteArrayOf(1, 2, 3, 4, 5))

        sba.clear()

        for (i in sba.unsafeBytes.indices) {
            assertEquals(0, sba.unsafeBytes[i], "Byte at index $i should be zero")
        }
    }

    @Test
    fun testClear_EmptyArray() {
        val sba = SecureByteArray(ByteArray(0))

        // Should not throw
        sba.clear()

        assertEquals(0, sba.unsafeBytes.size)
    }

    @Test
    fun testClear_AffectsInternalState() {
        val sba = SecureByteArray(byteArrayOf(100, -50, 127))

        sba.clear()

        val copy = sba.toByteArray()
        assertTrue(copy.all { it == 0.toByte() })
    }

    // ========================================================================
    // Equality Tests
    // ========================================================================

    @Test
    fun testEquals_SameContent() {
        val sba1 = SecureByteArray(byteArrayOf(1, 2, 3))
        val sba2 = SecureByteArray(byteArrayOf(1, 2, 3))

        assertEquals(sba1, sba2)
        assertTrue(sba1 == sba2)
    }

    @Test
    fun testEquals_DifferentContent() {
        val sba1 = SecureByteArray(byteArrayOf(1, 2, 3))
        val sba2 = SecureByteArray(byteArrayOf(1, 2, 4))

        assertNotEquals(sba1, sba2)
        assertFalse(sba1 == sba2)
    }

    @Test
    fun testEquals_DifferentSizes() {
        val sba1 = SecureByteArray(byteArrayOf(1, 2, 3))
        val sba2 = SecureByteArray(byteArrayOf(1, 2))

        assertNotEquals(sba1, sba2)
    }

    @Test
    fun testEquals_EmptyArrays() {
        val sba1 = SecureByteArray(ByteArray(0))
        val sba2 = SecureByteArray(ByteArray(0))

        assertEquals(sba1, sba2)
    }

    @Test
    fun testEquals_ReferentialEquality() {
        val sba = SecureByteArray(byteArrayOf(1, 2, 3))

        assertEquals(sba, sba)
        assertTrue(sba === sba)
    }

    @Test
    fun testEquals_WithNull() {
        val sba = SecureByteArray(byteArrayOf(1, 2, 3))

        assertFalse(sba.equals(null))
    }

    @Test
    fun testEquals_WithDifferentType() {
        val sba = SecureByteArray(byteArrayOf(1, 2, 3))
        val list = listOf(1, 2, 3)

        assertFalse(sba.equals(list))
    }

    @Test
    fun testEquals_NegativeBytes() {
        val sba1 = SecureByteArray(byteArrayOf(-128, -1, 0, 1, 127))
        val sba2 = SecureByteArray(byteArrayOf(-128, -1, 0, 1, 127))

        assertEquals(sba1, sba2)
    }

    // ========================================================================
    // HashCode Tests
    // ========================================================================

    @Test
    fun testHashCode_SameContent() {
        val sba1 = SecureByteArray(byteArrayOf(1, 2, 3))
        val sba2 = SecureByteArray(byteArrayOf(1, 2, 3))

        assertEquals(sba1.hashCode(), sba2.hashCode())
    }

    @Test
    fun testHashCode_DifferentContent() {
        val sba1 = SecureByteArray(byteArrayOf(1, 2, 3))
        val sba2 = SecureByteArray(byteArrayOf(3, 2, 1))

        // Not guaranteed to be different, but very likely
        assertNotEquals(sba1.hashCode(), sba2.hashCode())
    }

    @Test
    fun testHashCode_EmptyArray() {
        val sba1 = SecureByteArray(ByteArray(0))
        val sba2 = SecureByteArray(ByteArray(0))

        assertEquals(sba1.hashCode(), sba2.hashCode())
    }

    @Test
    fun testHashCode_ConsistentOverMultipleCalls() {
        val sba = SecureByteArray(byteArrayOf(10, 20, 30, 40))

        val hash1 = sba.hashCode()
        val hash2 = sba.hashCode()
        val hash3 = sba.hashCode()

        assertEquals(hash1, hash2)
        assertEquals(hash2, hash3)
    }

    @Test
    fun testHashCode_EqualObjectsSameHashCode() {
        val sba1 = SecureByteArray(byteArrayOf(-50, 0, 100))
        val sba2 = SecureByteArray(byteArrayOf(-50, 0, 100))

        // If equals, hashCode must be equal
        assertTrue(sba1 == sba2)
        assertEquals(sba1.hashCode(), sba2.hashCode())
    }

    @Test
    fun testHashCode_AfterClear() {
        val sba1 = SecureByteArray(byteArrayOf(1, 2, 3))
        val sba2 = SecureByteArray(byteArrayOf(0, 0, 0))

        sba1.clear()

        // After clearing, content is all zeros, so hash should match
        assertEquals(sba1.hashCode(), sba2.hashCode())
    }

    // ========================================================================
    // ToString Tests
    // ========================================================================

    @Test
    fun testToString_DoesNotLeakContent() {
        val secretData = byteArrayOf(1, 2, 3, 4, 5)
        val sba = SecureByteArray(secretData)

        val stringRep = sba.toString()

        // Should not contain the actual byte values
        assertFalse(stringRep.contains("1"))
        assertFalse(stringRep.contains("2"))
        assertFalse(stringRep.contains("secretData"))

        // Should contain size information
        assertTrue(stringRep.contains("5"))
        assertTrue(stringRep.contains("SecureByteArray"))
    }

    @Test
    fun testToString_ShowsCorrectSize() {
        val sba1 = SecureByteArray(ByteArray(0))
        val sba2 = SecureByteArray(ByteArray(10))
        val sba3 = SecureByteArray(ByteArray(256))

        assertTrue(sba1.toString().contains("size=0"))
        assertTrue(sba2.toString().contains("size=10"))
        assertTrue(sba3.toString().contains("size=256"))
    }

    // ========================================================================
    // Edge Cases and Integration Tests
    // ========================================================================

    @Test
    fun testLargeArray() {
        val largeData = ByteArray(10_000) { it.toByte() }
        val sba = SecureByteArray(largeData)

        assertEquals(10_000, sba.unsafeBytes.size)

        val copy = sba.toByteArray()
        assertTrue(copy.contentEquals(largeData))

        sba.clear()
        assertTrue(sba.unsafeBytes.all { it == 0.toByte() })
    }

    @Test
    fun testSingleByteArray() {
        val sba = SecureByteArray(byteArrayOf(42))

        assertEquals(1, sba.unsafeBytes.size)
        assertEquals(42, sba.unsafeBytes[0])

        val copy = sba.toByteArray()
        assertEquals(1, copy.size)
        assertEquals(42, copy[0])
    }

    @Test
    fun testAllByteValues() {
        // Test all possible byte values from -128 to 127
        val allBytes = ByteArray(256) { (it - 128).toByte() }
        val sba = SecureByteArray(allBytes)

        assertEquals(256, sba.unsafeBytes.size)

        for (i in 0 until 256) {
            assertEquals((i - 128).toByte(), sba.unsafeBytes[i])
        }
    }

    @Test
    fun testHashCodeDistribution() {
        // Test that different arrays produce different hash codes (usually)
        val hashes = mutableSetOf<Int>()

        for (i in 0 until 100) {
            val data = ByteArray(10) { (i * it).toByte() }
            val sba = SecureByteArray(data)
            hashes.add(sba.hashCode())
        }

        // Should have a good distribution (at least 90 different hashes out of 100)
        assertTrue(hashes.size >= 90, "Expected at least 90 unique hashes, got ${hashes.size}")
    }

    @Test
    fun testEqualsAndHashCode_Contract() {
        val sba1 = SecureByteArray(byteArrayOf(5, 10, 15))
        val sba2 = SecureByteArray(byteArrayOf(5, 10, 15))
        val sba3 = SecureByteArray(byteArrayOf(5, 10, 15))

        // Reflexive: x.equals(x) is true
        assertEquals(sba1, sba1)

        // Symmetric: x.equals(y) implies y.equals(x)
        assertEquals(sba1, sba2)
        assertEquals(sba2, sba1)

        // Transitive: x.equals(y) and y.equals(z) implies x.equals(z)
        assertEquals(sba1, sba2)
        assertEquals(sba2, sba3)
        assertEquals(sba1, sba3)

        // Consistent hash codes
        assertEquals(sba1.hashCode(), sba2.hashCode())
        assertEquals(sba2.hashCode(), sba3.hashCode())
    }

    @Test
    fun testClearAndEquals() {
        val sba1 = SecureByteArray(byteArrayOf(1, 2, 3))
        val sba2 = SecureByteArray(byteArrayOf(0, 0, 0))

        assertNotEquals(sba1, sba2)

        sba1.clear()

        assertEquals(sba1, sba2)
    }

    @Test
    fun testMultipleCopiesIndependent() {
        val sba = SecureByteArray(byteArrayOf(10, 20, 30))

        val copy1 = sba.toByteArray()
        val copy2 = sba.toByteArray()

        // Copies should be equal to each other
        assertTrue(copy1.contentEquals(copy2))

        // But independent
        copy1[0] = 99
        assertEquals(20, copy2[1]) // copy2 unchanged
        assertEquals(10, sba.unsafeBytes[0]) // sba unchanged
    }
}
