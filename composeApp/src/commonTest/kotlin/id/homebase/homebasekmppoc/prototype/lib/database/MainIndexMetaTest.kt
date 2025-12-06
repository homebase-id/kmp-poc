package id.homebase.homebasekmppoc.prototype.lib.database

import app.cash.sqldelight.db.SqlDriver
import id.homebase.homebasekmppoc.lib.database.DriveLocalTagIndex
import id.homebase.homebasekmppoc.lib.database.DriveTagIndex
import id.homebase.homebasekmppoc.lib.database.OdinDatabase
import id.homebase.homebasekmppoc.prototype.lib.core.time.UnixTimeUtc
import id.homebase.homebasekmppoc.prototype.lib.drives.query.QueryBatchCursor
import id.homebase.homebasekmppoc.prototype.lib.drives.query.TimeRowCursor
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
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

        // Create JSON header
        val jsonHeader = """{
                "fileId": "${fileId}",
                "fileMetadata": {
                    "globalTransitId": "52a491ac-9870-4d0c-94a1-1bf667393015",
                    "created": ${currentTime},
                    "updated": ${currentTime},
                    "transitCreated": ${currentTime},
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
                        "userDate": ${currentTime},
                        "content": {
                            "message": "test message",
                            "deliveryStatus": 20
                        },
                        "previewThumbnail": null,
                        "archivalStatus": 1
                    },
                    "localAppData": null,
                    "referencedFile": null,
                    "reactionPreview": null,
                    "versionTag": "test-version-tag",
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
                "defaultPayload": {
                    "message": "test message",
                    "deliveryStatus": 20
                }
            }"""

        // Parse JSON to create DriveMainIndex using our helper function
        val parsedResult = parseJsonHeaderToDriveMainIndex(identityId, driveId, jsonHeader)

        // Test the helper function
        MainIndexMetaHelpers.upsertDriveMainIndex(db, parsedResult.driveMainIndex)

        // Verify the record was inserted
        val retrievedRecord = db.driveMainIndexQueries.selectByIdentityAndDriveAndFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        ).executeAsOneOrNull()

        assertNotNull(retrievedRecord, "Record should exist after upsert")
        assertEquals(identityId, retrievedRecord?.identityId)
assertEquals(driveId, retrievedRecord?.driveId)
        assertEquals(fileId, retrievedRecord?.fileId)
        assertEquals("test-sender", retrievedRecord?.senderId)
        // Note: byteCount is now consolidated in jsonHeader
    }

