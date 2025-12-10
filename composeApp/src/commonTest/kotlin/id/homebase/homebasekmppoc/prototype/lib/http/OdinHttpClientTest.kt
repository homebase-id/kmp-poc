package id.homebase.homebasekmppoc.prototype.lib.http

import id.homebase.homebasekmppoc.lib.crypto.CryptoHelper
import id.homebase.homebasekmppoc.prototype.lib.authentication.AuthState
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Unit tests for OdinHttpClient
 *
 * Note: These tests focus on the URI building and encryption logic. Full HTTP testing with
 * MockEngine is limited because OdinHttpClient creates its own HttpClient internally.
 */
class OdinHttpClientTest {

    /** Creates a test shared secret for encryption/decryption. */
    private fun createTestSharedSecret(): String {
        val keyBytes = ByteArray(32) { it.toByte() }
        return Base64.encode(keyBytes)
    }

    /** Creates a test authenticated state for testing. */
    private fun createTestAuthState(): AuthState.Authenticated {
        return AuthState.Authenticated(
                identity = "test.identity.odin.earth",
                clientAuthToken = "test-client-auth-token",
                sharedSecret = createTestSharedSecret()
        )
    }

    @Test
    fun testBuildUriWithEncryptedQueryString_addsSSParameter() = runTest {
        // Arrange
        val authState = createTestAuthState()
        val client = OdinHttpClient(authState)
        val uri = "https://test.domain.com/api/path?param1=value1&param2=value2"

        // Act
        val encryptedUri = client.buildUriWithEncryptedQueryString(uri)

        // Assert
        assertTrue(encryptedUri.startsWith("https://test.domain.com/api/path?ss="))
        assertTrue(!encryptedUri.contains("param1=value1"))
        assertTrue(!encryptedUri.contains("param2=value2"))
    }

    @Test
    fun testBuildUriWithEncryptedQueryString_noQueryString_returnsOriginal() = runTest {
        // Arrange
        val authState = createTestAuthState()
        val client = OdinHttpClient(authState)
        val uri = "https://test.domain.com/api/path"

        // Act
        val encryptedUri = client.buildUriWithEncryptedQueryString(uri)

        // Assert
        assertEquals(uri, encryptedUri)
    }

    @Test
    fun testBuildUriWithEncryptedQueryString_emptyQueryString_returnsOriginal() = runTest {
        // Arrange
        val authState = createTestAuthState()
        val client = OdinHttpClient(authState)
        val uri = "https://test.domain.com/api/path?"

        // Act
        val encryptedUri = client.buildUriWithEncryptedQueryString(uri)

        // Assert
        assertEquals(uri, encryptedUri)
    }

    @Test
    fun testDecryptContentAsString_validPayload_decryptsCorrectly() = runTest {
        // Arrange
        val authState = createTestAuthState()
        val client = OdinHttpClient(authState)
        val originalText = "Hello, World!"

        // Encrypt the text first
        val encryptedPayload =
                CryptoHelper.encryptData(originalText, Base64.decode(authState.sharedSecret))
        val encryptedJson =
                id.homebase.homebasekmppoc.lib.serialization.OdinSystemSerializer.serialize(
                        encryptedPayload
                )

        // Act
        val decryptedText = client.decryptContentAsString(encryptedJson)

        // Assert
        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testDecryptContent_validTypedPayload_decryptsAndDeserializes() = runTest {
        // Arrange
        val authState = createTestAuthState()
        val client = OdinHttpClient(authState)

        @kotlinx.serialization.Serializable
        data class TestData(val message: String, val count: Int)

        val originalData = TestData(message = "test", count = 42)
        val originalJson =
                id.homebase.homebasekmppoc.lib.serialization.OdinSystemSerializer.serialize(
                        originalData
                )

        // Encrypt
        val encryptedPayload =
                CryptoHelper.encryptData(originalJson, Base64.decode(authState.sharedSecret))
        val encryptedJson =
                id.homebase.homebasekmppoc.lib.serialization.OdinSystemSerializer.serialize(
                        encryptedPayload
                )

        // Act
        val decryptedData = client.decryptContent<TestData>(encryptedJson)

        // Assert
        assertEquals("test", decryptedData.message)
        assertEquals(42, decryptedData.count)
    }

    @Test
    fun testDecryptContentAsString_invalidPayload_throwsException() = runTest {
        // Arrange
        val authState = createTestAuthState()
        val client = OdinHttpClient(authState)
        val invalidJson = "{ invalid json }"

        // Act & Assert
        assertFails { client.decryptContentAsString(invalidJson) }
    }

    @Test
    fun testOdinHttpClient_storesIdentityCorrectly() {
        // Arrange
        val authState = createTestAuthState()

        // Act
        val client = OdinHttpClient(authState)

        // Assert - We can verify this indirectly through URI building
        // The client should use the identity from authState
        runTest {
            val uri = "https://${authState.identity}/api/test?foo=bar"
            val encrypted = client.buildUriWithEncryptedQueryString(uri)
            assertTrue(encrypted.contains(authState.identity))
        }
    }

    @Test
    fun testCreateHttpClient_createsValidClient() {
        // Act
        val client = createHttpClient()

        // Assert
        // Just verify it creates without throwing
        assertTrue(true)
        client.close()
    }
}
