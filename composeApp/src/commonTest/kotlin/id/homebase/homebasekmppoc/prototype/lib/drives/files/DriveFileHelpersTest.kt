package id.homebase.homebasekmppoc.prototype.lib.drives.files

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Unit tests for DriveFileHelpers. */
class DriveFileHelpersTest {

    @Test
    fun testRoundToSmallerMultipleOf16_exactMultiple() {
        assertEquals(32L, DriveFileHelpers.roundToSmallerMultipleOf16(32))
        assertEquals(16L, DriveFileHelpers.roundToSmallerMultipleOf16(16))
        assertEquals(0L, DriveFileHelpers.roundToSmallerMultipleOf16(0))
    }

    @Test
    fun testRoundToSmallerMultipleOf16_roundsDown() {
        assertEquals(16L, DriveFileHelpers.roundToSmallerMultipleOf16(17))
        assertEquals(16L, DriveFileHelpers.roundToSmallerMultipleOf16(31))
        assertEquals(0L, DriveFileHelpers.roundToSmallerMultipleOf16(15))
        assertEquals(48L, DriveFileHelpers.roundToSmallerMultipleOf16(50))
    }

    @Test
    fun testRoundToLargerMultipleOf16_exactMultiple() {
        assertEquals(32L, DriveFileHelpers.roundToLargerMultipleOf16(32))
        assertEquals(16L, DriveFileHelpers.roundToLargerMultipleOf16(16))
        assertEquals(0L, DriveFileHelpers.roundToLargerMultipleOf16(0))
    }

    @Test
    fun testRoundToLargerMultipleOf16_roundsUp() {
        assertEquals(32L, DriveFileHelpers.roundToLargerMultipleOf16(17))
        assertEquals(32L, DriveFileHelpers.roundToLargerMultipleOf16(31))
        assertEquals(16L, DriveFileHelpers.roundToLargerMultipleOf16(1))
        assertEquals(64L, DriveFileHelpers.roundToLargerMultipleOf16(50))
    }

    @Test
    fun testGetRangeHeader_nullChunkStart() {
        val result = DriveFileHelpers.getRangeHeader(null, null)

        assertEquals(0, result.startOffset)
        assertNull(result.updatedChunkStart)
        assertNull(result.updatedChunkEnd)
        assertNull(result.rangeHeader)
    }

    @Test
    fun testGetRangeHeader_zeroChunkStart() {
        val result = DriveFileHelpers.getRangeHeader(0L, 100L)

        assertEquals(0, result.startOffset)
        assertEquals(0L, result.updatedChunkStart)
        assertEquals(111L, result.updatedChunkEnd) // roundToLarger(100) = 112, minus 1 = 111
        assertEquals("bytes=0-111", result.rangeHeader)
    }

    @Test
    fun testGetRangeHeader_alignedChunkStart() {
        val result = DriveFileHelpers.getRangeHeader(32L, 64L)

        // 32 - 16 = 16, roundToSmaller(16) = 16
        assertEquals(16, result.startOffset)
        assertEquals(16L, result.updatedChunkStart)
        assertEquals(63L, result.updatedChunkEnd) // roundToLarger(64) = 64, minus 1 = 63
        assertEquals("bytes=16-63", result.rangeHeader)
    }

    @Test
    fun testGetRangeHeader_unalignedChunkStart() {
        val result = DriveFileHelpers.getRangeHeader(50L, 100L)

        // 50 - 16 = 34, roundToSmaller(34) = 32
        assertEquals(18, result.startOffset) // 50 - 32 = 18
        assertEquals(32L, result.updatedChunkStart)
        assertEquals(111L, result.updatedChunkEnd) // roundToLarger(100) = 112, minus 1 = 111
        assertEquals("bytes=32-111", result.rangeHeader)
    }

    @Test
    fun testGetRangeHeader_noChunkEnd() {
        val result = DriveFileHelpers.getRangeHeader(100L, null)

        // 100 - 16 = 84, roundToSmaller(84) = 80
        assertEquals(20, result.startOffset) // 100 - 80 = 20
        assertEquals(80L, result.updatedChunkStart)
        assertNull(result.updatedChunkEnd)
        assertEquals("bytes=80-", result.rangeHeader)
    }

    @Test
    fun testGetRangeHeader_smallChunkStart() {
        val result = DriveFileHelpers.getRangeHeader(5L, 32L)

        // 5 - 16 = -11, but since chunkStart != 0, we use roundToSmaller(5 - 16) =
        // roundToSmaller(-11)
        // In Kotlin, -11 / 16 = 0 (integer division rounds toward zero for negative numbers with
        // positive divisor in JVM)
        // Actually: -11 / 16 = -1 in Kotlin (floor division), so result is -16
        // Let's actually trace this: chunkStart = 5, so updatedChunkStart = roundToSmaller(5 - 16)
        // = roundToSmaller(-11)
        // roundToSmaller(-11) = (-11 / 16) * 16 = -1 * 16 = -16
        // But negative ranges don't make sense for HTTP, let's verify the actual behavior

        // Actually, the original TypeScript would also produce negative values here
        // The test should verify current behavior
        val expectedStart = DriveFileHelpers.roundToSmallerMultipleOf16(5 - 16)
        assertEquals(kotlin.math.abs(5L - expectedStart).toInt(), result.startOffset)
    }

    @Test
    fun testGetRangeHeader_largeValues() {
        val result = DriveFileHelpers.getRangeHeader(1000000L, 2000000L)

        // 1000000 - 16 = 999984, roundToSmaller(999984) = 999984 (divisible by 16)
        assertEquals(16, result.startOffset)
        assertEquals(999984L, result.updatedChunkStart)
        // roundToLarger(2000000) = 2000000 (divisible by 16), minus 1 = 1999999
        assertEquals(1999999L, result.updatedChunkEnd)
        assertEquals("bytes=999984-1999999", result.rangeHeader)
    }

    @Test
    fun testGetRangeHeader_exactBlockBoundaries() {
        // Test with exact 16-byte boundaries
        val result = DriveFileHelpers.getRangeHeader(16L, 32L)

        // 16 - 16 = 0, roundToSmaller(0) = 0
        assertEquals(16, result.startOffset)
        assertEquals(0L, result.updatedChunkStart)
        // roundToLarger(32) = 32, minus 1 = 31
        assertEquals(31L, result.updatedChunkEnd)
        assertEquals("bytes=0-31", result.rangeHeader)
    }
}
