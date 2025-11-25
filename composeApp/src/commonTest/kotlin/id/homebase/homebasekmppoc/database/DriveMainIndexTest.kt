package id.homebase.homebasekmppoc.database

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

class DriveMainIndexTest {

    private var driver: SqlDriver? = null
    private lateinit var db: OdinDatabase

    @BeforeTest
    fun setup() {
        driver = createInMemoryDatabase()
        db = OdinDatabase(driver!!)
    }

    @AfterTest
    fun tearDown() {
        driver?.close()
    }

    @Test
    fun testUpsertSelectByIdentityAndDriveAndFile() = runTest {

        // Test data - create sample byte arrays and values
        val randomId = Random.nextLong()
        val currentTime = Random.nextLong()
        val identityId = "test-identity".encodeToByteArray()
        val driveId = "test-drive".encodeToByteArray()
        val fileId = "test-file-$currentTime".encodeToByteArray()
        val globalTransitId = "global-transit-$randomId".encodeToByteArray()
        val uniqueId = "unique-id-$randomId".encodeToByteArray()
        val groupId = "group-$randomId".encodeToByteArray()
        val hdrVersionTag = "hdr-version-$randomId".encodeToByteArray()
        val hdrLocalVersionTag = "hdr-local-version-$randomId".encodeToByteArray()

        // Test values
        val fileState = 1
        val requiredSecurityGroup = 2
        val fileSystemType = 3
        val userDate = currentTime
        val fileType = 4
        val dataType = 5
        val archivalStatus = 6
        val historyStatus = 7
        val senderId = "sender-$randomId"
        val byteCount = 1024L
        val hdrEncryptedKeyHeader = "encrypted-key-header"
        val hdrAppData = "app-data"
        val hdrLocalAppData = "local-app-data"
        val hdrReactionSummary = "reaction-summary"
        val hdrServerData = "server-data"
        val hdrTransferHistory = "transfer-history"
        val hdrFileMetaData = "file-metadata"
        val created = currentTime
        val modified = currentTime + 1000

        // Insert a record using upsertDriveMainIndex (for testing)
        db.driveMainIndexQueries.upsertDriveMainIndex(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId,
            globalTransitId = globalTransitId,
            fileState = fileState.toLong(),
            requiredSecurityGroup = requiredSecurityGroup.toLong(),
            fileSystemType = fileSystemType.toLong(),
            userDate = userDate,
            fileType = fileType.toLong(),
            dataType = dataType.toLong(),
            archivalStatus = archivalStatus.toLong(),
            historyStatus = historyStatus.toLong(),
            senderId = senderId,
            groupId = groupId,
            uniqueId = uniqueId,
            byteCount = byteCount,
            hdrEncryptedKeyHeader = hdrEncryptedKeyHeader,
            hdrVersionTag = hdrVersionTag,
            hdrAppData = hdrAppData,
            hdrLocalVersionTag = hdrLocalVersionTag,
            hdrLocalAppData = hdrLocalAppData,
            hdrReactionSummary = hdrReactionSummary,
            hdrServerData = hdrServerData,
            hdrTransferHistory = hdrTransferHistory,
            hdrFileMetaData = hdrFileMetaData,
            created = created,
            modified = modified
        )

        // Select the record by identity, drive, and file
        val selectedRecord = db.driveMainIndexQueries.selectByIdentityAndDriveAndFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        ).executeAsOne()

