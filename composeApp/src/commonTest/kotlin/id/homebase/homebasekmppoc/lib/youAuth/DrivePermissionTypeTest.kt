package id.homebase.homebasekmppoc.lib.youAuth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests for DrivePermissionType enum. */
class DrivePermissionTypeTest {

    @Test
    fun values_haveCorrectBitwiseValues() {
        assertEquals(1, DrivePermissionType.Read.value)
        assertEquals(2, DrivePermissionType.Write.value)
        assertEquals(4, DrivePermissionType.React.value)
        assertEquals(8, DrivePermissionType.Comment.value)
    }

    @Test
    fun combine_withMultiplePermissions_returnsSumOfValues() {
        val permissions = listOf(DrivePermissionType.Read, DrivePermissionType.Write)
        val combined = DrivePermissionType.combine(permissions)
        assertEquals(3, combined) // 1 + 2 = 3
    }

    @Test
    fun combine_withAllPermissions_returnsSumOfAll() {
        val permissions = DrivePermissionType.entries.toList()
        val combined = DrivePermissionType.combine(permissions)
        assertEquals(15, combined) // 1 + 2 + 4 + 8 = 15
    }

    @Test
    fun combine_withEmptyList_returnsZero() {
        val combined = DrivePermissionType.combine(emptyList())
        assertEquals(0, combined)
    }

    @Test
    fun fromValue_withCombinedValue_returnsCorrectPermissions() {
        val combined = 5 // Read(1) + React(4)
        val permissions = DrivePermissionType.fromValue(combined)

        assertEquals(2, permissions.size)
        assertTrue(permissions.contains(DrivePermissionType.Read))
        assertTrue(permissions.contains(DrivePermissionType.React))
    }

    @Test
    fun fromValue_withAllBitsSet_returnsAllPermissions() {
        val combined = 15 // All permissions
        val permissions = DrivePermissionType.fromValue(combined)

        assertEquals(4, permissions.size)
    }

    @Test
    fun fromValue_withZero_returnsEmptyList() {
        val permissions = DrivePermissionType.fromValue(0)
        assertTrue(permissions.isEmpty())
    }
}
