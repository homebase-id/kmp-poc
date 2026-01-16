package id.homebase.homebasekmppoc.prototype.lib.drives.files

import id.homebase.homebasekmppoc.prototype.lib.base.ApiCredentials
import id.homebase.homebasekmppoc.prototype.lib.base.CredentialsManager
import id.homebase.homebasekmppoc.prototype.lib.base.ServerException
import id.homebase.homebasekmppoc.prototype.lib.core.SecureByteArray
import id.homebase.homebasekmppoc.prototype.lib.drives.FileSystemType
import id.homebase.homebasekmppoc.prototype.lib.drives.TargetDrive
import id.homebase.homebasekmppoc.prototype.lib.http.MockOdinClientSetup
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest

/** Unit tests for DriveFileProvider. */
@OptIn(ExperimentalUuidApi::class)
class DriveFileProviderTest {

    private val driveId = Uuid.parse("00000000-0000-0000-0000-000000000001")

    @Test
    fun testSoftDeleteFile_successfulResponse() = runTest {
        // Arrange
        val odinClient =
            MockOdinClientSetup.createMockClient(
                responseBody = "{}",
                status = HttpStatusCode.OK
            )
        val provider = DriveFileProvider(odinClient, testCredentialsManager())

        // Act
        val result =
            provider.softDeleteFile(
                driveId = driveId,
                fileId = Uuid.parse("cfc97c30-7ee0-49d5-b303-c9fa1db6e252")
            )

        // Assert
        assertTrue(result.localFileDeleted)
    }

