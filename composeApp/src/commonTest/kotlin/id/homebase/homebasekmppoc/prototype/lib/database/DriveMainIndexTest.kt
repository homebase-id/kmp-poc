package id.homebase.homebasekmppoc.prototype.lib.database

import app.cash.sqldelight.db.SqlDriver
import id.homebase.homebasekmppoc.lib.database.OdinDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.uuid.Uuid

class DriveMainIndexTest {

    private var driver: SqlDriver? = null
    private lateinit var db: OdinDatabase

@BeforeTest
    fun setup() {
        driver = createInMemoryDatabase()
        db = TestDatabaseFactory.createTestDatabase(driver)
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
        val identityId = Uuid.random()
        val driveId = Uuid.random()
        val fileId = Uuid.random()
        val globalTransitId = Uuid.random()
        val uniqueId = Uuid.random()
        val groupId = Uuid.random()
        val hdrVersionTag = "hdr-version-$randomId".encodeToByteArray()
        val hdrLocalVersionTag = "hdr-local-version-$randomId".encodeToByteArray()

        // Test values
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
            uniqueId = uniqueId,
            globalTransitId = globalTransitId,
            groupId = groupId,
            senderId = senderId,
            fileType = fileType.toLong(),
            dataType = dataType.toLong(),
            archivalStatus = archivalStatus.toLong(),
            historyStatus = historyStatus.toLong(),
            userDate = userDate,
            created = created,
            modified = modified,
            fileSystemType = fileSystemType.toLong(),
            jsonHeader = """{"versionTag":"$hdrVersionTag","byteCount":$byteCount,"encryptedKeyHeader":"$hdrEncryptedKeyHeader","appData":"$hdrAppData","localVersionTag":"$hdrLocalVersionTag","localAppData":"$hdrLocalAppData","reactionSummary":"$hdrReactionSummary","serverData":"$hdrServerData","transferHistory":"$hdrTransferHistory","fileMetaData":"$hdrFileMetaData"}"""
        )

        // Select the record by identity, drive, and file
        val selectedRecord = db.driveMainIndexQueries.selectByIdentityAndDriveAndFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        ).executeAsOne()

// Verify all fields match
        assertEquals(identityId, selectedRecord.identityId)
        assertEquals(driveId, selectedRecord.driveId)
assertEquals(fileId, selectedRecord.fileId)
        assertEquals(globalTransitId, selectedRecord.globalTransitId)
        assertEquals(userDate, selectedRecord.userDate)
        assertEquals(fileType.toLong(), selectedRecord.fileType)
        assertEquals(dataType.toLong(), selectedRecord.dataType)
        assertEquals(archivalStatus.toLong(), selectedRecord.archivalStatus)
        assertEquals(historyStatus.toLong(), selectedRecord.historyStatus)
        // Note: fileState, requiredSecurityGroup, fileSystemType are now consolidated in jsonHeader
assertEquals(senderId, selectedRecord.senderId)
assertEquals(groupId, selectedRecord.groupId)
assertEquals(uniqueId, selectedRecord.uniqueId)
assertEquals(created, selectedRecord.created)
assertEquals(modified, selectedRecord.modified)
// Note: old header fields are now consolidated in jsonHeader
    }

    @Test
    fun testUpsertUpdateExistingRecord() = runTest {

        // Test data
        val currentTime = Random.nextLong()
val identityId = Uuid.random()
        val driveId = Uuid.random()
        val fileId = Uuid.random()
        val globalTransitId = Uuid.random()
        val uniqueId = Uuid.random()
        val hdrVersionTag = "hdr-version-original".encodeToByteArray()

// Insert initial record
        db.driveMainIndexQueries.upsertDriveMainIndex(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId,
            uniqueId = uniqueId,
            globalTransitId = globalTransitId,
            groupId = null,
            senderId = "original-sender",
            fileType = 4L,
            dataType = 5L,
            archivalStatus = 6L,
            historyStatus = 7L,
            userDate = currentTime,
            created = currentTime,
            modified = currentTime,
            fileSystemType = 3L,
            jsonHeader = """{"versionTag":"${hdrVersionTag.contentToString()}","byteCount":1024,"encryptedKeyHeader":"original-key-header","appData":"original-app-data","localVersionTag":null,"localAppData":null,"reactionSummary":null,"serverData":"original-server-data","transferHistory":null,"fileMetaData":"original-metadata"}"""
        )

// Update the record with new values
        val updatedTime = currentTime + 5000
        val updatedGlobalTransitId = Uuid.random()
        val updatedUniqueId = Uuid.random()

db.driveMainIndexQueries.upsertDriveMainIndex(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId,
            uniqueId = updatedUniqueId,
            globalTransitId = updatedGlobalTransitId,
            groupId = Uuid.random(),
            senderId = "updated-sender",
            fileType = 44L,
            dataType = 55L,
            archivalStatus = 66L,
            historyStatus = 77L,
            userDate = updatedTime,
            created = currentTime,
            modified = updatedTime,
            fileSystemType = 33L,
            jsonHeader = """{"versionTag":"${"hdr-version-updated".encodeToByteArray().contentToString()}","byteCount":2048,"encryptedKeyHeader":"updated-key-header","appData":"updated-app-data","localVersionTag":"${"hdr-local-version-updated".encodeToByteArray().contentToString()}","localAppData":"updated-local-app-data","reactionSummary":"updated-reaction-summary","serverData":"updated-server-data","transferHistory":"updated-transfer-history","fileMetaData":"updated-metadata"}"""
        )

        // Select and verify the new record (simulating update by inserting different record)
        val updatedRecord = db.driveMainIndexQueries.selectByIdentityAndDriveAndFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        ).executeAsOne()

