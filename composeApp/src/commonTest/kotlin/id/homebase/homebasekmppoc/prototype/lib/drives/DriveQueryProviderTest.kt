package id.homebase.homebasekmppoc.prototype.lib.drives

import id.homebase.homebasekmppoc.prototype.lib.crypto.AesCbc
import id.homebase.homebasekmppoc.prototype.lib.crypto.CryptoHelper
import id.homebase.homebasekmppoc.prototype.lib.http.SharedSecretEncryptedPayload
import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/** Unit tests for DriveQueryProvider using Ktor MockEngine */
class DriveQueryProviderTest {

    /**
     * Creates a test shared secret for encryption/decryption. Uses a fixed 32-byte key for
     * reproducible tests.
     */
    private fun createTestSharedSecret(): String {
        // Fixed 32-byte key for AES-256
        val keyBytes = ByteArray(32) { it.toByte() }
        return Base64.encode(keyBytes)
    }

    /** Creates an encrypted response payload using the shared secret. */
    private suspend fun createEncryptedResponse(
            response: QueryBatchResponse,
            sharedSecretBase64: String
    ): String {
        val json = OdinSystemSerializer.serialize(response)
        val sharedSecret = Base64.decode(sharedSecretBase64)
        val payload = CryptoHelper.encryptData(json, sharedSecret)
        return OdinSystemSerializer.serialize(payload)
    }

    /** Creates a MockEngine HttpClient that returns the given encrypted response. */
    private fun createMockClient(
            responseBody: String,
            status: HttpStatusCode = HttpStatusCode.OK
    ): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond(
                            content = ByteReadChannel(responseBody),
                            status = status,
                            headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
            install(ContentNegotiation) { json(OdinSystemSerializer.json) }
        }
    }

    @Test
    fun testQueryBatch_successfulResponse() = runTest {
        // Arrange
        val sharedSecret = createTestSharedSecret()
        val expectedResponse =
                QueryBatchResponse(
                        name = "test-query",
                        invalidDrive = false,
                        includeMetadataHeader = true,
                        cursorState = "cursor123",
                        searchResults = emptyList()
                )
        val encryptedResponse = createEncryptedResponse(expectedResponse, sharedSecret)
        val mockClient = createMockClient(encryptedResponse)
        val provider = DriveQueryProvider(mockClient)

        // Act
        val result =
                provider.queryBatch(
                        uri =
                                "https://test.domain.com/api/apps/v1/drive/query/batch?maxRecords=1000",
                        clientAuthToken = "test-token",
                        sharedSecret = sharedSecret
                )

        // Assert
        assertEquals(expectedResponse.name, result.name)
        assertEquals(expectedResponse.invalidDrive, result.invalidDrive)
        assertEquals(expectedResponse.includeMetadataHeader, result.includeMetadataHeader)
        assertEquals(expectedResponse.cursorState, result.cursorState)
    }

    @Test
    fun testQueryBatch_invalidDriveResponse() = runTest {
        // Arrange
        val sharedSecret = createTestSharedSecret()
        val expectedResponse = QueryBatchResponse.fromInvalidDrive("test-drive")
        val encryptedResponse = createEncryptedResponse(expectedResponse, sharedSecret)
        val mockClient = createMockClient(encryptedResponse)
        val provider = DriveQueryProvider(mockClient)

        // Act
        val result =
                provider.queryBatch(
                        uri =
                                "https://test.domain.com/api/apps/v1/drive/query/batch?maxRecords=1000",
                        clientAuthToken = "test-token",
                        sharedSecret = sharedSecret
                )

        // Assert
        assertTrue(result.invalidDrive)
        assertEquals("test-drive", result.name)
        assertTrue(result.searchResults.isEmpty())
    }

    @Test
    fun testQueryBatch_errorResponse_throwsException() = runTest {
        // Arrange
        val sharedSecret = createTestSharedSecret()
        val errorResponse = QueryBatchResponse(name = "error")
        val encryptedResponse = createEncryptedResponse(errorResponse, sharedSecret)
        val mockClient = createMockClient(encryptedResponse, HttpStatusCode.InternalServerError)
        val provider = DriveQueryProvider(mockClient)

        // Act & Assert
        assertFailsWith<Exception> {
            provider.queryBatch(
                    uri = "https://test.domain.com/api/apps/v1/drive/query/batch?maxRecords=1000",
                    clientAuthToken = "test-token",
                    sharedSecret = sharedSecret
            )
        }
    }

    @Test
    fun testQueryBatch_fullParameters_buildsCorrectQuery() = runTest {
        // Arrange
        val sharedSecret = createTestSharedSecret()
        var capturedUrl: String? = null

        val mockClient =
                HttpClient(MockEngine) {
                    engine {
                        addHandler { request ->
                            capturedUrl = request.url.toString()
                            val response = QueryBatchResponse(name = "test")
                            val json = OdinSystemSerializer.serialize(response)
                            val secret = Base64.decode(sharedSecret)
                            val encryptedBytes =
                                    AesCbc.encrypt(
                                            json.encodeToByteArray(),
                                            secret,
                                            ByteArray(16) { 0 }
                                    )
                            val payload =
                                    SharedSecretEncryptedPayload(
                                            iv = Base64.encode(ByteArray(16) { 0 }),
                                            data = Base64.encode(encryptedBytes)
                                    )
                            respond(
                                    content =
                                            ByteReadChannel(
                                                    OdinSystemSerializer.serialize(payload)
                                            ),
                                    status = HttpStatusCode.OK,
                                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )
                        }
                    }
                    install(ContentNegotiation) { json(OdinSystemSerializer.json) }
                }

        val provider = DriveQueryProvider(mockClient)

        // Act
        provider.queryBatch(
                domain = "test.domain.com",
                clientAuthToken = "test-token",
                sharedSecret = sharedSecret,
                driveAlias = "alias-123",
                driveType = "type-456"
        )

        // Assert - URL should contain encrypted ss parameter
        assertEquals(capturedUrl?.contains("ss="), true)
        assertEquals(capturedUrl?.startsWith("https://test.domain.com/api/apps/v1/drive/query/batch"), true)
    }

}
