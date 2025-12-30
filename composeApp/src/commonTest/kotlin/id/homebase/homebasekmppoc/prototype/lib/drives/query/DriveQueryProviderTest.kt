package id.homebase.homebasekmppoc.prototype.lib.drives.query

import id.homebase.homebasekmppoc.prototype.lib.crypto.EncryptedKeyHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.query.FileQueryParams
import id.homebase.homebasekmppoc.prototype.lib.drives.FileSystemType
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchResponse
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchResultOptionsRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.ServerMetadata
import id.homebase.homebasekmppoc.prototype.lib.drives.SharedSecretEncryptedFileHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.TargetDrive
import id.homebase.homebasekmppoc.prototype.lib.drives.files.AppFileMetaData
import id.homebase.homebasekmppoc.prototype.lib.drives.files.FileMetadata
import id.homebase.homebasekmppoc.prototype.lib.http.MockOdinClientSetup
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
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest

/**
 * Unit tests for DriveQueryProvider (new query module version) using OdinClient
 *
 * Uses the shared MockOdinClientSetup for creating mock OdinClient instances.
 */
@OptIn(ExperimentalEncodingApi::class, ExperimentalUuidApi::class)
class DriveQueryProviderTest {

    /** Creates a test shared secret - delegates to MockOdinClientSetup */
    private fun createTestSharedSecret(): ByteArray = MockOdinClientSetup.createTestSharedSecret()

    /** Creates a mock HttpClient - delegates to MockOdinClientSetup */
    private fun createMockClient(
        responseBody: String,
        status: HttpStatusCode = HttpStatusCode.OK
    ): HttpClient = MockOdinClientSetup.createMockClient(responseBody, status)

    /** Creates a test OdinClient with mock HttpClient - delegates to MockOdinClientSetup */
    private fun createTestOdinClient(
        mockClient: HttpClient,
        sharedSecret: ByteArray? = createTestSharedSecret()
    ) = MockOdinClientSetup.createMockOdinClient(mockClient, sharedSecret)

    /** Creates a sample QueryBatchRequest for testing */
    private fun createTestQueryRequest(): QueryBatchRequest {
        return QueryBatchRequest(
            queryParams = FileQueryParams(),
            resultOptionsRequest =
                QueryBatchResultOptionsRequest(
                    maxRecords = 100,
                    includeMetadataHeader = true
                )
        )
    }

    /** Creates a sample QueryBatchResponse for testing */
    private fun createTestResponse(name: String = "test-query"): QueryBatchResponse {
        return QueryBatchResponse(
            name = name,
            invalidDrive = false,
            includeMetadataHeader = true,
            cursorState = "cursor123",
            searchResults = emptyList()
        )
    }

    @Test
    fun testQueryBatch_successfulResponse() = runTest {
        // Arrange
        val expectedResponse = createTestResponse()
        val responseJson = OdinSystemSerializer.serialize(expectedResponse)
        val mockClient = createMockClient(responseJson)
        val odinClient = createTestOdinClient(mockClient)
        val provider = DriveQueryProvider(odinClient)

        val driveId = Uuid.parse("00000000-0000-0000-0000-000000000001")

        // Act
        val result = provider.queryBatch(driveId, createTestQueryRequest())

        // Assert
        assertEquals(expectedResponse.name, result.name)
        assertEquals(expectedResponse.invalidDrive, result.invalidDrive)
        assertEquals(expectedResponse.includeMetadataHeader, result.includeMetadataHeader)
        assertEquals(expectedResponse.cursorState, result.cursorState)
    }

    @Test
    fun testQueryBatch_invalidDriveResponse() = runTest {
        // Arrange
        val expectedResponse = QueryBatchResponse.fromInvalidDrive("test-drive")
        val responseJson = OdinSystemSerializer.serialize(expectedResponse)
        val mockClient = createMockClient(responseJson)
        val odinClient = createTestOdinClient(mockClient)
        val provider = DriveQueryProvider(odinClient)
        val driveId = Uuid.parse("00000000-0000-0000-0000-000000000001")

        // Act
        val result = provider.queryBatch(driveId, createTestQueryRequest())

        // Assert
        assertTrue(result.invalidDrive)
        assertEquals("test-drive", result.name)
        assertTrue(result.searchResults.isEmpty())
    }

