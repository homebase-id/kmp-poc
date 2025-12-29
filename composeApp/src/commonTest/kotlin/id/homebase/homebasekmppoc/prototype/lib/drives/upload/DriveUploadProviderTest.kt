package id.homebase.homebasekmppoc.prototype.lib.drives.upload

import id.homebase.homebasekmppoc.prototype.lib.core.OdinClientErrorCode
import id.homebase.homebasekmppoc.prototype.lib.core.OdinClientException
import id.homebase.homebasekmppoc.prototype.lib.drives.FileSystemType
import id.homebase.homebasekmppoc.prototype.lib.drives.files.GlobalTransitIdFileIdentifier
import id.homebase.homebasekmppoc.prototype.lib.http.MockOdinClientSetup
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest

/** Unit tests for DriveUploadProvider. */
@OptIn(ExperimentalUuidApi::class)
class DriveUploadProviderTest {

        private val testTargetDrive =
                id.homebase.homebasekmppoc.prototype.lib.drives.TargetDrive(
                        alias = Uuid.parse("00000000-0000-0000-0000-000000000001"),
                        type = Uuid.parse("00000000-0000-0000-0000-000000000002")
                )

        private fun createTestUploadResult(): CreateFileResult {
                return CreateFileResult(
                        keyHeader = null,
                        file =
                                FileIdFileIdentifier(
                                        fileId = "test-file-id",
                                        targetDrive = testTargetDrive
                                ),
                        globalTransitIdFileIdentifier =
                                GlobalTransitIdFileIdentifier(
                                        globalTransitId = Uuid.random(),
                                        targetDrive = testTargetDrive
                                ),
                        recipientStatus = emptyMap(),
                        newVersionTag = "v1"
                )
        }

        private fun createTestUpdateResult(): UpdateFileResult {
                return UpdateFileResult(
                        file =
                                FileIdFileIdentifier(
                                        fileId = "test-file-id",
                                        targetDrive = testTargetDrive
                                ),
                        globalTransitIdFileIdentifier =
                                GlobalTransitIdFileIdentifier(
                                        globalTransitId = Uuid.random(),
                                        targetDrive = testTargetDrive
                                ),
                        newVersionTag = "v2",
                        recipientStatus = emptyMap()
                )
        }

        private fun createTestFormData(): MultiPartFormDataContent {
                return MultiPartFormDataContent(formData { append("test", "value") })
        }

        @Test
        fun testPureUpload_successfulResponse() = runTest {
                // Arrange
                val expectedResult = createTestUploadResult()
                val responseJson =
                        id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
                                .serialize(expectedResult)
                val odinClient = MockOdinClientSetup.setupMockOdinClient(responseJson)
                val provider = DriveUploadProvider(odinClient)

                // Act
                val result = provider.pureUpload(createTestFormData())

                // Assert
                assertNotNull(result)
                assertEquals("test-file-id", result.file.fileId)
                assertEquals("v1", result.newVersionTag)
        }

        @Test
        fun testPureUpload_withFileSystemType() = runTest {
                // Arrange
                val expectedResult = createTestUploadResult()
                val responseJson =
                        id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
                                .serialize(expectedResult)
                val odinClient = MockOdinClientSetup.setupMockOdinClient(responseJson)
                val provider = DriveUploadProvider(odinClient)

                // Act
                val result =
                        provider.pureUpload(
                                data = createTestFormData(),
                                fileSystemType = FileSystemType.Comment
                        )

                // Assert
                assertNotNull(result)
        }

        @Test
        fun testPureUpload_versionConflict_withCallback() = runTest {
                // Arrange
                val errorJson =
                        """{"errorCode": "VersionTagMismatch", "message": "Version conflict"}"""
                val odinClient =
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = errorJson,
                                status = HttpStatusCode.Conflict
                        )
                val provider = DriveUploadProvider(odinClient)
                var callbackInvoked = false

                // Act
                val result =
                        provider.pureUpload(
                                data = createTestFormData(),
                                onVersionConflict = {
                                        callbackInvoked = true
                                        null
                                }
                        )

