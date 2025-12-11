package id.homebase.homebasekmppoc.prototype.lib.database

import app.cash.sqldelight.db.SqlDriver
import id.homebase.homebasekmppoc.prototype.lib.core.time.UnixTimeUtc
import id.homebase.homebasekmppoc.lib.database.DriveLocalTagIndex
import id.homebase.homebasekmppoc.lib.database.DriveTagIndex
import id.homebase.homebasekmppoc.lib.database.OdinDatabase
import id.homebase.homebasekmppoc.prototype.lib.drives.SharedSecretEncryptedFileHeader
import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer

import id.homebase.homebasekmppoc.prototype.lib.drives.query.QueryBatchCursor
import id.homebase.homebasekmppoc.prototype.lib.drives.query.TimeRowCursor
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertContentEquals
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.time.Clock
import kotlin.uuid.Uuid

class MainIndexMetaTest {
    private var driver: SqlDriver? = null
    private lateinit var db: OdinDatabase

    private lateinit var cursorSync: CursorSync

    @BeforeTest
    fun setup() {
        driver = createInMemoryDatabase()
        db = TestDatabaseFactory.createTestDatabase(driver)
        cursorSync = CursorSync(db)
    }

    @AfterTest
    fun tearDown() {
        driver?.close()
    }

@Test
fun testUpsertDriveMainIndexHelper() = runTest {
        // Test data
        val identityId = Uuid.random()
        val driveId = Uuid.random()
        val fileId = Uuid.random()
        val currentTime = Clock.System.now().epochSeconds

        // Create JSON header with all required fields for SharedSecretEncryptedFileHeader
        val jsonHeader = """{
                "fileId": "${fileId}",
                "targetDrive": {
                    "alias": "${driveId}",
                    "type": "8f448716e34cedf9014145e043ca6612"
                },
                "fileState": "active",
                "fileSystemType": "standard",
                "sharedSecretEncryptedKeyHeader": {
                    "encryptionVersion": 1,
                    "type": "aes",
                    "iv": "fA2HYW8SoHnP3oMxgPcckA==",
                    "encryptedAesKey": "lCGJ4kL+OC2I+Q1YIvkTVU/GUpmVHAMA+axkwZQJxu5tGHAQd2CLzEzGX0X2pcyE"
                },
                "fileMetadata": {
                    "globalTransitId": "52a491ac-9870-4d0c-94a1-1bf667393015",
                    "created": ${currentTime}000,
                    "updated": ${currentTime}000,
                    "transitCreated": 0,
                    "transitUpdated": 0,
                    "isEncrypted": true,
                    "senderOdinId": "test-sender",
                    "originalAuthor": "test-sender",
                    "appData": {
                        "uniqueId": "55d2e47e-ec86-f9b8-1e3d-d7bdeeb0527b",
                        "tags": null,
                        "fileType": 1,
                        "dataType": 1,
                        "groupId": null,
                        "userDate": ${currentTime}000,
                        "content": "test message",
                        "previewThumbnail": null,
                        "archivalStatus": 1
                    },
                    "localAppData": null,
                    "referencedFile": null,
                    "reactionPreview": null,
                    "versionTag": "1355aa19-2031-d800-403d-e8696a8be494",
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
                    "fileByteCount": 1000,
                    "originalRecipientCount": 0,
                    "transferHistory": null
                },
                "priority": 300,
                "fileByteCount": 1000
            }"""

        // Deserialize JSON header to SharedSecretEncryptedFileHeader
        val header = OdinSystemSerializer.deserialize<SharedSecretEncryptedFileHeader>(jsonHeader)

        // Create FileMetadataProcessor instance to convert header to DriveMainIndex record
        val processor = FileHeaderProcessor(db)
        val driveMainIndexRecord = processor.convertFileHeaderToDriveMainIndexRecord(identityId, driveId, header)

        // Test the helper function
        MainIndexMetaHelpers.upsertDriveMainIndex(db, driveMainIndexRecord)

        // Verify the record was inserted
        val retrievedRecord = db.driveMainIndexQueries.selectByIdentityAndDriveAndFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        ).executeAsOneOrNull()