    @Test
    fun testQueryBatch_withSearchResults() = runTest {
        // Arrange
        val searchResult =
            SharedSecretEncryptedFileHeader(
                fileId = Uuid.random(),
                driveId = Uuid.parse("00000000-0000-0000-0000-000000000001"),
                fileState =
                    id.homebase.homebasekmppoc.prototype.lib.drives.FileState.Active,
                fileSystemType = FileSystemType.Standard,
                sharedSecretEncryptedKeyHeader = EncryptedKeyHeader.empty(),
                fileMetadata =
                    FileMetadata(
                        isEncrypted = false,
                        appData =
                            AppFileMetaData(
                                fileType = 100,
                                dataType = 200,
                                content = "test content"
                            )
                    ),
                serverMetadata = ServerMetadata()
            )
        val expectedResponse =
            QueryBatchResponse(
                name = "test",
                invalidDrive = false,
                includeMetadataHeader = true,
                searchResults = listOf(searchResult)
            )
        val responseJson = OdinSystemSerializer.serialize(expectedResponse)
        val mockClient = createMockClient(responseJson)
        val odinClient = createTestOdinClient(mockClient)
        val provider = DriveQueryProvider(odinClient)
        val driveId = Uuid.parse("00000000-0000-0000-0000-000000000001")

        // Act
        val result = provider.queryBatch(driveId, createTestQueryRequest())

        // Assert
        assertEquals(1, result.searchResults.size)
        assertNotNull(result.searchResults[0].fileMetadata)
        assertEquals("test content", result.searchResults[0].fileMetadata.appData.content)
    }

    @Test
    fun testQueryBatch_withDecryptOption_setsIncludeMetadataHeader() = runTest {
        // Arrange
        var capturedRequest: Boolean? = null
        val mockClient =
            HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        // Check if includeMetadataHeader was added
                        capturedRequest =
                            request.url.parameters["includeMetadataHeader"]?.toBoolean()
                        val response = createTestResponse()
                        respond(
                            content =
                                ByteReadChannel(
                                    OdinSystemSerializer.serialize(response)
                                ),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json")
                        )
                    }
                }
                install(ContentNegotiation) { json(OdinSystemSerializer.json) }
            }

        val odinClient = createTestOdinClient(mockClient)
        val provider = DriveQueryProvider(odinClient)

        val driveId = Uuid.parse("00000000-0000-0000-0000-000000000001")

        // Create request without includeMetadataHeader
        val request =
            QueryBatchRequest(
                queryParams = FileQueryParams(),
                resultOptionsRequest =
                    QueryBatchResultOptionsRequest(
                        maxRecords = 100,
                        includeMetadataHeader = false
                    )
            )

        // Act
        provider.queryBatch(driveId, request, QueryBatchOptions(decrypt = true))

        // Assert - includeMetadataHeader should be set to true when decrypt is enabled
        assertEquals(true, capturedRequest)
    }

    @Test
    fun testQueryBatch_emptyResults() = runTest {
        // Arrange
        val expectedResponse =
            QueryBatchResponse(
                name = "empty",
                invalidDrive = false,
                includeMetadataHeader = false,
                searchResults = emptyList()
            )
        val responseJson = OdinSystemSerializer.serialize(expectedResponse)
        val mockClient = createMockClient(responseJson)
        val odinClient = createTestOdinClient(mockClient)
        val provider = DriveQueryProvider(odinClient)

        val driveId = Uuid.parse("00000000-0000-0000-0000-000000000001")

        // Act
        val result = provider.queryBatch(driveId, createTestQueryRequest())

        // Assert
        assertTrue(result.searchResults.isEmpty())
        assertEquals("empty", result.name)
    }
}
