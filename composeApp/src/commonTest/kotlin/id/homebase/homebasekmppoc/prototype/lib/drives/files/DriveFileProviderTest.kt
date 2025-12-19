package id.homebase.homebasekmppoc.prototype.lib.drives.files

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

        private val testTargetDrive =
                TargetDrive(
                        alias = Uuid.parse("00000000-0000-0000-0000-000000000001"),
                        type = Uuid.parse("00000000-0000-0000-0000-000000000002")
                )

        @Test
        fun testDeleteFile_successfulResponse() = runTest {
                // Arrange
                val odinClient =
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = "{}",
                                status = HttpStatusCode.OK
                        )
                val provider = DriveFileProvider(odinClient)

                // Act
                val result =
                        provider.deleteFile(targetDrive = testTargetDrive, fileId = "test-file-id")

                // Assert
                assertTrue(result)
        }

        @Test
        fun testDeleteFile_hardDelete() = runTest {
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
                val odinClient = MockOdinClientSetup.createMockOdinClient(mockClient)
                val provider = DriveFileProvider(odinClient)

                // Act
                val result =
                        provider.deleteFile(
                                targetDrive = testTargetDrive,
                                fileId = "test-file-id",
                                hardDelete = true
                        )

                // Assert
                assertTrue(result)
                assertTrue(requestedEndpoint?.contains("harddelete") == true)
        }

        @Test
        fun testDeleteFile_softDelete() = runTest {
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
                val odinClient = MockOdinClientSetup.createMockOdinClient(mockClient)
                val provider = DriveFileProvider(odinClient)

                // Act
                val result =
                        provider.deleteFile(
                                targetDrive = testTargetDrive,
                                fileId = "test-file-id",
                                hardDelete = false
                        )

                // Assert
                assertTrue(result)
                assertTrue(requestedEndpoint?.contains("/delete") == true)
                assertTrue(requestedEndpoint?.contains("harddelete") == false)
        }

        @Test
        fun testDeleteFile_withRecipients() = runTest {
                // Arrange
                val odinClient =
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = "{}",
                                status = HttpStatusCode.OK
                        )
                val provider = DriveFileProvider(odinClient)

                // Act
                val result =
                        provider.deleteFile(
                                targetDrive = testTargetDrive,
                                fileId = "test-file-id",
                                recipients =
                                        listOf("recipient1@domain.com", "recipient2@domain.com")
                        )

                // Assert
                assertTrue(result)
        }

        @Test
        fun testDeleteFile_notFound_returnsFalse() = runTest {
                // Arrange
                val errorJson = """{"errorCode": "FileNotFound", "message": "File not found"}"""
                val odinClient =
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = errorJson,
                                status = HttpStatusCode.NotFound
                        )
                val provider = DriveFileProvider(odinClient)

                // Act
                val result =
                        provider.deleteFile(
                                targetDrive = testTargetDrive,
                                fileId = "non-existent-file"
                        )

                // Assert - returns false when file not found
                assertEquals(false, result)
        }

        @Test
        fun testDeleteFile_emptyTargetDrive_throwsIllegalArgument() = runTest {
                // Arrange
                val odinClient = MockOdinClientSetup.setupMockOdinClient("{}")
                val provider = DriveFileProvider(odinClient)
                val emptyDrive = TargetDrive(alias = Uuid.NIL, type = Uuid.NIL)

                // Act & Assert
                assertFailsWith<IllegalArgumentException> {
                        provider.deleteFile(targetDrive = emptyDrive, fileId = "test-file-id")
                }
        }

        @Test
        fun testDeleteFile_emptyFileId_throwsIllegalArgument() = runTest {
                // Arrange
                val odinClient = MockOdinClientSetup.setupMockOdinClient("{}")
                val provider = DriveFileProvider(odinClient)

                // Act & Assert
                assertFailsWith<IllegalArgumentException> {
                        provider.deleteFile(targetDrive = testTargetDrive, fileId = "")
                }
        }

        @Test
        fun testDeleteFiles_successfulBatchDelete() = runTest {
                // Arrange
                val odinClient =
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = "{}",
                                status = HttpStatusCode.OK
                        )
                val provider = DriveFileProvider(odinClient)

                // Act
                val result =
                        provider.deleteFiles(
                                targetDrive = testTargetDrive,
                                fileIds = listOf("file1", "file2", "file3")
                        )

                // Assert
                assertTrue(result)
        }

        @Test
        fun testDeleteFiles_usesCorrectEndpoint() = runTest {
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
                val odinClient = MockOdinClientSetup.createMockOdinClient(mockClient)
                val provider = DriveFileProvider(odinClient)

                // Act
                provider.deleteFiles(
                        targetDrive = testTargetDrive,
                        fileIds = listOf("file1", "file2")
                )

                // Assert
                assertTrue(requestedEndpoint?.contains("deletefileidbatch") == true)
        }

        @Test
        fun testDeleteFiles_emptyList_throwsIllegalArgument() = runTest {
                // Arrange
                val odinClient = MockOdinClientSetup.setupMockOdinClient("{}")
                val provider = DriveFileProvider(odinClient)

                // Act & Assert
                assertFailsWith<IllegalArgumentException> {
                        provider.deleteFiles(targetDrive = testTargetDrive, fileIds = emptyList())
                }
        }

        @Test
        fun testDeleteFilesByGroupId_successfulBatchDelete() = runTest {
                // Arrange
                val odinClient =
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = "{}",
                                status = HttpStatusCode.OK
                        )
                val provider = DriveFileProvider(odinClient)

                // Act
                val result =
                        provider.deleteFilesByGroupId(
                                targetDrive = testTargetDrive,
                                groupIds = listOf("group1", "group2")
                        )

                // Assert
                assertTrue(result)
        }

        @Test
        fun testDeleteFilesByGroupId_usesCorrectEndpoint() = runTest {
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
                val odinClient = MockOdinClientSetup.createMockOdinClient(mockClient)
                val provider = DriveFileProvider(odinClient)

                // Act
                provider.deleteFilesByGroupId(
                        targetDrive = testTargetDrive,
                        groupIds = listOf("group1")
                )

                // Assert
                assertTrue(requestedEndpoint?.contains("deletegroupidbatch") == true)
        }

        @Test
        fun testDeleteFilesByGroupId_emptyList_throwsIllegalArgument() = runTest {
                // Arrange
                val odinClient = MockOdinClientSetup.setupMockOdinClient("{}")
                val provider = DriveFileProvider(odinClient)

                // Act & Assert
                assertFailsWith<IllegalArgumentException> {
                        provider.deleteFilesByGroupId(
                                targetDrive = testTargetDrive,
                                groupIds = emptyList()
                        )
                }
        }

        @Test
        fun testDeleteFiles_serverError_returnsFalse() = runTest {
                // Arrange
                val errorJson = """{"message": "Internal server error"}"""
                val odinClient =
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = errorJson,
                                status = HttpStatusCode.InternalServerError
                        )
                val provider = DriveFileProvider(odinClient)

                // Act
                val result =
                        provider.deleteFiles(
                                targetDrive = testTargetDrive,
                                fileIds = listOf("file1")
                        )

                // Assert - returns false on server error
                assertEquals(false, result)
        }

        @Test
        fun testDeleteFile_withFileSystemType() = runTest {
                // Arrange
                val odinClient =
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = "{}",
                                status = HttpStatusCode.OK
                        )
                val provider = DriveFileProvider(odinClient)

                // Act
                val result =
                        provider.deleteFile(
                                targetDrive = testTargetDrive,
                                fileId = "test-file-id",
                                fileSystemType = FileSystemType.Comment
                        )

                // Assert
                assertTrue(result)
        }

        // ==================== getFileHeader Tests ====================

        @Test
        fun testGetFileHeader_successfulResponse() = runTest {
                // Arrange
                val homebaseFile = createTestHomebaseFile()
                val responseJson =
                        id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
                                .serialize(homebaseFile)
                val odinClient = MockOdinClientSetup.setupMockOdinClient(responseJson)
                val provider = DriveFileProvider(odinClient)

                // Act
                val result =
                        provider.getFileHeader(
                                targetDrive = testTargetDrive,
                                fileId = "test-file-id"
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
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody =
                                        """{"errorCode": "FileNotFound", "message": "File not found"}""",
                                status = HttpStatusCode.NotFound
                        )
                val provider = DriveFileProvider(odinClient)

                // Act
                val result =
                        provider.getFileHeader(
                                targetDrive = testTargetDrive,
                                fileId = "non-existent-file"
                        )

                // Assert
                assertNull(result)
        }

        @Test
        fun testGetFileHeader_invalidTargetDrive_throwsIllegalArgument() = runTest {
                // Arrange
                val odinClient = MockOdinClientSetup.setupMockOdinClient("{}")
                val provider = DriveFileProvider(odinClient)
                val emptyDrive = TargetDrive(alias = Uuid.NIL, type = Uuid.NIL)

                // Act & Assert
                assertFailsWith<IllegalArgumentException> {
                        provider.getFileHeader(targetDrive = emptyDrive, fileId = "test-file-id")
                }
        }

        @Test
        fun testGetFileHeader_emptyFileId_throwsIllegalArgument() = runTest {
                // Arrange
                val odinClient = MockOdinClientSetup.setupMockOdinClient("{}")
                val provider = DriveFileProvider(odinClient)

                // Act & Assert
                assertFailsWith<IllegalArgumentException> {
                        provider.getFileHeader(targetDrive = testTargetDrive, fileId = "")
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
                val odinClient = MockOdinClientSetup.createMockOdinClient(mockClient)
                val provider = DriveFileProvider(odinClient)

                // Act
                val result =
                        provider.getPayloadBytes(
                                targetDrive = testTargetDrive,
                                fileId = "test-file-id",
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
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = """{"errorCode": "FileNotFound"}""",
                                status = HttpStatusCode.NotFound
                        )
                val provider = DriveFileProvider(odinClient)

                // Act
                val result =
                        provider.getPayloadBytes(
                                targetDrive = testTargetDrive,
                                fileId = "test-file-id",
                                key = "payload-key"
                        )

                // Assert
                assertNull(result)
        }

        @Test
        fun testGetPayloadBytes_emptyKey_throwsIllegalArgument() = runTest {
                // Arrange
                val odinClient = MockOdinClientSetup.setupMockOdinClient("{}")
                val provider = DriveFileProvider(odinClient)

                // Act & Assert
                assertFailsWith<IllegalArgumentException> {
                        provider.getPayloadBytes(
                                targetDrive = testTargetDrive,
                                fileId = "test-file-id",
                                key = ""
                        )
                }
        }

        // ==================== getThumbBytes Tests ====================

        @Test
        fun testGetThumbBytes_successfulResponse() = runTest {
                // Arrange
                val testBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47) // PNG magic bytes
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
                val odinClient = MockOdinClientSetup.createMockOdinClient(mockClient)
                val provider = DriveFileProvider(odinClient)

                // Act
                val result =
                        provider.getThumbBytes(
                                targetDrive = testTargetDrive,
                                fileId = "test-file-id",
                                payloadKey = "payload-key",
                                width = 100,
                                height = 100,
                                options = FileOperationOptions(decrypt = false)
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
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = """{"errorCode": "FileNotFound"}""",
                                status = HttpStatusCode.NotFound
                        )
                val provider = DriveFileProvider(odinClient)

                // Act
                val result =
                        provider.getThumbBytes(
                                targetDrive = testTargetDrive,
                                fileId = "test-file-id",
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
                val odinClient = MockOdinClientSetup.setupMockOdinClient("{}")
                val provider = DriveFileProvider(odinClient)

                // Act & Assert - zero width
                assertFailsWith<IllegalArgumentException> {
                        provider.getThumbBytes(
                                targetDrive = testTargetDrive,
                                fileId = "test-file-id",
                                payloadKey = "payload-key",
                                width = 0,
                                height = 100
                        )
                }

                // Act & Assert - negative height
                assertFailsWith<IllegalArgumentException> {
                        provider.getThumbBytes(
                                targetDrive = testTargetDrive,
                                fileId = "test-file-id",
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
                val odinClient = MockOdinClientSetup.setupMockOdinClient(responseJson)
                val provider = DriveFileProvider(odinClient)

                // Act
                val result =
                        provider.getTransferHistory(
                                targetDrive = testTargetDrive,
                                fileId = "test-file-id"
                        )

                // Assert
                assertNotNull(result)
                assertEquals(transferHistory.originalRecipientCount, result.originalRecipientCount)
        }

        @Test
        fun testGetTransferHistory_notFound_returnsNull() = runTest {
                // Arrange
                val odinClient =
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = """{"errorCode": "FileNotFound"}""",
                                status = HttpStatusCode.NotFound
                        )
                val provider = DriveFileProvider(odinClient)

                // Act
                val result =
                        provider.getTransferHistory(
                                targetDrive = testTargetDrive,
                                fileId = "non-existent-file"
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
}
