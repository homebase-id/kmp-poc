package id.homebase.homebasekmppoc.prototype.lib.drives

import id.homebase.homebasekmppoc.lib.drives.GlobalTransitIdFileIdentifier
import id.homebase.homebasekmppoc.lib.drives.TargetDrive
import id.homebase.homebasekmppoc.lib.serialization.OdinSystemSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/** Unit tests for GetPayloadRequest classes - serialization and basic functionality */
class GetPayloadRequestTest {

    @Test
    fun testGetPayloadRequest_serialization() {
        // Arrange
        val targetDrive =
                TargetDrive(
                        alias = Uuid.parse("12345678-1234-1234-1234-123456789abc"),
                        type = Uuid.parse("abcdefab-cdef-abcd-efab-cdefabcdefab")
                )
        val fileId = Uuid.parse("99999999-9999-9999-9999-999999999999")
        val request =
                GetPayloadRequest(
                        file = ExternalFileIdentifier(targetDrive = targetDrive, fileId = fileId)
                )

        // Act
        val json = OdinSystemSerializer.serialize(request)
        val deserialized = OdinSystemSerializer.deserialize<GetPayloadRequest>(json)

        // Assert
        assertEquals(request.file?.fileId, deserialized.file?.fileId)
        assertEquals(request.file?.targetDrive?.alias, deserialized.file?.targetDrive?.alias)
        assertEquals(request.file?.targetDrive?.type, deserialized.file?.targetDrive?.type)
    }

    @Test
    fun testGetPayloadRequest_withChunk_serialization() {
        // Arrange
        val request =
                GetPayloadRequest(
                                file =
                                        ExternalFileIdentifier(
                                                targetDrive =
                                                        TargetDrive(
                                                                alias = Uuid.random(),
                                                                type = Uuid.random()
                                                        ),
                                                fileId = Uuid.random()
                                        )
                        )
                        .apply {
                            chunk = FileChunk(start = 0, length = 1024)
                            key = "encryption-key-123"
                        }

        // Act
        val json = OdinSystemSerializer.serialize(request)
        val deserialized = OdinSystemSerializer.deserialize<GetPayloadRequest>(json)

        // Assert
        assertEquals(request.chunk?.start, deserialized.chunk?.start)
        assertEquals(request.chunk?.length, deserialized.chunk?.length)
        assertEquals(request.key, deserialized.key)
    }

    @Test
    fun testGetPayloadRequest_nullFile_serialization() {
        // Arrange
        val request = GetPayloadRequest(file = null)

        // Act
        val json = OdinSystemSerializer.serialize(request)
        val deserialized = OdinSystemSerializer.deserialize<GetPayloadRequest>(json)

        // Assert
        assertNull(deserialized.file)
    }

    @Test
    fun testGetPayloadByGlobalTransitIdRequest_serialization() {
        // Arrange
        val targetDrive = TargetDrive(alias = Uuid.random(), type = Uuid.random())
        val globalTransitId = Uuid.random()
        val request =
                GetPayloadByGlobalTransitIdRequest(
                        file =
                                GlobalTransitIdFileIdentifier(
                                        targetDrive = targetDrive,
                                        globalTransitId = globalTransitId
                                )
                )

        // Act
        val json = OdinSystemSerializer.serialize(request)
        val deserialized =
                OdinSystemSerializer.deserialize<GetPayloadByGlobalTransitIdRequest>(json)

        // Assert
        assertEquals(globalTransitId, deserialized.file?.globalTransitId)
        assertEquals(targetDrive.alias, deserialized.file?.targetDrive?.alias)
    }

    @Test
    fun testGetPayloadByUniqueIdRequest_serialization() {
        // Arrange
        val targetDrive = TargetDrive(alias = Uuid.random(), type = Uuid.random())
        val uniqueId = Uuid.random()
        val request = GetPayloadByUniqueIdRequest(uniqueId = uniqueId, targetDrive = targetDrive)

        // Act
        val json = OdinSystemSerializer.serialize(request)
        val deserialized = OdinSystemSerializer.deserialize<GetPayloadByUniqueIdRequest>(json)

        // Assert
        assertEquals(uniqueId, deserialized.uniqueId)
        assertEquals(targetDrive.alias, deserialized.targetDrive?.alias)
        assertEquals(targetDrive.type, deserialized.targetDrive?.type)
    }

    @Test
    fun testGetFileHeaderByUniqueIdRequest_serialization() {
        // Arrange
        val targetDrive = TargetDrive(alias = Uuid.random(), type = Uuid.random())
        val uniqueId = Uuid.random()
        val request = GetFileHeaderByUniqueIdRequest(uniqueId = uniqueId, targetDrive = targetDrive)

        // Act
        val json = OdinSystemSerializer.serialize(request)
        val deserialized = OdinSystemSerializer.deserialize<GetFileHeaderByUniqueIdRequest>(json)

        // Assert
        assertEquals(uniqueId, deserialized.uniqueId)
        assertEquals(targetDrive.alias, deserialized.targetDrive?.alias)
    }

    @Test
    fun testFileChunk_serialization() {
        // Arrange
        val chunk = FileChunk(start = 100, length = 500)

        // Act
        val json = OdinSystemSerializer.serialize(chunk)

        // Assert
        assertTrue(json.contains("100"))
        assertTrue(json.contains("500"))
    }
}
