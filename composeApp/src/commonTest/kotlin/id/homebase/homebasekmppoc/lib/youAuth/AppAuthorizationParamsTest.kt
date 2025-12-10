package id.homebase.homebasekmppoc.lib.youAuth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests for AppAuthorizationParams data class. */
class AppAuthorizationParamsTest {

    @Test
    fun create_withBasicParams_createsCorrectInstance() {
        val params =
                AppAuthorizationParams.create(
                        appName = "Test App",
                        appId = "app-123",
                        friendlyName = "Chrome | macOS",
                        returnUrl = "https://callback"
                )

        assertEquals("Test App", params.name)
        assertEquals("app-123", params.appId)
        assertEquals("Chrome | macOS", params.friendlyName)
        assertEquals("https://callback", params.returnUrl)
    }

    @Test
    fun create_withDrives_serializesDrives() {
        val drives =
                listOf(
                        TargetDriveAccessRequest(
                                alias = "photos",
                                type = "media",
                                name = "Photos",
                                description = "Photo album",
                                permissions = listOf(DrivePermissionType.Read)
                        )
                )

        val params =
                AppAuthorizationParams.create(
                        appName = "Test App",
                        appId = "app-123",
                        friendlyName = "Test",
                        drives = drives,
                        returnUrl = "https://callback"
                )

        assertTrue(params.drives != null)
        assertTrue(params.drives!!.contains("photos"))
    }

    @Test
    fun create_withPermissions_joinsWithComma() {
        val params =
                AppAuthorizationParams.create(
                        appName = "Test App",
                        appId = "app-123",
                        friendlyName = "Test",
                        permissions = listOf(1, 2, 3),
                        returnUrl = "https://callback"
                )

        assertEquals("1,2,3", params.permissions)
    }

    @Test
    fun create_withCircles_joinsWithComma() {
        val params =
                AppAuthorizationParams.create(
                        appName = "Test App",
                        appId = "app-123",
                        friendlyName = "Test",
                        circles = listOf("circle1", "circle2"),
                        returnUrl = "https://callback"
                )

        assertEquals("circle1,circle2", params.circles)
    }

    @Test
    fun toJson_returnsValidJsonString() {
        val params =
                AppAuthorizationParams.create(
                        appName = "Test App",
                        appId = "app-123",
                        friendlyName = "Test Client",
                        returnUrl = "https://callback"
                )

        val json = params.toJson()

        assertTrue(json.startsWith("{"))
        assertTrue(json.endsWith("}"))
        assertTrue(json.contains("\"n\":\"Test App\""))
        assertTrue(json.contains("\"appId\":\"app-123\""))
    }

    @Test
    fun create_withOrigin_setsOrigin() {
        val params =
                AppAuthorizationParams.create(
                        appName = "Test App",
                        appId = "app-123",
                        friendlyName = "Test",
                        returnUrl = "https://callback",
                        origin = "https://myapp.com"
                )

        assertEquals("https://myapp.com", params.origin)
    }
}