assertEquals(updatedGlobalTransitId, updatedRecord.globalTransitId)
        assertEquals("updated-sender", updatedRecord.senderId)
        assertEquals(updatedUniqueId, updatedRecord.uniqueId)
        assertEquals(updatedTime, updatedRecord.modified)
        assertEquals(currentTime, updatedRecord.created)
        // Note: old header fields are now consolidated in jsonHeader
    }

    @Test
    fun testSelectByIdentityAndDriveAndFileWithNonExistentRecord() = runTest {

        // Test data for non-existent record
val identityId = Uuid.random()
        val driveId = Uuid.random()
        val fileId = Uuid.random()

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
val identityId = Uuid.random()
        val driveId = Uuid.random()

for (i in 1..3) {
            db.driveMainIndexQueries.upsertDriveMainIndex(
                identityId = identityId,
                driveId = driveId,
                fileId = Uuid.random(),
                uniqueId = Uuid.random(),
                globalTransitId = Uuid.random(),
                groupId = null,
                senderId = "sender-$i",
                fileType = 1L,
                dataType = 1L,
                archivalStatus = 1L,
                historyStatus = 1L,
                userDate = currentTime,
                created = currentTime + i,
                modified = currentTime + i + 1000,
                fileSystemType = 1L,
                jsonHeader = """{"versionTag":"${"version-$i".encodeToByteArray().contentToString()}","byteCount":${i * 100L},"encryptedKeyHeader":"key-$i","appData":"app-data-$i","localVersionTag":null,"localAppData":null,"reactionSummary":null,"serverData":"server-data-$i","transferHistory":null,"fileMetaData":"metadata-$i"}"""
            )
        }

        // Verify count
        assertEquals(3L, db.driveMainIndexQueries.countAll().executeAsOne())

        // Verify select all
        val allRecords = db.driveMainIndexQueries.selectAll().executeAsList()
        assertEquals(3, allRecords.size, "Should have exactly 3 records")

// Verify the records have different fileIds
        val fileIds = allRecords.map { it.fileId.toString() }
        assertEquals(3, fileIds.toSet().size, "All fileIds should be different")
    }

    @Test
    fun testDeleteAll() = runTest {

// Insert a test record first
        val currentTime = Random.nextLong()
        db.driveMainIndexQueries.upsertDriveMainIndex(
            identityId = Uuid.random(),
            driveId = Uuid.random(),
            fileId = Uuid.random(),
            uniqueId = Uuid.random(),
            globalTransitId = Uuid.random(),
            groupId = null,
            senderId = "sender-delete",
            fileType = 1L,
            dataType = 1L,
            archivalStatus = 1L,
            historyStatus = 1L,
            userDate = currentTime,
            created = currentTime,
            modified = currentTime,
            fileSystemType = 1L,
            jsonHeader = """{"versionTag":"${"version-delete".encodeToByteArray().contentToString()}","byteCount":100,"encryptedKeyHeader":"key-delete","appData":"app-data-delete","localVersionTag":null,"localAppData":null,"reactionSummary":null,"serverData":"server-data-delete","transferHistory":null,"fileMetaData":"metadata-delete"}"""
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