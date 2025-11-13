package id.homebase.homebasekmppoc

import id.homebase.homebasekmppoc.youauth.buildAuthorizeUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

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

    @Test
    fun testEncodeUrl_simpleText() {
        val input = "hello"
        val expected = "hello"
        assertEquals(expected, encodeUrl(input))
    }

    @Test
    fun testEncodeUrl_withSpaces() {
        val input = "hello world"
        val expected = "hello%20world"
        assertEquals(expected, encodeUrl(input))
    }

    @Test
    fun testEncodeUrl_withSpecialChars() {
        val input = "hello@example.com"
        val expected = "hello%40example.com"
        assertEquals(expected, encodeUrl(input))
    }

    @Test
    fun testEncodeUrl_unreservedChars() {
        val input = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
        // All unreserved characters should remain unchanged
        assertEquals(input, encodeUrl(input))
    }

    @Test
    fun testEncodeUrl_complexString() {
        val input = "key=value&foo=bar baz"
        val expected = "key%3Dvalue%26foo%3Dbar%20baz"
        assertEquals(expected, encodeUrl(input))
    }

    @Test
    fun testEncodeUrl_utf8Characters() {
        val input = "hello 世界"
        val expected = "hello%20%E4%B8%96%E7%95%8C"
        assertEquals(expected, encodeUrl(input))
    }

    @Test
    fun testDecodeUrl_simpleText() {
        val input = "hello"
        val expected = "hello"
        assertEquals(expected, decodeUrl(input))
    }

    @Test
    fun testDecodeUrl_withSpaces() {
        val input = "hello%20world"
        val expected = "hello world"
        assertEquals(expected, decodeUrl(input))
    }

    @Test
    fun testDecodeUrl_withPlusAsSpace() {
        val input = "hello+world"
        val expected = "hello world"
        assertEquals(expected, decodeUrl(input))
    }

    @Test
    fun testDecodeUrl_withSpecialChars() {
        val input = "hello%40example.com"
        val expected = "hello@example.com"
        assertEquals(expected, decodeUrl(input))
    }

    @Test
    fun testDecodeUrl_complexString() {
        val input = "key%3Dvalue%26foo%3Dbar%20baz"
        val expected = "key=value&foo=bar baz"
        assertEquals(expected, decodeUrl(input))
    }

    @Test
    fun testDecodeUrl_utf8Characters() {
        val input = "hello%20%E4%B8%96%E7%95%8C"
        val expected = "hello 世界"
        assertEquals(expected, decodeUrl(input))
    }

    @Test
    fun testDecodeUrl_invalidPercentSequence() {
        val input = "hello%2"
        assertFailsWith<IllegalArgumentException> {
            decodeUrl(input)
        }
    }

    @Test
    fun testDecodeUrl_invalidHexCharacters() {
        val input = "hello%ZZ"
        assertFailsWith<IllegalArgumentException> {
            decodeUrl(input)
        }
    }

    @Test
    fun testEncodeDecodeRoundtrip() {
        val testStrings = listOf(
            "simple text",
            "hello@example.com",
            "key=value&foo=bar",
            "hello 世界",
            "special!@#$%^&*()chars",
            "path/to/resource?query=value"
        )

        testStrings.forEach { original ->
            val encoded = encodeUrl(original)
            val decoded = decodeUrl(encoded)
            assertEquals(original, decoded, "Roundtrip failed for: $original")
        }
    }
}