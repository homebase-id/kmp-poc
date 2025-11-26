package id.homebase.homebasekmppoc.database

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.time.Clock
import kotlin.uuid.Uuid

class CommitExperimentTest {
    private var driver: SqlDriver? = null
    private lateinit var db: OdinDatabase

    @BeforeTest
    fun setup() {
        driver = createInMemoryDatabase()
        
        // Create adapters for UUID columns
        val driveTagIndexAdapter = DriveTagIndex.Adapter(
            identityIdAdapter = UuidAdapter,
            driveIdAdapter = UuidAdapter,
            fileIdAdapter = UuidAdapter,
            tagIdAdapter = UuidAdapter
        )
        
        val driveLocalTagIndexAdapter = DriveLocalTagIndex.Adapter(
            identityIdAdapter = UuidAdapter,
            driveIdAdapter = UuidAdapter,
            fileIdAdapter = UuidAdapter,
            tagIdAdapter = UuidAdapter
        )
        
        val driveMainIndexAdapter = DriveMainIndex.Adapter(
            identityIdAdapter = UuidAdapter,
            driveIdAdapter = UuidAdapter,
            fileIdAdapter = UuidAdapter,
            globalTransitIdAdapter = UuidAdapter,
            groupIdAdapter = UuidAdapter,
            uniqueIdAdapter = UuidAdapter
        )
        
        val keyValueAdapter = KeyValue.Adapter(
            keyAdapter = UuidAdapter
        )
        
        db = OdinDatabase(driver!!, driveLocalTagIndexAdapter, driveMainIndexAdapter, driveTagIndexAdapter, keyValueAdapter)
    }

    @AfterTest
    fun tearDown() {
        driver?.close()
    }


@Test
    fun testInsertSelectDeleteTag() = runTest {
        // Test data - create sample UUIDs
        val identityId = Uuid.random()
        val driveId = Uuid.random()
        val fileId = Uuid.random()
        val tagId = Uuid.random()
        val currentTime = Random.nextLong()

    // Start a transaction here

        // Insert a tag1
        db.driveTagIndexQueries.insertTag(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId,
            tagId = tagId
        )

        // Insert a tag2 with different tagId
        val tagId2 = Uuid.random()
        db.driveTagIndexQueries.insertTag(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId,
            tagId = tagId2
        )

// Insert a driveMainIndex record
        db.driveMainIndexQueries.upsertDriveMainIndex(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId,
            globalTransitId = Uuid.random(),
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
            uniqueId = Uuid.random(),
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

        // Commit transaction here
    }
}
