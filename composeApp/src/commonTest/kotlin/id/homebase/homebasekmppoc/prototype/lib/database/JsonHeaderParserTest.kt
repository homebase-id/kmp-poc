package id.homebase.homebasekmppoc.prototype.lib.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith
import kotlin.uuid.Uuid

open class JsonHeaderParserTest {
    
    @Test
    fun parseJsonFileheaderToDriveMainIndex_withValidJson_returnsCorrectDriveMainIndex() {
        val jsonHeader = """
        {
            "fileId": "ca3caf19-5078-0200-da5b-4b7db1eccf27",
            "fileMetadata": {
                "globalTransitId": "52a491ac-9870-4d0c-94a1-1bf667393015",
                "created": 1765026867127,
                "updated": 1765026867127,
                "transitCreated": 1765026867126,
                "transitUpdated": 0,
                "isEncrypted": true,
                "senderOdinId": "fyr.mejlvang.casa",
                "originalAuthor": "fyr.mejlvang.casa",
                "appData": {
                    "uniqueId": "55d2e47e-ec86-f9b8-1e3d-d7bdeeb0527b",
                    "tags": null,
                    "fileType": 7878,
                    "dataType": 0,
                    "groupId": "1c762053-c163-4116-b8ae-2c1846bc0435",
                    "userDate": 1765026866188,
                    "content": {
                        "message": "done",
                        "deliveryStatus": 20
                    },
                    "previewThumbnail": null,
                    "archivalStatus": 0
                },
                "localAppData": null,
                "referencedFile": null,
                "reactionPreview": null,
                "versionTag": "ca3caf19-607b-b400-cb5e-fe345e061c5d",
                "payloads": [],
                "dataSource": null
            },
            "serverMetadata": {
                "accessControlList": {
                    "requiredSecurityGroup": "owner",
                    "circleIdList": null,
                    "odinIdList": null
                },
                "doNotIndex": false,
                "allowDistribution": false,
                "fileSystemType": "standard",
                "fileByteCount": 4876,
                "originalRecipientCount": 0,
                "transferHistory": null
            },
            "defaultPayload": {
                "message": "done",
                "deliveryStatus": 20
            }
        }
        """.trimIndent()
        
        val identityId = Uuid.random()
        val driveId = Uuid.random()
        
        val result = parseJsonHeaderToDriveMainIndex(identityId, driveId, jsonHeader)
        
        assertEquals(identityId, result.identityId)
        assertEquals(driveId, result.driveId)
        assertEquals(Uuid.parse("ca3caf19-5078-0200-da5b-4b7db1eccf27"), result.fileId)
        assertEquals(Uuid.parse("52a491ac-9870-4d0c-94a1-1bf667393015"), result.globalTransitId)
        assertEquals("fyr.mejlvang.casa", result.senderId)
        assertEquals(Uuid.parse("55d2e47e-ec86-f9b8-1e3d-d7bdeeb0527b"), result.uniqueId)
        assertEquals(Uuid.parse("1c762053-c163-4116-b8ae-2c1846bc0435"), result.groupId)
        assertEquals(7878L, result.fileType)
        assertEquals(0L, result.dataType)
        assertEquals(0L, result.archivalStatus)
        assertEquals(1765026866188L, result.userDate)
        assertEquals(1765026867127L, result.created)
        assertEquals(1765026867127L, result.modified)
        assertEquals(jsonHeader, result.jsonHeader)
    }
    
    @Test
    fun parseJsonFileheaderToDriveMainIndex_withNullValues_usesDefaults() {
        val jsonHeader = """
        {
            "fileId": "ca3caf19-5078-0200-da5b-4b7db1eccf27",
            "fileMetadata": {
                "globalTransitId": null,
                "created": 1765026867127,
                "senderOdinId": null,
                "appData": {
                    "uniqueId": null,
                    "fileType": null,
                    "dataType": null,
                    "groupId": null,
                    "userDate": null,
                    "archivalStatus": null
                }
            }
        }
        """.trimIndent()
        
        val identityId = Uuid.random()
        val driveId = Uuid.random()
        
        val result = parseJsonHeaderToDriveMainIndex(identityId, driveId,jsonHeader)
        
        assertEquals(identityId, result.identityId)
        assertEquals(driveId, result.driveId)
        assertEquals(Uuid.parse("ca3caf19-5078-0200-da5b-4b7db1eccf27"), result.fileId)
        assertEquals(null, result.globalTransitId)
        assertEquals(null, result.senderId)
        assertNotNull(result.uniqueId) // Should be random UUID
        assertEquals(null, result.groupId)
        assertEquals(0L, result.fileType) // Default
        assertEquals(0L, result.dataType) // Default
        assertEquals(0L, result.userDate) // Default
        assertEquals(0L, result.archivalStatus) // Default
    }
    
    @Test
    fun parseJsonFileheaderToDriveMainIndex_withMissingFileId_throwsException() {
        val jsonHeader = """
        {
            "fileMetadata": {
                "created": 1765026867127
            }
        }
        """.trimIndent()
        
        assertFailsWith<IllegalArgumentException>("Missing required field: fileId") {
            parseJsonHeaderToDriveMainIndex(Uuid.random(), Uuid.random(), jsonHeader)
        }
    }
    
    @Test
    fun parseJsonFileheaderToDriveMainIndex_withMissingFileMetadata_throwsException() {
        val jsonHeader = """
        {
            "fileId": "ca3caf19-5078-0200-da5b-4b7db1eccf27"
        }
        """.trimIndent()
        
        assertFailsWith<IllegalArgumentException>("Missing required field: fileMetadata") {
            parseJsonHeaderToDriveMainIndex(Uuid.random(), Uuid.random(), jsonHeader)
        }
    }
    
    @Test
    fun parseJsonFileheaderToDriveMainIndex_withInvalidJson_throwsException() {
        val jsonHeader = "{ invalid json }"
        
        assertFailsWith<IllegalArgumentException>() {
            parseJsonHeaderToDriveMainIndex(Uuid.random(), Uuid.random(), jsonHeader)
        }
    }
}