@Test
    fun testBaseUpsertEntryZapZapWithTags() = runTest {
        // Test data
        val identityId = Uuid.random()
        val driveId = Uuid.random()
        val fileId = Uuid.random()
        val currentTime = Clock.System.now().epochSeconds

        // Create JSON header
        val jsonHeader = """{
                "fileId": "${fileId}",
                "fileMetadata": {
                    "globalTransitId": "52a491ac-9870-4d0c-94a1-1bf667393015",
                    "created": ${currentTime},
                    "updated": ${currentTime},
                    "transitCreated": ${currentTime},
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
                        "userDate": ${currentTime},
                        "content": {
                            "message": "test message",
                            "deliveryStatus": 20
                        },
                        "previewThumbnail": null,
                        "archivalStatus": 1
                    },
                    "localAppData": null,
                    "referencedFile": null,
                    "reactionPreview": null,
                    "versionTag": "test-version-tag",
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
                "defaultPayload": {
                    "message": "test message",
                    "deliveryStatus": 20
                }
            }"""

        // Create tag records
        val tagId1 = Uuid.random()
        val tagId2 = Uuid.random()
        val tagIndexRecords = listOf(
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
        val localTagIndexRecords = listOf(
            DriveLocalTagIndex(
                rowId = 1L,
                identityId = identityId,
                driveId = driveId,
                fileId = fileId,
                tagId = localTagId1
            )
        )

        // Create FileMetadataProcessor instance to test BaseUpsertEntryZapZap
        val processor = FileMetadataProcessor(db)

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

// Call BaseUpsertEntryZapZap function
        processor.BaseUpsertEntryZapZap(
            identityId = identityId,
            driveId = driveId,
            jsonHeader = jsonHeader,
            cursor = originalCursor
        )

        // Verify the main record was inserted
        val retrievedRecord = db.driveMainIndexQueries.selectByIdentityAndDriveAndFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        ).executeAsOneOrNull()

assertNotNull(retrievedRecord, "Record should exist after BaseUpsertEntryZapZap")
        assertEquals(identityId, retrievedRecord?.identityId)
        assertEquals("test-sender", retrievedRecord?.senderId)

        val loadedCursor = cursorSync.loadCursor();
        assertNotNull(loadedCursor!!.pagingCursor, "Paging cursor should not be null")
        assertNotNull(loadedCursor.stopAtBoundary, "Stop at boundary cursor should not be null")
        assertNotNull(loadedCursor.nextBoundaryCursor, "Next boundary cursor should not be null")

        // Verify paging cursor fields
        assertEquals(
            originalCursor.pagingCursor!!.time,
            loadedCursor.pagingCursor!!.time,
            "Paging cursor time should match"
        )
        assertEquals(
            originalCursor.pagingCursor!!.rowId,
            loadedCursor.pagingCursor!!.rowId,
            "Paging cursor row ID should match"
        )

        // Verify stop at boundary cursor fields
        assertEquals(
            originalCursor.stopAtBoundary!!.time,
            loadedCursor.stopAtBoundary!!.time,
            "Stop at boundary cursor time should match"
        )
        assertEquals(
            originalCursor.stopAtBoundary!!.rowId,
            loadedCursor.stopAtBoundary!!.rowId,
            "Stop at boundary cursor row ID should match"
        )

        // Verify next boundary cursor fields
        assertEquals(
            originalCursor.nextBoundaryCursor!!.time,
            loadedCursor.nextBoundaryCursor!!.time,
            "Next boundary cursor time should match"
        )
        assertEquals(
            originalCursor.nextBoundaryCursor!!.rowId,
            loadedCursor.nextBoundaryCursor!!.rowId,
            "Next boundary cursor row ID should match"
        )

        processor.deleteEntryDriveMainIndex(db, identityId, driveId, fileId);
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

        // Create JSON header
        val jsonHeader = """{
                "fileId": "${fileId}",
                "fileMetadata": {
                    "globalTransitId": "52a491ac-9870-4d0c-94a1-1bf667393015",
                    "created": ${currentTime},
                    "updated": ${currentTime},
                    "transitCreated": ${currentTime},
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
                        "userDate": ${currentTime},
                        "content": {
                            "message": "test message",
                            "deliveryStatus": 20
                        },
                        "previewThumbnail": null,
                        "archivalStatus": 1
                    },
                    "localAppData": null,
                    "referencedFile": null,
                    "reactionPreview": null,
                    "versionTag": "test-version-tag",
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
                "defaultPayload": {
                    "message": "test message",
                    "deliveryStatus": 20
                }
            }"""

        // Create tag records
        val tagIndexRecords = listOf<DriveTagIndex>()
        val localTagIndexRecords = listOf<DriveLocalTagIndex>()

        // Create FileMetadataProcessor instance to test BaseUpsertEntryZapZap
        val processor = FileMetadataProcessor(db)

// Call BaseUpsertEntryZapZap function with null cursor
        processor.BaseUpsertEntryZapZap(
            identityId = identityId,
            driveId = driveId,
            jsonHeader = jsonHeader,
            cursor = null
        )

        // Verify the main record was inserted
        val retrievedRecord = db.driveMainIndexQueries.selectByIdentityAndDriveAndFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        ).executeAsOneOrNull()

assertNotNull(retrievedRecord, "Record should exist after BaseUpsertEntryZapZap with null cursor")
        assertEquals(identityId, retrievedRecord?.identityId)
        assertEquals("test-sender", retrievedRecord?.senderId)
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

        // Create JSON header
        val jsonHeader = """{
                "fileId": "${fileId}",
                "fileMetadata": {
                    "globalTransitId": "52a491ac-9870-4d0c-94a1-1bf667393015",
                    "created": ${currentTime},
                    "updated": ${currentTime},
                    "transitCreated": ${currentTime},
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
                        "userDate": ${currentTime},
                        "content": {
                            "message": "test message",
                            "deliveryStatus": 20
                        },
                        "previewThumbnail": null,
                        "archivalStatus": 1
                    },
                    "localAppData": null,
                    "referencedFile": null,
                    "reactionPreview": null,
                    "versionTag": "test-version-tag",
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
                "defaultPayload": {
                    "message": "test message",
                    "deliveryStatus": 20
                }
            }"""

        // Create FileMetadataProcessor instance to test BaseUpsertEntryZapZap
        val processor = FileMetadataProcessor(db)
        processor.BaseUpsertEntryZapZap(
            identityId = identityId,
            driveId = driveId,
            jsonHeader = jsonHeader,
            cursor = null
        )

        // Verify the main record was inserted
        val retrievedRecord = db.driveMainIndexQueries.selectByIdentityAndDriveAndFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        ).executeAsOneOrNull()

        assertNotNull(retrievedRecord, "Record should exist after BaseUpsertEntryZapZap")
        assertEquals(identityId, retrievedRecord?.identityId)
        assertEquals("test-sender", retrievedRecord?.senderId)

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
}