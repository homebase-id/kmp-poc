package id.homebase.homebasekmppoc.lib.youauth

import id.homebase.homebasekmppoc.prototype.lib.http.MockOdinClientSetup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray

class PermissionExtensionManagerTest {

        private val hostIdentity = "test-host.homebase.link"
        private val appId = "test-app-id"
        private val appName = "Test App"
        private val returnUrl = "test://callback"

        private fun createConfig(
                permissions: List<AppPermissionType> = emptyList(),
                drives: List<TargetDriveAccessRequest> = emptyList(),
                circleDrives: List<TargetDriveAccessRequest> = emptyList(),
                needsAllConnected: Boolean = false
        ): PermissionExtensionConfig {
                return PermissionExtensionConfig(
                        appId = appId,
                        appName = appName,
                        permissions = permissions,
                        drives = drives,
                        circleDrives = circleDrives,
                        needsAllConnected = needsAllConnected,
                        returnUrl = returnUrl
                )
        }

        private fun createSecurityContextJson(
                permissions: List<Int> = emptyList(),
                driveGrants: List<DriveGrant> = emptyList(),
                isGrantedAllConnected: Boolean = false
        ): String {
                val permissionGroup = buildJsonObject {
                        putJsonObject("permissionSet") {
                                putJsonArray("keys") {
                                        permissions.forEach { add(JsonPrimitive(it)) }
                                }
                        }
                        putJsonArray("driveGrants") {
                                driveGrants.forEach { grant ->
                                        add(
                                                buildJsonObject {
                                                        putJsonObject("permissionedDrive") {
                                                                putJsonObject("drive") {
                                                                        put(
                                                                                "alias",
                                                                                JsonPrimitive(
                                                                                        grant.permissionedDrive
                                                                                                .drive
                                                                                                .alias
                                                                                )
                                                                        )
                                                                        put(
                                                                                "type",
                                                                                JsonPrimitive(
                                                                                        grant.permissionedDrive
                                                                                                .drive
                                                                                                .type
                                                                                )
                                                                        )
                                                                }
                                                                putJsonArray("permission") {
                                                                        grant.permissionedDrive
                                                                                .permission
                                                                                .forEach {
                                                                                        add(
                                                                                                JsonPrimitive(
                                                                                                        it.value
                                                                                                )
                                                                                        )
                                                                                }
                                                                }
                                                        }
                                                }
                                        )
                                }
                        }
                }

                val securityContext = buildJsonObject {
                        putJsonObject("caller") {
                                put("securityLevel", JsonPrimitive("owner"))
                                put(
                                        "isGrantedConnectedIdentitiesSystemCircle",
                                        JsonPrimitive(isGrantedAllConnected)
                                )
                        }
                        putJsonObject("permissionContext") {
                                putJsonArray("permissionGroups") { add(permissionGroup) }
                        }
                }
                return securityContext.toString()
        }

        // Helper to put JsonObject
        private fun JsonObjectBuilder.putJsonObject(
                key: String,
                block: JsonObjectBuilder.() -> Unit
        ) {
                put(key, buildJsonObject(block))
        }

        @Test
        fun testGetMissingPermissions_allGranted_returnsNull() = runTest {
                // Arrange
                val config =
                        createConfig(
                                permissions =
                                        listOf(
                                                AppPermissionType.ManageFeed,
                                                AppPermissionType.ReadConnections
                                        )
                        )
                val jsonResponse =
                        createSecurityContextJson(
                                permissions =
                                        listOf(
                                                AppPermissionType.ManageFeed.value,
                                                AppPermissionType.ReadConnections.value
                                        )
                        )

                val odinClient = MockOdinClientSetup.setupMockOdinClient(jsonResponse)
                val manager = PermissionExtensionManager.create(odinClient, hostIdentity)

                // Act
                val result = manager.getMissingPermissions(config)

                // Assert
                assertNull(result)
        }

