package id.homebase.homebasekmppoc.lib.youauth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Tests for TargetDriveAccessRequest data class. */
class TargetDriveAccessRequestTest {

    @Test
    fun toMap_withBasicFields_returnsCorrectMap() {
        val request =
                TargetDriveAccessRequest(
                        alias = "photos",
                        type = "media",
                        name = "Photo Album",
                        description = "User's photo album",
                        permissions = listOf(DrivePermissionType.Read, DrivePermissionType.Write)
                )

        val map = request.toMap()

        assertEquals("photos", map["a"])
        assertEquals("media", map["t"])
        assertEquals("Photo Album", map["n"])
        assertEquals("User's photo album", map["d"])
        assertEquals(3, map["p"]) // Read(1) + Write(2)
    }

    @Test
    fun toMap_withOptionalFields_includesOptionalFields() {
        val request =
                TargetDriveAccessRequest(
                        alias = "files",
                        type = "document",
                        name = "Documents",
                        description = "User documents",
                        permissions = listOf(DrivePermissionType.Read),
                        allowAnonymousRead = true,
                        allowSubscriptions = false
                )

        val map = request.toMap()

        assertEquals(true, map["r"])
        assertEquals(false, map["s"])
    }

    @Test
    fun toMap_withoutOptionalFields_excludesNullFields() {
        val request =
                TargetDriveAccessRequest(
                        alias = "test",
                        type = "test",
                        name = "Test",
                        description = "Test",
                        permissions = emptyList()
                )

        val map = request.toMap()

        // Optional fields should not be in map if null
        assertTrue(!map.containsKey("r") || map["r"] == null)
        assertTrue(!map.containsKey("s") || map["s"] == null)
    }

    @Test
    fun toMap_withEmptyPermissions_returnsZero() {
        val request =
                TargetDriveAccessRequest(
                        alias = "test",
                        type = "test",
                        name = "Test",
                        description = "Test",
                        permissions = emptyList()
                )

        val map = request.toMap()
        assertEquals(0, map["p"])
    }

    @Test
    fun toMap_withAttributes_includesEncodedAttributes() {
        val request =
                TargetDriveAccessRequest(
                        alias = "test",
                        type = "test",
                        name = "Test",
                        description = "Test",
                        permissions = listOf(DrivePermissionType.Read),
                        attributes = mapOf("key1" to "value1", "key2" to "value2")
                )

        val map = request.toMap()
        assertNotNull(map["at"])
        assertTrue((map["at"] as String).contains("key1"))
    }
}