        assertNotNull(retrievedRecord, "Record should exist after upsert")
        assertEquals(identityId, retrievedRecord.identityId)
assertEquals(driveId, retrievedRecord.driveId)
        assertEquals(fileId, retrievedRecord.fileId)
        assertEquals("test-sender", retrievedRecord.senderId)
        // Note: byteCount is now consolidated in jsonHeader
    }

@Test
    fun testBaseUpsertEntryZapZapWithTags() = runTest {
        // Test data
        val identityId = Uuid.random()
        val driveId = Uuid.random()
        val fileId = Uuid.random()
        val currentTime = Clock.System.now().epochSeconds

        // Create JSON header with all required fields for SharedSecretEncryptedFileHeader
        val jsonHeader = """{
                "fileId": "${fileId}",
                "targetDrive": {
                    "alias": "${driveId}",
                    "type": "8f448716e34cedf9014145e043ca6612"
                },
                "fileState": "active",
                "fileSystemType": "standard",
                "sharedSecretEncryptedKeyHeader": {
                    "encryptionVersion": 1,
                    "type": "aes",
                    "iv": "fA2HYW8SoHnP3oMxgPcckA==",
                    "encryptedAesKey": "lCGJ4kL+OC2I+Q1YIvkTVU/GUpmVHAMA+axkwZQJxu5tGHAQd2CLzEzGX0X2pcyE"
                },
                "fileMetadata": {
                    "globalTransitId": "52a491ac-9870-4d0c-94a1-1bf667393015",
                    "created": ${currentTime}000,
                    "updated": ${currentTime}000,
                    "transitCreated": 0,
                    "transitUpdated": 0,
                    "isEncrypted": true,
                    "senderOdinId": "test-sender",
                    "originalAuthor": "test-sender",
                    "appData": {
                        "uniqueId": "55d2e47e-ec86-f9b8-1e3d-d7bdeeb0527b",
                        "tags": [
                            "bdaef89a-f262-8bd2-554f-380c4537e0e5"
                        ],
                        "fileType": 1,
                        "dataType": 1,
                        "groupId": null,
                        "userDate": ${currentTime}000,
                        "content": "test content",
                        "archivalStatus": 1
                    },
                    "localAppData": null,
                    "referencedFile": null,
                    "reactionPreview": null,
                    "versionTag": "1355aa19-2031-d800-403d-e8696a8be494",
                    "payloads": [
                        {
                            "iv": "3zvsfQ3qQbyup44mm1BAfw==",
                            "key": "pst_mdi0",
                            "contentType": "image/jpeg",
                            "bytesWritten": 5082368,
                            "lastModified": 1763710153486,
                            "descriptorContent": null,
                            "uid": 115586508618072064
                        }
                    ],
                    "dataSource": null
                },
                "serverMetadata": {
                    "accessControlList": {
                        "requiredSecurityGroup": "connected",
                        "circleIdList": null,
                        "odinIdList": null
                    },
                    "doNotIndex": false,
                    "allowDistribution": true,
                    "fileSystemType": "standard",
                    "fileByteCount": 5402950,
                    "originalRecipientCount": 0,
                    "transferHistory": null
                },
                "priority": 300,
                "fileByteCount": 5402950
            }"""

        // Create tag records
        val tagId1 = Uuid.random()
        val tagId2 = Uuid.random()
        listOf(
            DriveTagIndex(
                rowId = 1L,
                identityId = identityId,
                driveId = driveId,
                fileId = fileId,
                tagId = tagId1
            ),
            DriveTagIndex(
                rowId = 2L,
                identityId = identityId,
                driveId = driveId,
                fileId = fileId,
                tagId = tagId2
            )
        )

        // Create local tag records
        val localTagId1 = Uuid.random()
        listOf(
            DriveLocalTagIndex(
                rowId = 1L,
                identityId = identityId,
                driveId = driveId,
                fileId = fileId,
                tagId = localTagId1
            )
        )

        // Create FileMetadataProcessor instance to test BaseUpsertEntryZapZap
        val processor = FileHeaderProcessor(db)

        val originalCursor = QueryBatchCursor(
            pagingCursor = TimeRowCursor(
                time = UnixTimeUtc(1704067200000L), // 2024-01-01 00:00:00 UTC
                rowId = 12345L
            ),
            stopAtBoundary = TimeRowCursor(
                time = UnixTimeUtc(1704153600000L), // 2024-01-02 00:00:00 UTC
                rowId = 67890L
            ),
            nextBoundaryCursor = TimeRowCursor(
                time = UnixTimeUtc(1704240000000L), // 2024-01-03 00:00:00 UTC
                rowId = 11111L
            )
        )

        // Deserialize JSON header to SharedSecretEncryptedFileHeader
        val header = OdinSystemSerializer.deserialize<SharedSecretEncryptedFileHeader>(jsonHeader)

        // Call BaseUpsertEntryZapZap function
        processor.BaseUpsertEntryZapZap(
            identityId = identityId,
            driveId = driveId,
            fileHeader = header,
            cursor = originalCursor
        )

        // Verify the main record was inserted
        val retrievedRecord = db.driveMainIndexQueries.selectByIdentityAndDriveAndFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        ).executeAsOneOrNull()

        assertNotNull(retrievedRecord, "Record should exist after BaseUpsertEntryZapZap")
        assertEquals(identityId, retrievedRecord.identityId)
        assertEquals("test-sender", retrievedRecord.senderId)

        val loadedCursor = cursorSync.loadCursor()
        assertNotNull(loadedCursor!!.pagingCursor, "Paging cursor should not be null")
        assertNotNull(loadedCursor.stopAtBoundary, "Stop at boundary cursor should not be null")
        assertNotNull(loadedCursor.nextBoundaryCursor, "Next boundary cursor should not be null")

        // Verify paging cursor fields
        assertEquals(
            originalCursor.pagingCursor!!.time,
            loadedCursor.pagingCursor.time,
            "Paging cursor time should match"
        )
        assertEquals(
            originalCursor.pagingCursor.rowId,
            loadedCursor.pagingCursor.rowId,
            "Paging cursor row ID should match"
        )

        // Verify stop at boundary cursor fields
        assertEquals(
            originalCursor.stopAtBoundary!!.time,
            loadedCursor.stopAtBoundary.time,
            "Stop at boundary cursor time should match"
        )
        assertEquals(
            originalCursor.stopAtBoundary.rowId,
            loadedCursor.stopAtBoundary.rowId,
            "Stop at boundary cursor row ID should match"
        )

        // Verify next boundary cursor fields
        assertEquals(
            originalCursor.nextBoundaryCursor!!.time,
            loadedCursor.nextBoundaryCursor.time,
            "Next boundary cursor time should match"
        )
        assertEquals(
            originalCursor.nextBoundaryCursor.rowId,
            loadedCursor.nextBoundaryCursor.rowId,
            "Next boundary cursor row ID should match"
        )

        processor.deleteEntryDriveMainIndex(db, identityId, driveId, fileId)
        assertEquals(db.driveMainIndexQueries.countAll().executeAsOne(), 0L)
        assertEquals(db.driveTagIndexQueries.countAll().executeAsOne(), 0L)
        assertEquals(db.driveLocalTagIndexQueries.countAll().executeAsOne(), 0L)
    }