    @Test
    fun testDeleteFile_hardSoftDelete() = runTest {
        // Arrange
        var requestedEndpoint: String? = null
        val mockClient =
            HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        requestedEndpoint = request.url.encodedPath
                        respond(
                            content = ByteReadChannel("{}"),
                            status = HttpStatusCode.OK,
                            headers =
                                headersOf(
                                    HttpHeaders.ContentType,
                                    "application/json"
                                )
                        )
                    }
                }
                install(ContentNegotiation) {
                    json(
                        id.homebase.homebasekmppoc.prototype.lib
                            .serialization.OdinSystemSerializer.json
                    )
                }
            }
        val provider = DriveFileProvider(mockClient, testCredentialsManager())

        // Act
        val result =
            provider.softDeleteFile(
                driveId = driveId,
                fileId = Uuid.parse("cfc97c30-7ee0-49d5-b303-c9fa1db6e252")
            )

        // Assert
        assertTrue(result.localFileDeleted)
        assertTrue(requestedEndpoint?.contains("hard-delete") == true)
    }

    @Test
    fun testDeleteFile_softSoftDelete() = runTest {
        // Arrange
        var requestedEndpoint: String? = null
        val mockClient =
            HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        requestedEndpoint = request.url.encodedPath
                        respond(
                            content = ByteReadChannel("{}"),
                            status = HttpStatusCode.OK,
                            headers =
                                headersOf(
                                    HttpHeaders.ContentType,
                                    "application/json"
                                )
                        )
                    }
                }
                install(ContentNegotiation) {
                    json(
                        id.homebase.homebasekmppoc.prototype.lib
                            .serialization.OdinSystemSerializer.json
                    )
                }
            }
        val provider = DriveFileProvider(mockClient, testCredentialsManager())

        // Act
        val result =
            provider.softDeleteFile(
                driveId = driveId,
                fileId = Uuid.parse("cfc97c30-7ee0-49d5-b303-c9fa1db6e252")
            )

        // Assert
        assertTrue(result.localFileDeleted)
        assertTrue(requestedEndpoint?.contains("/delete") == true)
        assertTrue(requestedEndpoint.contains("hard-delete") == false)
    }

    @Test
    fun testSoftDeleteFile_withRecipients() = runTest {
        // Arrange
        val odinClient =
            MockOdinClientSetup.createMockClient(
                responseBody = "{}",
                status = HttpStatusCode.OK
            )
        val provider = DriveFileProvider(odinClient, testCredentialsManager())

        // Act
        val result =
            provider.softDeleteFile(
                driveId = driveId,
                fileId = Uuid.parse("cfc97c30-7ee0-49d5-b303-c9fa1db6e252"),
                recipients =
                    listOf("recipient1@domain.com", "recipient2@domain.com")
            )

        // Assert
        assertTrue(result.localFileDeleted)
    }

    @Test
    fun testSoftDeleteFile_notFound_returnsFalse() = runTest {
        // Arrange
        val errorJson = """{"errorCode": "FileNotFound", "message": "File not found"}"""
        val odinClient =
            MockOdinClientSetup.createMockClient(
                responseBody = errorJson,
                status = HttpStatusCode.NotFound
            )
        val provider = DriveFileProvider(odinClient, testCredentialsManager())

        // Act
        val result =
            provider.softDeleteFile(
                driveId = driveId,
                fileId = Uuid.parse("92e7c962-2449-47b8-894c-de1bff1b304d")
            )

        // Assert - returns false when file not found
        assertEquals(false, result.localFileDeleted)
    }

    @Test
    fun testSoftDeleteFile_emptyTargetDrive_throwsIllegalArgument() = runTest {
        // Arrange
        val odinClient = MockOdinClientSetup.createMockClient("{}")
        val provider = DriveFileProvider(odinClient, testCredentialsManager())
        val emptyDrive = TargetDrive(alias = Uuid.NIL, type = Uuid.NIL)

        // Act & Assert
        assertFailsWith<IllegalArgumentException> {
            provider.softDeleteFile(
                driveId = emptyDrive.alias,
                fileId = Uuid.parse("cfc97c30-7ee0-49d5-b303-c9fa1db6e252")
            )
        }
    }

    @Test
    fun testSoftDeleteFile_emptyFileId_throwsIllegalArgument() = runTest {
        // Arrange
        val odinClient = MockOdinClientSetup.createMockClient("{}")
        val provider = DriveFileProvider(odinClient, testCredentialsManager())

        // Act & Assert
        assertFailsWith<IllegalArgumentException> {
            provider.softDeleteFile(driveId = driveId, fileId = Uuid.NIL)
        }
    }

    @Test
    fun testDeleteFiles_successfulBatchSoftDelete() = runTest {
        // Arrange
        val odinClient =
            MockOdinClientSetup.createMockClient(
                responseBody = "{}",
                status = HttpStatusCode.OK
            )
        val provider = DriveFileProvider(odinClient, testCredentialsManager())

        // Act
        val result =
            provider.deleteFiles(
                driveId = driveId,
                fileIds = listOf(Uuid.random(), Uuid.random(), Uuid.random())
            )

        // Assert
        assertNotNull(result)
        assertTrue(result.results.isNotEmpty())
    }

    @Test
    fun testSoftDeleteFiles_usesCorrectEndpoint() = runTest {
        // Arrange
        var requestedEndpoint: String? = null
        val mockClient =
            HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        requestedEndpoint = request.url.encodedPath
                        respond(
                            content = ByteReadChannel("{}"),
                            status = HttpStatusCode.OK,
                            headers =
                                headersOf(
                                    HttpHeaders.ContentType,
                                    "application/json"
                                )
                        )
                    }
                }
                install(ContentNegotiation) {
                    json(
                        id.homebase.homebasekmppoc.prototype.lib
                            .serialization.OdinSystemSerializer.json
                    )
                }
            }
        val provider = DriveFileProvider(mockClient, testCredentialsManager())

        // Act
        provider.deleteFiles(
            driveId = driveId,
            fileIds = listOf(Uuid.random(), Uuid.random())
        )

        // Assert
        assertTrue(requestedEndpoint?.contains("delete-batch/by-file-id") == true)
    }

    @Test
    fun testSoftDeleteFiles_emptyList_throwsIllegalArgument() = runTest {
        // Arrange
        val odinClient = MockOdinClientSetup.createMockClient("{}")
        val provider = DriveFileProvider(odinClient, testCredentialsManager())

        // Act & Assert
        assertFailsWith<IllegalArgumentException> {
            provider.deleteFiles(driveId = driveId, fileIds = emptyList())
        }
    }

    @Test
    fun testDeleteFilesByGroupId_successfulBatchSoftDelete() = runTest {
        // Arrange
        val odinClient =
            MockOdinClientSetup.createMockClient(
                responseBody = "{}",
                status = HttpStatusCode.OK
            )
        val provider = DriveFileProvider(odinClient, testCredentialsManager())

        // Act
        val result =
            provider.deleteFilesByGroupId(
                driveId = driveId,
                groupIds = listOf(Uuid.random(), Uuid.random())
            )

        // Assert
        assertNotNull(result)
        assertTrue(result.results.isNotEmpty())

    }

    @Test
    fun testSoftDeleteFilesByGroupId_usesCorrectEndpoint() = runTest {
        // Arrange
        var requestedEndpoint: String? = null
        val mockClient =
            HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        requestedEndpoint = request.url.encodedPath
                        respond(
                            content = ByteReadChannel("{}"),
                            status = HttpStatusCode.OK,
                            headers =
                                headersOf(
                                    HttpHeaders.ContentType,
                                    "application/json"
                                )
                        )
                    }
                }
                install(ContentNegotiation) {
                    json(
                        id.homebase.homebasekmppoc.prototype.lib
                            .serialization.OdinSystemSerializer.json
                    )
                }
            }
        val provider = DriveFileProvider(mockClient, testCredentialsManager())

        // Act
        provider.deleteFilesByGroupId(
            driveId = driveId,
            groupIds = listOf(Uuid.random())
        )

        // Assert
        assertTrue(requestedEndpoint?.contains("delete-batch/by-group-id") == true)
    }

    @Test
    fun testSoftDeleteFilesByGroupId_emptyList_throwsIllegalArgument() = runTest {
        // Arrange
        val odinClient = MockOdinClientSetup.createMockClient("{}")
        val provider = DriveFileProvider(odinClient, testCredentialsManager())

        // Act & Assert
        assertFailsWith<IllegalArgumentException> {
            provider.deleteFilesByGroupId(
                driveId = driveId,
                groupIds = emptyList()
            )
        }
    }

    @Test
    fun testSoftDeleteFiles_serverError_returnsFalse() = runTest {
        // Arrange
        val errorJson = """{"message": "Internal server error"}"""
        val odinClient =
            MockOdinClientSetup.createMockClient(
                responseBody = errorJson,
                status = HttpStatusCode.InternalServerError
            )
        val provider = DriveFileProvider(odinClient, testCredentialsManager())

        // Act
        val result =
            provider.deleteFiles(
                driveId = driveId,
                fileIds = listOf(Uuid.random())
            )

        // Assert - returns false on server error
        assertFailsWith<ServerException> {
            provider.deleteFiles(
                driveId = driveId,
                fileIds = listOf(Uuid.random())
            )
        }
    }

    // ==================== getFileHeader Tests ====================

    @Test
    fun testGetFileHeader_successfulResponse() = runTest {
        // Arrange
        val homebaseFile = createTestHomebaseFile()
        val responseJson =
            id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
                .serialize(homebaseFile)
        val odinClient = MockOdinClientSetup.createMockClient(responseJson)
        val provider = DriveFileProvider(odinClient, testCredentialsManager())

        // Act
        val result =
            provider.getFileHeader(
                driveId = driveId,
                fileId = Uuid.parse("cfc97c30-7ee0-49d5-b303-c9fa1db6e252")
            )

        // Assert
        assertNotNull(result)
        assertEquals(homebaseFile.fileId, result.fileId)
        assertEquals(homebaseFile.fileSystemType, result.fileSystemType)
    }

    @Test
    fun testGetFileHeader_notFound_returnsNull() = runTest {
        // Arrange
        val odinClient =
            MockOdinClientSetup.createMockClient(
                responseBody =
                    """{"errorCode": "FileNotFound", "message": "File not found"}""",
                status = HttpStatusCode.NotFound
            )
        val provider = DriveFileProvider(odinClient, testCredentialsManager())

        // Act
        val result =
            provider.getFileHeader(
                driveId = driveId,
                fileId = Uuid.parse("92e7c962-2449-47b8-894c-de1bff1b304d")
            )

        // Assert
        assertNull(result)
    }

    @Test
    fun testGetFileHeader_invalidTargetDrive_throwsIllegalArgument() = runTest {
        // Arrange
        val odinClient = MockOdinClientSetup.createMockClient("{}")
        val provider = DriveFileProvider(odinClient, testCredentialsManager())
        val emptyDrive = TargetDrive(alias = Uuid.NIL, type = Uuid.NIL)

        // Act & Assert
        assertFailsWith<IllegalArgumentException> {
            provider.getFileHeader(
                driveId = emptyDrive.alias,
                fileId = Uuid.parse("cfc97c30-7ee0-49d5-b303-c9fa1db6e252")
            )
        }
    }

    @Test
    fun testGetFileHeader_emptyFileId_throwsIllegalArgument() = runTest {
        // Arrange
        val odinClient = MockOdinClientSetup.createMockClient("{}")
        val provider = DriveFileProvider(odinClient, testCredentialsManager())

        // Act & Assert
        assertFailsWith<IllegalArgumentException> {
            provider.getFileHeader(driveId = driveId, fileId = Uuid.NIL)
        }
    }

    // ==================== getPayloadBytes Tests ====================

    @Test
    fun testGetPayloadBytes_successfulResponse() = runTest {
        // Arrange
        val testBytes = "Hello, World!".encodeToByteArray()
        val mockClient =
            HttpClient(MockEngine) {
                engine {
                    addHandler { _ ->
                        respond(
                            content = ByteReadChannel(testBytes),
                            status = HttpStatusCode.OK,
                            headers =
                                headersOf(
                                    HttpHeaders.ContentType,
                                    "application/octet-stream"
                                )
                        )
                    }
                }
                install(ContentNegotiation) {
                    json(
                        id.homebase.homebasekmppoc.prototype.lib
                            .serialization.OdinSystemSerializer.json
                    )
                }
            }
        val provider = DriveFileProvider(mockClient, testCredentialsManager())

        // Act
        val result =
            provider.getPayloadBytesRaw(
                driveId = driveId,
                fileId = Uuid.parse("cfc97c30-7ee0-49d5-b303-c9fa1db6e252"),
                key = "payload-key",
                options = PayloadOperationOptions(decrypt = false)
            )

        // Assert
        assertNotNull(result)
        assertTrue(result.bytes.contentEquals(testBytes))
    }

    @Test
    fun testGetPayloadBytes_notFound_returnsNull() = runTest {
        // Arrange
        val odinClient =
            MockOdinClientSetup.createMockClient(
                responseBody = """{"errorCode": "FileNotFound"}""",
                status = HttpStatusCode.NotFound
            )
        val provider = DriveFileProvider(odinClient, testCredentialsManager())

        // Act
        val result =
            provider.getPayloadBytesRaw(
                driveId = driveId,
                fileId = Uuid.parse("cfc97c30-7ee0-49d5-b303-c9fa1db6e252"),
                key = "payload-key"
            )

        // Assert
        assertNull(result)
    }

    @Test
    fun testGetPayloadBytes_emptyKey_throwsIllegalArgument() = runTest {
        // Arrange
        val odinClient = MockOdinClientSetup.createMockClient("{}")
        val provider = DriveFileProvider(odinClient, testCredentialsManager())

        // Act & Assert
        assertFailsWith<IllegalArgumentException> {
            provider.getPayloadBytesRaw(
                driveId = driveId,
                fileId = Uuid.parse("cfc97c30-7ee0-49d5-b303-c9fa1db6e252"),
                key = ""
            )
        }
    }

    // ==================== getThumbBytes Tests ====================

    @Test
    fun testGetThumbBytes_successfulResponse() = runTest {
        // Arrange
        val testBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47) // PNG magic bytes
        val httpClient =
            HttpClient(MockEngine) {
                engine {
                    addHandler { _ ->
                        respond(
                            content = ByteReadChannel(testBytes),
                            status = HttpStatusCode.OK,
                            headers =
                                headersOf(
                                    HttpHeaders.ContentType,
                                    "image/png"
                                )
                        )
                    }
                }
                install(ContentNegotiation) {
                    json(
                        id.homebase.homebasekmppoc.prototype.lib
                            .serialization.OdinSystemSerializer.json
                    )
                }
            }
        val provider = DriveFileProvider(httpClient, testCredentialsManager())

        // Act
        val result =
            provider.getThumbBytesRaw(
                driveId = driveId,
                fileId = Uuid.parse("cfc97c30-7ee0-49d5-b303-c9fa1db6e252"),
                payloadKey = "payload-key",
                width = 100,
                height = 100
            )

        // Assert
        assertNotNull(result)
        assertTrue(result.bytes.contentEquals(testBytes))
        assertTrue(result.contentType.contains("image"))
    }

    @Test
    fun testGetThumbBytes_notFound_returnsNull() = runTest {
        // Arrange
        val odinClient =
            MockOdinClientSetup.createMockClient(
                responseBody = """{"errorCode": "FileNotFound"}""",
                status = HttpStatusCode.NotFound
            )
        val provider = DriveFileProvider(odinClient, testCredentialsManager())

        // Act
        val result =
            provider.getThumbBytesRaw(
                driveId = driveId,
                fileId = Uuid.parse("cfc97c30-7ee0-49d5-b303-c9fa1db6e252"),
                payloadKey = "payload-key",
                width = 100,
                height = 100
            )

        // Assert
        assertNull(result)
    }

    @Test
    fun testGetThumbBytes_invalidDimensions_throwsIllegalArgument() = runTest {
        // Arrange
        val odinClient = MockOdinClientSetup.createMockClient("{}")
        val provider = DriveFileProvider(odinClient, testCredentialsManager())

        // Act & Assert - zero width
        assertFailsWith<IllegalArgumentException> {
            provider.getThumbBytesRaw(
                driveId = driveId,
                fileId = Uuid.parse("cfc97c30-7ee0-49d5-b303-c9fa1db6e252"),
                payloadKey = "payload-key",
                width = 0,
                height = 100
            )
        }

        // Act & Assert - negative height
        assertFailsWith<IllegalArgumentException> {
            provider.getThumbBytesRaw(
                driveId = driveId,
                fileId = Uuid.parse("cfc97c30-7ee0-49d5-b303-c9fa1db6e252"),
                payloadKey = "payload-key",
                width = 100,
                height = -1
            )
        }
    }

    // ==================== getTransferHistory Tests ====================

    @Test
    fun testGetTransferHistory_successfulResponse() = runTest {
        // Arrange
        val transferHistory = createTestTransferHistory()
        val responseJson =
            id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
                .serialize(transferHistory)
        val odinClient = MockOdinClientSetup.createMockClient(responseJson)
        val provider = DriveFileProvider(odinClient, testCredentialsManager())

        // Act
        val result =
            provider.getTransferHistory(
                driveId = driveId,
                fileId = Uuid.parse("cfc97c30-7ee0-49d5-b303-c9fa1db6e252")
            )

        // Assert
        assertNotNull(result)
        assertEquals(transferHistory.originalRecipientCount, result.originalRecipientCount)
    }

    @Test
    fun testGetTransferHistory_notFound_returnsNull() = runTest {
        // Arrange
        val httpClient =
            MockOdinClientSetup.createMockClient(
                responseBody = """{"errorCode": "FileNotFound"}""",
                status = HttpStatusCode.NotFound
            )
        val provider = DriveFileProvider(httpClient, testCredentialsManager())

        // Act
        val result =
            provider.getTransferHistory(
                driveId = driveId,
                fileId = Uuid.random()
            )

        // Assert
        assertNull(result)
    }

    // ==================== Helper Methods ====================

    private fun createTestHomebaseFile(): HomebaseFile {
        return HomebaseFile(
            fileId = Uuid.random(),
            fileSystemType = FileSystemType.Standard,
            fileState = HomebaseFileState.Active,
            fileMetadata =
                FileMetadata(
                    isEncrypted = false,
                    appData = AppFileMetaData(fileType = 1)
                ),
            sharedSecretEncryptedKeyHeader =
                id.homebase.homebasekmppoc.prototype.lib.crypto.EncryptedKeyHeader
                    .empty()
        )
    }

    private fun createTestTransferHistory(): TransferHistory {
        return TransferHistory(
            originalRecipientCount = 2,
            history =
                TransferHistoryPage(
                    request =
                        TransferHistoryRequest(
                            pageNumber = 1,
                            pageSize = 10
                        ),
                    totalPages = 1,
                    results = emptyList()
                )
        )
    }

    suspend fun testCredentialsManager(): CredentialsManager =
        CredentialsManager().apply {
            setActiveCredentials(
                ApiCredentials.create(
                    domain = "test",
                    clientAccessToken = "fake-token",
                    sharedSecret = SecureByteArray(ByteArray(32))
                )
            )
        }
}