        // Verify all fields match
        assertEquals(identityId.toList(), selectedRecord.identityId.toList())
        assertEquals(driveId.toList(), selectedRecord.driveId.toList())
        assertEquals(fileId.toList(), selectedRecord.fileId.toList())
        assertEquals(globalTransitId.toList(), selectedRecord.globalTransitId?.toList())
        assertEquals(fileState.toLong(), selectedRecord.fileState)
        assertEquals(requiredSecurityGroup.toLong(), selectedRecord.requiredSecurityGroup)
        assertEquals(fileSystemType.toLong(), selectedRecord.fileSystemType)
        assertEquals(userDate, selectedRecord.userDate)
        assertEquals(fileType.toLong(), selectedRecord.fileType)
        assertEquals(dataType.toLong(), selectedRecord.dataType)
        assertEquals(archivalStatus.toLong(), selectedRecord.archivalStatus)
        assertEquals(historyStatus.toLong(), selectedRecord.historyStatus)
        assertEquals(senderId, selectedRecord.senderId)
        assertEquals(groupId.toList(), selectedRecord.groupId?.toList())
        assertEquals(uniqueId.toList(), selectedRecord.uniqueId?.toList())
        assertEquals(byteCount, selectedRecord.byteCount)
        assertEquals(hdrEncryptedKeyHeader, selectedRecord.hdrEncryptedKeyHeader)
        assertEquals(hdrVersionTag.toList(), selectedRecord.hdrVersionTag.toList())
        assertEquals(hdrAppData, selectedRecord.hdrAppData)
        assertEquals(hdrLocalVersionTag.toList(), selectedRecord.hdrLocalVersionTag?.toList())
        assertEquals(hdrLocalAppData, selectedRecord.hdrLocalAppData)
        assertEquals(hdrReactionSummary, selectedRecord.hdrReactionSummary)
        assertEquals(hdrServerData, selectedRecord.hdrServerData)
        assertEquals(hdrTransferHistory, selectedRecord.hdrTransferHistory)
        assertEquals(hdrFileMetaData, selectedRecord.hdrFileMetaData)
        assertEquals(created, selectedRecord.created)
        assertEquals(modified, selectedRecord.modified)
    }

    @Test
    fun testUpsertUpdateExistingRecord() = runTest {

        // Test data
        val currentTime = Random.nextLong()
        val identityId = "test-identity-update".encodeToByteArray()
        val driveId = "test-drive-update".encodeToByteArray()
        val fileId = "test-file-update".encodeToByteArray()
        val globalTransitId = "global-transit-original".encodeToByteArray()
        val uniqueId = "unique-id-original".encodeToByteArray()
        val hdrVersionTag = "hdr-version-original".encodeToByteArray()

        // Insert initial record
        db.driveMainIndexQueries.upsertDriveMainIndex(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId,
            globalTransitId = globalTransitId,
            fileState = 1L,
            requiredSecurityGroup = 2L,
            fileSystemType = 3L,
            userDate = currentTime,
            fileType = 4L,
            dataType = 5L,
            archivalStatus = 6L,
            historyStatus = 7L,
            senderId = "original-sender",
            groupId = null,
            uniqueId = uniqueId,
            byteCount = 1024L,
            hdrEncryptedKeyHeader = "original-key-header",
            hdrVersionTag = hdrVersionTag,
            hdrAppData = "original-app-data",
            hdrLocalVersionTag = null,
            hdrLocalAppData = null,
            hdrReactionSummary = null,
            hdrServerData = "original-server-data",
            hdrTransferHistory = null,
            hdrFileMetaData = "original-metadata",
            created = currentTime,
            modified = currentTime
        )

        // Update the record with new values
        val updatedTime = currentTime + 5000
        val updatedGlobalTransitId = "global-transit-updated".encodeToByteArray()
        val updatedUniqueId = "unique-id-updated".encodeToByteArray()

        db.driveMainIndexQueries.upsertDriveMainIndex(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId,
            globalTransitId = updatedGlobalTransitId,
            fileState = 11L,
            requiredSecurityGroup = 22L,
            fileSystemType = 33L,
            userDate = updatedTime,
            fileType = 44L,
            dataType = 55L,
            archivalStatus = 66L,
            historyStatus = 77L,
            senderId = "updated-sender",
            groupId = "updated-group".encodeToByteArray(),
            uniqueId = updatedUniqueId,
            byteCount = 2048L,
            hdrEncryptedKeyHeader = "updated-key-header",
            hdrVersionTag = "hdr-version-updated".encodeToByteArray(),
            hdrAppData = "updated-app-data",
            hdrLocalVersionTag = "hdr-local-version-updated".encodeToByteArray(),
            hdrLocalAppData = "updated-local-app-data",
            hdrReactionSummary = "updated-reaction-summary",
            hdrServerData = "updated-server-data",
            hdrTransferHistory = "updated-transfer-history",
            hdrFileMetaData = "updated-metadata",
            created = currentTime,
            modified = updatedTime
        )

        // Select and verify the new record (simulating update by inserting different record)
        val updatedRecord = db.driveMainIndexQueries.selectByIdentityAndDriveAndFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        ).executeAsOne()

        assertEquals(updatedGlobalTransitId.toList(), updatedRecord.globalTransitId?.toList())
        assertEquals(11L, updatedRecord.fileState)
        assertEquals("updated-sender", updatedRecord.senderId)
        assertEquals(updatedUniqueId.toList(), updatedRecord.uniqueId?.toList())
        assertEquals(2048L, updatedRecord.byteCount)
        assertEquals("updated-app-data", updatedRecord.hdrAppData)
        assertEquals(updatedTime, updatedRecord.modified)
        assertEquals(currentTime, updatedRecord.created)
    }

    @Test
    fun testSelectByIdentityAndDriveAndFileWithNonExistentRecord() = runTest {

        // Test data for non-existent record
        val identityId = "non-existent-identity".encodeToByteArray()
        val driveId = "non-existent-drive".encodeToByteArray()
        val fileId = "non-existent-file".encodeToByteArray()

        // Try to select non-existent record
        val records = db.driveMainIndexQueries.selectByIdentityAndDriveAndFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        ).executeAsList()

        // Verify no records found
        assertTrue(records.isEmpty(), "Should have no records for non-existent file")
    }

    @Test
    fun testCountAllAndSelectAll() = runTest {

        // Initially should be empty
        assertEquals(0L, db.driveMainIndexQueries.countAll().executeAsOne())

        // Insert some test records
        val currentTime = Random.nextLong()
        val identityId = "test-identity-count".encodeToByteArray()
        val driveId = "test-drive-count".encodeToByteArray()

        for (i in 1..3) {
            db.driveMainIndexQueries.upsertDriveMainIndex(
                identityId = identityId,
                driveId = driveId,
                fileId = "file-$i".encodeToByteArray(),
                globalTransitId = "transit-$i".encodeToByteArray(),
                fileState = i.toLong(),
                requiredSecurityGroup = 1L,
                fileSystemType = 1L,
                userDate = currentTime,
                fileType = 1L,
                dataType = 1L,
                archivalStatus = 1L,
                historyStatus = 1L,
                senderId = "sender-$i",
                groupId = null,
                uniqueId = "unique-$i".encodeToByteArray(),
                byteCount = i * 100L,
                hdrEncryptedKeyHeader = "key-$i",
                hdrVersionTag = "version-$i".encodeToByteArray(),
                hdrAppData = "app-data-$i",
                hdrLocalVersionTag = null,
                hdrLocalAppData = null,
                hdrReactionSummary = null,
                hdrServerData = "server-data-$i",
                hdrTransferHistory = null,
                hdrFileMetaData = "metadata-$i",
                created = currentTime + i,
                modified = currentTime + i + 1000
            )
        }

        // Verify count
        assertEquals(3L, db.driveMainIndexQueries.countAll().executeAsOne())

        // Verify select all
        val allRecords = db.driveMainIndexQueries.selectAll().executeAsList()
        assertEquals(3, allRecords.size, "Should have exactly 3 records")

        // Verify the records have different fileIds
        val fileIds = allRecords.map { it.fileId.decodeToString() }
        assertTrue(fileIds.contains("file-1"))
        assertTrue(fileIds.contains("file-2"))
        assertTrue(fileIds.contains("file-3"))
    }

    @Test
    fun testDeleteAll() = runTest {

        // Insert a test record first
        val currentTime = Random.nextLong()
        db.driveMainIndexQueries.upsertDriveMainIndex(
            identityId = "test-identity-delete".encodeToByteArray(),
            driveId = "test-drive-delete".encodeToByteArray(),
            fileId = "file-to-delete".encodeToByteArray(),
            globalTransitId = "transit-delete".encodeToByteArray(),
            fileState = 1L,
            requiredSecurityGroup = 1L,
            fileSystemType = 1L,
            userDate = currentTime,
            fileType = 1L,
            dataType = 1L,
            archivalStatus = 1L,
            historyStatus = 1L,
            senderId = "sender-delete",
            groupId = null,
            uniqueId = "unique-delete".encodeToByteArray(),
            byteCount = 100L,
            hdrEncryptedKeyHeader = "key-delete",
            hdrVersionTag = "version-delete".encodeToByteArray(),
            hdrAppData = "app-data-delete",
            hdrLocalVersionTag = null,
            hdrLocalAppData = null,
            hdrReactionSummary = null,
            hdrServerData = "server-data-delete",
            hdrTransferHistory = null,
            hdrFileMetaData = "metadata-delete",
            created = currentTime,
            modified = currentTime
        )

        // Verify we have 1 record
        assertEquals(1L, db.driveMainIndexQueries.countAll().executeAsOne())

        // Delete all records
        db.driveMainIndexQueries.deleteAll()

        // Verify all records are deleted
        assertEquals(0L, db.driveMainIndexQueries.countAll().executeAsOne())
        
        val allRecords = db.driveMainIndexQueries.selectAll().executeAsList()
        assertTrue(allRecords.isEmpty(), "Should have no records after deleteAll")
    }
}