@Test
    fun testBaseUpsertEntryZapZapWithNullCursor() = runTest {
        // Test data
        val identityId = Uuid.random()
        val driveId = Uuid.random()
        val fileId = Uuid.random()
        val currentTime = Clock.System.now().epochSeconds

        // Create JSON header with all required fields for SharedSecretEncryptedFileHeader
        val jsonHeader = """{
                "fileId": "${fileId}",
                "targetDrive": {
                    "alias": "${driveId}",
                    "type": "8f448716e34cedf9014145e043ca6612"
                },
                "fileState": "active",
                "fileSystemType": "standard",
                "sharedSecretEncryptedKeyHeader": {
                    "encryptionVersion": 1,
                    "type": "aes",
                    "iv": "fA2HYW8SoHnP3oMxgPcckA==",
                    "encryptedAesKey": "lCGJ4kL+OC2I+Q1YIvkTVU/GUpmVHAMA+axkwZQJxu5tGHAQd2CLzEzGX0X2pcyE"
                },
                "fileMetadata": {
                    "globalTransitId": "52a491ac-9870-4d0c-94a1-1bf667393015",
                    "created": ${currentTime}000,
                    "updated": ${currentTime}000,
                    "transitCreated": 0,
                    "transitUpdated": 0,
                    "isEncrypted": true,
                    "senderOdinId": "test-sender",
                    "originalAuthor": "test-sender",
                    "appData": {
                        "uniqueId": "55d2e47e-ec86-f9b8-1e3d-d7bdeeb0527b",
                        "tags": null,
                        "fileType": 1,
                        "dataType": 1,
                        "groupId": null,
                        "userDate": ${currentTime}000,
                        "content": "test content",
                        "archivalStatus": 1
                    },
                    "localAppData": null,
                    "referencedFile": null,
                    "reactionPreview": null,
                    "versionTag": "1355aa19-2031-d800-403d-e8696a8be494",
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
                    "fileByteCount": 1000,
                    "originalRecipientCount": 0,
                    "transferHistory": null
                },
                "priority": 300,
                "fileByteCount": 1000
            }"""

        // Create tag records
        listOf<DriveTagIndex>()
        listOf<DriveLocalTagIndex>()

        // Create FileMetadataProcessor instance to test BaseUpsertEntryZapZap
        val processor = FileHeaderProcessor(db)

        // Deserialize JSON header to SharedSecretEncryptedFileHeader
        val header = OdinSystemSerializer.deserialize<SharedSecretEncryptedFileHeader>(jsonHeader)

        // Call BaseUpsertEntryZapZap function with null cursor
        processor.BaseUpsertEntryZapZap(
            identityId = identityId,
            driveId = driveId,
            fileHeader = header,
            cursor = null
        )

        // Verify the main record was inserted
        val retrievedRecord = db.driveMainIndexQueries.selectByIdentityAndDriveAndFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        ).executeAsOneOrNull()

        assertNotNull(retrievedRecord, "Record should exist after BaseUpsertEntryZapZap with null cursor")
        assertEquals(identityId, retrievedRecord.identityId)
        assertEquals("test-sender", retrievedRecord.senderId)
    }

    @Test
    fun testBaseUpsertEntryZapZapWithExistingTags() = runTest {
        // Test data
        val identityId = Uuid.random()
        val driveId = Uuid.random()
        val fileId = Uuid.random()
        val currentTime = Clock.System.now().epochSeconds

        // First, insert some existing tags to test deletion
        val existingTagId1 = Uuid.random()
        val existingTagId2 = Uuid.random()
        
        db.driveTagIndexQueries.insertTag(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId,
            tagId = existingTagId1
        )
        
        db.driveTagIndexQueries.insertTag(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId,
            tagId = existingTagId2
        )

        // Create new tag records (different from existing)
        val newTagId1 = Uuid.random()
        val newTagId2 = Uuid.random()

        // Create JSON header with all required fields for SharedSecretEncryptedFileHeader
        val jsonHeader = """{
                "fileId": "${fileId}",
                "targetDrive": {
                    "alias": "${driveId}",
                    "type": "8f448716e34cedf9014145e043ca6612"
                },
                "fileState": "active",
                "fileSystemType": "standard",
                "sharedSecretEncryptedKeyHeader": {
                    "encryptionVersion": 1,
                    "type": "aes",
                    "iv": "fA2HYW8SoHnP3oMxgPcckA==",
                    "encryptedAesKey": "lCGJ4kL+OC2I+Q1YIvkTVU/GUpmVHAMA+axkwZQJxu5tGHAQd2CLzEzGX0X2pcyE"
                },
                "fileMetadata": {
                    "globalTransitId": "52a491ac-9870-4d0c-94a1-1bf667393015",
                    "created": ${currentTime}000,
                    "updated": ${currentTime}000,
                    "transitCreated": 0,
                    "transitUpdated": 0,
                    "isEncrypted": true,
                    "senderOdinId": "test-sender",
                    "originalAuthor": "test-sender",
                    "appData": {
                        "uniqueId": "55d2e47e-ec86-f9b8-1e3d-d7bdeeb0527b",
                        "tags": ["${newTagId1}", "${newTagId2}"],
                        "fileType": 1,
                        "dataType": 1,
                        "groupId": null,
                        "userDate": ${currentTime}000,
                        "content": "test content",
                        "archivalStatus": 1
                    },
                    "localAppData": null,
                    "referencedFile": null,
                    "reactionPreview": null,
                    "versionTag": "1355aa19-2031-d800-403d-e8696a8be494",
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
                    "fileByteCount": 1000,
                    "originalRecipientCount": 0,
                    "transferHistory": null
                },
                "priority": 300,
                "fileByteCount": 1000
            }"""

        // Create FileMetadataProcessor instance to test BaseUpsertEntryZapZap
        val processor = FileHeaderProcessor(db)
        
        // Deserialize JSON header to SharedSecretEncryptedFileHeader
        val fileHeader = OdinSystemSerializer.deserialize<SharedSecretEncryptedFileHeader>(jsonHeader)
        
        processor.BaseUpsertEntryZapZap(
            identityId = identityId,
            driveId = driveId,
            fileHeader = fileHeader,
            cursor = null
        )

        // Verify the main record was inserted
        val retrievedRecord = db.driveMainIndexQueries.selectByIdentityAndDriveAndFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        ).executeAsOneOrNull()

        assertNotNull(retrievedRecord, "Record should exist after BaseUpsertEntryZapZap")
        assertEquals(identityId, retrievedRecord.identityId)
        assertEquals("test-sender", retrievedRecord.senderId)

        // Verify that old tags were deleted and new tags were inserted
        val finalTags = db.driveTagIndexQueries.selectByFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        ).executeAsList()

        assertEquals(2, finalTags.size, "Should have exactly 2 tags after BaseUpsertEntryZapZap")
        
        val finalTagIds = finalTags.map { it.tagId }.toSet()
        assertTrue(finalTagIds.contains(newTagId1), "Should contain new tag 1")
        assertTrue(finalTagIds.contains(newTagId2), "Should contain new tag 2")
        assertFalse(finalTagIds.contains(existingTagId1), "Should not contain old tag 1")
        assertFalse(finalTagIds.contains(existingTagId2), "Should not contain old tag 2")
    }

    @Test
    fun testConvertDriveMainIndexRecordToFileHeader_RoundTrip() = runTest {
        // Test data
        val identityId = Uuid.random()
        val driveId = Uuid.random()
        val fileId = Uuid.random()
        val currentTime = Clock.System.now().epochSeconds

        // Create JSON header with all required fields for SharedSecretEncryptedFileHeader
        val jsonHeader = """{
                "fileId": "${fileId}",
                "targetDrive": {
                    "alias": "${driveId}",
                    "type": "8f448716e34cedf9014145e043ca6612"
                },
                "fileState": "active",
                "fileSystemType": "standard",
                "sharedSecretEncryptedKeyHeader": {
                    "encryptionVersion": 1,
                    "type": "aes",
                    "iv": "fA2HYW8SoHnP3oMxgPcckA==",
                    "encryptedAesKey": "lCGJ4kL+OC2I+Q1YIvkTVU/GUpmVHAMA+axkwZQJxu5tGHAQd2CLzEzGX0X2pcyE"
                },
                "fileMetadata": {
                    "globalTransitId": "52a491ac-9870-4d0c-94a1-1bf667393015",
                    "created": ${currentTime}000,
                    "updated": ${currentTime}000,
                    "transitCreated": 0,
                    "transitUpdated": 0,
                    "isEncrypted": true,
                    "senderOdinId": "test-sender",
                    "originalAuthor": "test-sender",
                    "appData": {
                        "uniqueId": "55d2e47e-ec86-f9b8-1e3d-d7bdeeb0527b",
                        "tags": null,
                        "fileType": 1,
                        "dataType": 1,
                        "groupId": null,
                        "userDate": ${currentTime}000,
                        "content": "test content",
                        "archivalStatus": 1
                    },
                    "localAppData": null,
                    "referencedFile": null,
                    "reactionPreview": null,
                    "versionTag": "1355aa19-2031-d800-403d-e8696a8be494",
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
                    "fileByteCount": 1000,
                    "originalRecipientCount": 0,
                    "transferHistory": null
                },
                "priority": 300,
                "fileByteCount": 1000
            }"""

        // Create FileHeaderProcessor instance
        val processor = FileHeaderProcessor(db)

        // Deserialize JSON header to SharedSecretEncryptedFileHeader
        val originalHeader = OdinSystemSerializer.deserialize<SharedSecretEncryptedFileHeader>(jsonHeader)

        // Convert to DriveMainIndex record
        val driveMainIndexRecord = processor.convertFileHeaderToDriveMainIndexRecord(identityId, driveId, originalHeader)

        // Convert back to SharedSecretEncryptedFileHeader
        val reconstructedHeader = processor.convertDriveMainIndexRecordToFileHeader(driveMainIndexRecord)

        // Verify round-trip conversion preserves all data
        assertEquals(originalHeader.fileId, reconstructedHeader.fileId)
        assertEquals(originalHeader.targetDrive.alias, reconstructedHeader.targetDrive.alias)
        assertEquals(originalHeader.targetDrive.type, reconstructedHeader.targetDrive.type)
        assertEquals(originalHeader.fileState, reconstructedHeader.fileState)
        assertEquals(originalHeader.fileSystemType, reconstructedHeader.fileSystemType)
        assertEquals(originalHeader.sharedSecretEncryptedKeyHeader.encryptionVersion, reconstructedHeader.sharedSecretEncryptedKeyHeader.encryptionVersion)
        assertEquals(originalHeader.sharedSecretEncryptedKeyHeader.type, reconstructedHeader.sharedSecretEncryptedKeyHeader.type)
        assertContentEquals(originalHeader.sharedSecretEncryptedKeyHeader.iv, reconstructedHeader.sharedSecretEncryptedKeyHeader.iv)
        assertContentEquals(originalHeader.sharedSecretEncryptedKeyHeader.encryptedAesKey, reconstructedHeader.sharedSecretEncryptedKeyHeader.encryptedAesKey)
        
        // Verify file metadata
        assertEquals(originalHeader.fileMetadata.globalTransitId, reconstructedHeader.fileMetadata.globalTransitId)
        assertEquals(originalHeader.fileMetadata.created, reconstructedHeader.fileMetadata.created)
        assertEquals(originalHeader.fileMetadata.updated, reconstructedHeader.fileMetadata.updated)
        assertEquals(originalHeader.fileMetadata.isEncrypted, reconstructedHeader.fileMetadata.isEncrypted)
        assertEquals(originalHeader.fileMetadata.senderOdinId, reconstructedHeader.fileMetadata.senderOdinId)
        assertEquals(originalHeader.fileMetadata.originalAuthor, reconstructedHeader.fileMetadata.originalAuthor)
        
        // Verify app data
        assertEquals(originalHeader.fileMetadata.appData.uniqueId, reconstructedHeader.fileMetadata.appData.uniqueId)
        assertEquals(originalHeader.fileMetadata.appData.fileType, reconstructedHeader.fileMetadata.appData.fileType)
        assertEquals(originalHeader.fileMetadata.appData.dataType, reconstructedHeader.fileMetadata.appData.dataType)
        assertEquals(originalHeader.fileMetadata.appData.userDate, reconstructedHeader.fileMetadata.appData.userDate)
        assertEquals(originalHeader.fileMetadata.appData.content, reconstructedHeader.fileMetadata.appData.content)
        assertEquals(originalHeader.fileMetadata.appData.archivalStatus, reconstructedHeader.fileMetadata.appData.archivalStatus)
        
        // Verify server metadata
        assertEquals(originalHeader.serverMetadata.accessControlList?.requiredSecurityGroup, reconstructedHeader.serverMetadata.accessControlList?.requiredSecurityGroup)
        //assertEquals(originalHeader.serverMetadata.doNotIndex, reconstructedHeader.serverMetadata.doNotIndex)
        assertEquals(originalHeader.serverMetadata.allowDistribution, reconstructedHeader.serverMetadata.allowDistribution)
        assertEquals(originalHeader.serverMetadata.fileSystemType, reconstructedHeader.serverMetadata.fileSystemType)
        assertEquals(originalHeader.serverMetadata.fileByteCount, reconstructedHeader.serverMetadata.fileByteCount)
        assertEquals(originalHeader.serverMetadata.originalRecipientCount, reconstructedHeader.serverMetadata.originalRecipientCount)
        
        // Verify other fields
        assertEquals(originalHeader.priority, reconstructedHeader.priority)
        assertEquals(originalHeader.fileByteCount, reconstructedHeader.fileByteCount)
    }
}