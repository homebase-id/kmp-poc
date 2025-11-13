package id.homebase.homebasekmppoc

import id.homebase.homebasekmppoc.youauth.buildAuthorizeUrl
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

    @Test
    fun testBuildAuthorizeUrl() = kotlinx.coroutines.test.runTest {
        val identity = "test.example.com"
        val url = buildAuthorizeUrl(identity)

        // Should not be null or empty
        assertNotNull(url)
        assertTrue(url.isNotEmpty())

        // Should be a valid HTTPS URL for authorization
        assertTrue(url.startsWith("https://"))
        assertTrue(url.contains("/api/owner/v1/youauth/authorize"))
        assertTrue(url.contains("?"))

        // Should contain essential parameters
        assertTrue(url.contains("client_id="))
        assertTrue(url.contains("client_type="))
        assertTrue(url.contains("redirect_uri="))
        assertTrue(url.contains("public_key="))
        assertTrue(url.contains("state="))
    }
}