                // Assert
                assertTrue(callbackInvoked)
                assertNull(result)
        }

        @Test
        fun testPureUpload_versionConflict_withCallbackReturningResult() = runTest {
                // Arrange
                val errorJson =
                        """{"errorCode": "VersionTagMismatch", "message": "Version conflict"}"""
                val odinClient =
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = errorJson,
                                status = HttpStatusCode.Conflict
                        )
                val provider = DriveUploadProvider(odinClient)
                val retryResult = createTestUploadResult()

                // Act
                val result =
                        provider.pureUpload(
                                data = createTestFormData(),
                                onVersionConflict = { retryResult }
                        )

                // Assert
                assertNotNull(result)
                assertEquals("v1", result.newVersionTag)
        }

        @Test
        fun testPureUpload_versionConflict_noCallback_throwsException() = runTest {
                // Arrange
                val errorJson =
                        """{"errorCode": "VersionTagMismatch", "message": "Version conflict"}"""
                val odinClient =
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = errorJson,
                                status = HttpStatusCode.Conflict
                        )
                val provider = DriveUploadProvider(odinClient)

                // Act & Assert
                val exception =
                        assertFailsWith<OdinClientException> {
                                provider.pureUpload(createTestFormData())
                        }
                assertEquals(OdinClientErrorCode.VersionTagMismatch, exception.errorCode)
        }

        @Test
        fun testPureUpload_badRequest_throwsException() = runTest {
                // Arrange
                val errorJson = """{"errorCode": "InvalidUpload", "message": "Bad request"}"""
                val odinClient =
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = errorJson,
                                status = HttpStatusCode.BadRequest
                        )
                val provider = DriveUploadProvider(odinClient)

                // Act & Assert
                val exception =
                        assertFailsWith<OdinClientException> {
                                provider.pureUpload(createTestFormData())
                        }
                assertEquals(OdinClientErrorCode.InvalidUpload, exception.errorCode)
        }

        @Test
        fun testPureUpload_serverError_throwsException() = runTest {
                // Arrange
                val errorJson = """{"message": "Internal server error"}"""
                val odinClient =
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = errorJson,
                                status = HttpStatusCode.InternalServerError
                        )
                val provider = DriveUploadProvider(odinClient)

                // Act & Assert
                val exception =
                        assertFailsWith<OdinClientException> {
                                provider.pureUpload(createTestFormData())
                        }
                assertEquals(
                        OdinClientErrorCode.RemoteServerReturnedInternalServerError,
                        exception.errorCode
                )
        }

        @Test
        fun testPureUpdate_successfulResponse() = runTest {
                // Arrange
                val expectedResult = createTestUpdateResult()
                val responseJson =
                        id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
                                .serialize(expectedResult)
                val odinClient = MockOdinClientSetup.setupMockOdinClient(responseJson)
                val provider = DriveUploadProvider(odinClient)

                // Act
                val result = provider.pureUpdate(createTestFormData())

                // Assert
                assertNotNull(result)
                assertEquals("test-file-id", result.file?.fileId)
                assertEquals("v2", result.newVersionTag)
        }

        @Test
        fun testPureUpdate_versionConflict_withCallback() = runTest {
                // Arrange
                val errorJson =
                        """{"errorCode": "VersionTagMismatch", "message": "Version conflict"}"""
                val odinClient =
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = errorJson,
                                status = HttpStatusCode.Conflict
                        )
                val provider = DriveUploadProvider(odinClient)
                var callbackInvoked = false

                // Act
                val result =
                        provider.pureUpdate(
                                data = createTestFormData(),
                                onVersionConflict = {
                                        callbackInvoked = true
                                        null
                                }
                        )

                // Assert
                assertTrue(callbackInvoked)
                assertNull(result)
        }

        @Test
        fun testPureUpdate_notFound_throwsException() = runTest {
                // Arrange
                val errorJson = """{"errorCode": "FileNotFound", "message": "File not found"}"""
                val odinClient =
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = errorJson,
                                status = HttpStatusCode.NotFound
                        )
                val provider = DriveUploadProvider(odinClient)

                // Act & Assert
                val exception =
                        assertFailsWith<OdinClientException> {
                                provider.pureUpdate(createTestFormData())
                        }
                assertEquals(OdinClientErrorCode.FileNotFound, exception.errorCode)
        }

        @Test
        fun testPureUpdate_unauthorized_throwsException() = runTest {
                // Arrange
                val errorJson = """{"message": "Unauthorized"}"""
                val odinClient =
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = errorJson,
                                status = HttpStatusCode.Unauthorized
                        )
                val provider = DriveUploadProvider(odinClient)

                // Act & Assert
                val exception =
                        assertFailsWith<OdinClientException> {
                                provider.pureUpdate(createTestFormData())
                        }
                assertEquals(OdinClientErrorCode.InvalidAuthToken, exception.errorCode)
        }

        @Test
        fun testPureUpload_errorWithIntegerCode() = runTest {
                // Arrange - test that integer error codes are deserialized correctly
                val errorJson = """{"errorCode": 4161, "message": "Invalid file"}"""
                val odinClient =
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = errorJson,
                                status = HttpStatusCode.BadRequest
                        )
                val provider = DriveUploadProvider(odinClient)

                // Act & Assert
                val exception =
                        assertFailsWith<OdinClientException> {
                                provider.pureUpload(createTestFormData())
                        }
                assertEquals(OdinClientErrorCode.InvalidFile, exception.errorCode)
        }

        // ==================== uploadLocalMetadataTags Tests ====================

        @Test
        fun testUploadLocalMetadataTags_successfulResponse() = runTest {
                // Arrange
                val expectedResult = LocalMetadataUploadResult(newLocalVersionTag = "v1")
                val responseJson =
                        id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
                                .serialize(expectedResult)
                val odinClient = MockOdinClientSetup.setupMockOdinClient(responseJson)
                val provider = DriveUploadProvider(odinClient)
                val file =
                        FileIdFileIdentifier(fileId = "test-file-id", targetDrive = testTargetDrive)
                val localAppData = LocalAppData(versionTag = "v0", tags = listOf("tag1", "tag2"))

                // Act
                val result =
                        provider.uploadLocalMetadataTags(file = file, localAppData = localAppData)

                // Assert
                assertNotNull(result)
                assertEquals("v1", result.newLocalVersionTag)
        }

        @Test
        fun testUploadLocalMetadataTags_versionConflict_withCallback() = runTest {
                // Arrange
                val errorJson =
                        """{"errorCode": "VersionTagMismatch", "message": "Version conflict"}"""
                val odinClient =
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = errorJson,
                                status = HttpStatusCode.Conflict
                        )
                val provider = DriveUploadProvider(odinClient)
                val file =
                        FileIdFileIdentifier(fileId = "test-file-id", targetDrive = testTargetDrive)
                val localAppData = LocalAppData(versionTag = "v0", tags = listOf("tag1"))
                var callbackInvoked = false

                // Act
                val result =
                        provider.uploadLocalMetadataTags(
                                file = file,
                                localAppData = localAppData,
                                onVersionConflict = {
                                        callbackInvoked = true
                                        null
                                }
                        )

                // Assert
                assertTrue(callbackInvoked)
                assertNull(result)
        }

        @Test
        fun testUploadLocalMetadataTags_versionConflict_callbackReturnsResult() = runTest {
                // Arrange
                val errorJson =
                        """{"errorCode": "VersionTagMismatch", "message": "Version conflict"}"""
                val odinClient =
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = errorJson,
                                status = HttpStatusCode.Conflict
                        )
                val provider = DriveUploadProvider(odinClient)
                val file =
                        FileIdFileIdentifier(fileId = "test-file-id", targetDrive = testTargetDrive)
                val localAppData = LocalAppData(versionTag = "v0", tags = listOf("tag1"))
                val retryResult = LocalMetadataUploadResult(newLocalVersionTag = "v2")

                // Act
                val result =
                        provider.uploadLocalMetadataTags(
                                file = file,
                                localAppData = localAppData,
                                onVersionConflict = { retryResult }
                        )

                // Assert
                assertNotNull(result)
                assertEquals("v2", result.newLocalVersionTag)
        }

        @Test
        fun testUploadLocalMetadataTags_badRequest_throwsException() = runTest {
                // Arrange
                val errorJson = """{"errorCode": "ArgumentError", "message": "Invalid request"}"""
                val odinClient =
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = errorJson,
                                status = HttpStatusCode.BadRequest
                        )
                val provider = DriveUploadProvider(odinClient)
                val file =
                        FileIdFileIdentifier(fileId = "test-file-id", targetDrive = testTargetDrive)
                val localAppData = LocalAppData(versionTag = "v0", tags = listOf("tag1"))

                // Act & Assert
                val exception =
                        assertFailsWith<OdinClientException> {
                                provider.uploadLocalMetadataTags(
                                        file = file,
                                        localAppData = localAppData
                                )
                        }
                assertEquals(OdinClientErrorCode.ArgumentError, exception.errorCode)
        }

        // ==================== uploadLocalMetadataContent Tests ====================

        @Test
        fun testUploadLocalMetadataContent_successfulResponse_unencryptedFile() = runTest {
                // Arrange
                val expectedResult = LocalMetadataUploadResult(newLocalVersionTag = "v1")
                val responseJson =
                        id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
                                .serialize(expectedResult)
                val odinClient = MockOdinClientSetup.setupMockOdinClient(responseJson)
                val provider = DriveUploadProvider(odinClient)

                // Create unencrypted HomebaseFile
                val homebaseFile = createTestHomebaseFile(isEncrypted = false)
                val localAppData = LocalAppData(versionTag = "v0", content = "test content")

                // Act
                val result =
                        provider.uploadLocalMetadataContent(
                                targetDrive = testTargetDrive,
                                file = homebaseFile,
                                localAppData = localAppData
                        )

                // Assert
                assertNotNull(result)
                assertEquals("v1", result.newLocalVersionTag)
        }

        @Test
        fun testUploadLocalMetadataContent_versionConflict_withCallback() = runTest {
                // Arrange
                val errorJson =
                        """{"errorCode": "VersionTagMismatch", "message": "Version conflict"}"""
                val odinClient =
                        MockOdinClientSetup.setupMockOdinClient(
                                responseBody = errorJson,
                                status = HttpStatusCode.Conflict
                        )
                val provider = DriveUploadProvider(odinClient)
                val homebaseFile = createTestHomebaseFile(isEncrypted = false)
                val localAppData = LocalAppData(versionTag = "v0", content = "test content")
                var callbackInvoked = false

                // Act
                val result =
                        provider.uploadLocalMetadataContent(
                                targetDrive = testTargetDrive,
                                file = homebaseFile,
                                localAppData = localAppData,
                                onVersionConflict = {
                                        callbackInvoked = true
                                        null
                                }
                        )

                // Assert
                assertTrue(callbackInvoked)
                assertNull(result)
        }

        // ==================== Helper Methods ====================

        private fun createTestHomebaseFile(
                isEncrypted: Boolean = false
        ): id.homebase.homebasekmppoc.prototype.lib.drives.files.HomebaseFile {
                return id.homebase.homebasekmppoc.prototype.lib.drives.files.HomebaseFile(
                        fileId = Uuid.random(),
                        fileSystemType =
                                id.homebase.homebasekmppoc.prototype.lib.drives.FileSystemType
                                        .Standard,
                        fileState =
                                id.homebase.homebasekmppoc.prototype.lib.drives.files
                                        .HomebaseFileState.Active,
                        fileMetadata =
                                id.homebase.homebasekmppoc.prototype.lib.drives.files.FileMetadata(
                                        isEncrypted = isEncrypted,
                                        appData =
                                                id.homebase.homebasekmppoc.prototype.lib.drives
                                                        .files.AppFileMetaData(fileType = 1)
                                ),
                        sharedSecretEncryptedKeyHeader =
                                id.homebase.homebasekmppoc.prototype.lib.crypto.EncryptedKeyHeader
                                        .empty()
                )
        }
}
