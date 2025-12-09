package id.homebase.homebasekmppoc.prototype.lib.drives

import id.homebase.homebasekmppoc.lib.drives.TargetDrive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/** Unit tests for ExternalFileIdentifier */
class ExternalFileIdentifierTest {

    @Test
    fun testHasValue_withValidIds_returnsTrue() {
        // Arrange
        val targetDrive = TargetDrive(alias = Uuid.random(), type = Uuid.random())
        val fileId = Uuid.random()
        val identifier = ExternalFileIdentifier(targetDrive = targetDrive, fileId = fileId)

        // Act & Assert
        assertTrue(identifier.hasValue())
    }

    @Test
    fun testHasValue_withNilFileId_returnsFalse() {
        // Arrange
        val targetDrive = TargetDrive(alias = Uuid.random(), type = Uuid.random())
        val identifier = ExternalFileIdentifier(targetDrive = targetDrive, fileId = Uuid.NIL)

        // Act & Assert
        assertFalse(identifier.hasValue())
    }

    @Test
    fun testHasValue_withInvalidTargetDrive_returnsFalse() {
        // Arrange
        val targetDrive = TargetDrive(alias = Uuid.NIL, type = Uuid.random())
        val identifier = ExternalFileIdentifier(targetDrive = targetDrive, fileId = Uuid.random())

        // Act & Assert
        assertFalse(identifier.hasValue())
    }

    @Test
    fun testToKey_returnsNonEmptyByteArray() {
        // Arrange
        val targetDrive = TargetDrive(alias = Uuid.random(), type = Uuid.random())
        val identifier = ExternalFileIdentifier(targetDrive = targetDrive, fileId = Uuid.random())

        // Act
        val key = identifier.toKey()

        // Assert
        assertTrue(key.isNotEmpty())
    }

    @Test
    fun testToKey_differentIdentifiers_produceDifferentKeys() {
        // Arrange
        val targetDrive = TargetDrive(alias = Uuid.random(), type = Uuid.random())
        val identifier1 = ExternalFileIdentifier(targetDrive = targetDrive, fileId = Uuid.random())
        val identifier2 = ExternalFileIdentifier(targetDrive = targetDrive, fileId = Uuid.random())

        // Act
        val key1 = identifier1.toKey()
        val key2 = identifier2.toKey()

        // Assert
        assertFalse(key1.contentEquals(key2))
    }

    @Test
    fun testToFileIdentifier_convertsCorrectly() {
        // Arrange
        val targetDrive = TargetDrive(alias = Uuid.random(), type = Uuid.random())
        val fileId = Uuid.random()
        val externalIdentifier = ExternalFileIdentifier(targetDrive = targetDrive, fileId = fileId)

        // Act
        val fileIdentifier = externalIdentifier.toFileIdentifier()

        // Assert
        assertEquals(fileId, fileIdentifier.fileId)
        assertEquals(targetDrive.alias, fileIdentifier.targetDrive.alias)
        assertEquals(targetDrive.type, fileIdentifier.targetDrive.type)
    }

    @Test
    fun testToString_containsFileIdAndDrive() {
        // Arrange
        val fileId = Uuid.random()
        val targetDrive = TargetDrive(alias = Uuid.random(), type = Uuid.random())
        val identifier = ExternalFileIdentifier(targetDrive = targetDrive, fileId = fileId)

        // Act
        val str = identifier.toString()

        // Assert
        assertTrue(str.contains(fileId.toString()))
        assertTrue(str.contains("TargetDrive"))
    }
}