        @Test
        fun testGetMissingPermissions_missingPermission_returnsResult() = runTest {
                // Arrange
                val config =
                        createConfig(
                                permissions =
                                        listOf(
                                                AppPermissionType.ManageFeed,
                                                AppPermissionType.ReadConnections
                                        )
                        )
                // Only grant ManageFeed
                val jsonResponse =
                        createSecurityContextJson(
                                permissions = listOf(AppPermissionType.ManageFeed.value)
                        )

                val odinClient = MockOdinClientSetup.setupMockOdinClient(jsonResponse)
                val manager = PermissionExtensionManager.create(odinClient, hostIdentity)

                // Act
                val result = manager.getMissingPermissions(config)

                // Assert
                assertNotNull(result)
                assertTrue(result.hasMissingPermissions)
                assertEquals(1, result.missingPermissions.size)
                assertEquals(AppPermissionType.ReadConnections, result.missingPermissions[0])
                assertTrue(
                        result.extendPermissionUrl.contains(
                                "p=${AppPermissionType.ReadConnections.value}"
                        )
                )
        }

        @Test
        fun testGetMissingPermissions_missingDrive_returnsResult() = runTest {
                // Arrange
                val targetDrive =
                        TargetDriveAccessRequest(
                                alias = "drive-alias",
                                type = "drive-type",
                                permissions = listOf(DrivePermissionType.Read),
                                name = "Test Drive",
                                description = "Test Desc"
                        )
                val config = createConfig(drives = listOf(targetDrive))

                // Grant no drives
                val jsonResponse = createSecurityContextJson()

                val odinClient = MockOdinClientSetup.setupMockOdinClient(jsonResponse)
                val manager = PermissionExtensionManager.create(odinClient, hostIdentity)

                // Act
                val result = manager.getMissingPermissions(config)

                // Assert
                assertNotNull(result)
                assertTrue(result.hasMissingPermissions)
                assertEquals(1, result.missingDrives.size)
                assertEquals(targetDrive.alias, result.missingDrives[0].alias)
                assertTrue(result.extendPermissionUrl.contains("d="))
        }

        @Test
        fun testGetMissingPermissions_driveInsufficientPermission_returnsResult() = runTest {
                // Arrange
                val targetDrive =
                        TargetDriveAccessRequest(
                                alias = "drive-alias",
                                type = "drive-type",
                                permissions =
                                        listOf(DrivePermissionType.Read, DrivePermissionType.Write),
                                name = "Test Drive",
                                description = "Test Desc"
                        )
                val config = createConfig(drives = listOf(targetDrive))

                // Grant only Read
                val grantedDrive =
                        PermissionedDrive(
                                drive = DriveReference("drive-alias", "drive-type"),
                                permission = listOf(DrivePermissionType.Read)
                        )
                val jsonResponse =
                        createSecurityContextJson(driveGrants = listOf(DriveGrant(grantedDrive)))

                val odinClient = MockOdinClientSetup.setupMockOdinClient(jsonResponse)
                val manager = PermissionExtensionManager.create(odinClient, hostIdentity)

                // Act
                val result = manager.getMissingPermissions(config)

                // Assert
                assertNotNull(result)
                assertTrue(result.hasMissingPermissions)
                assertEquals(1, result.missingDrives.size)
        }

        @Test
        fun testGetMissingPermissions_missingAllConnectedCircle_returnsResult() = runTest {
                // Arrange
                val config = createConfig(needsAllConnected = true)

                // Not granted
                val jsonResponse = createSecurityContextJson(isGrantedAllConnected = false)

                val odinClient = MockOdinClientSetup.setupMockOdinClient(jsonResponse)
                val manager = PermissionExtensionManager.create(odinClient, hostIdentity)

                // Act
                val result = manager.getMissingPermissions(config)

                // Assert
                assertNotNull(result)
                assertTrue(result.missingAllConnectedCircle)
                assertTrue(result.extendPermissionUrl.contains("c="))
        }
}
