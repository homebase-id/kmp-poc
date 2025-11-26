package id.homebase.homebasekmppoc.database

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

class CommitExperimentTest {
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
    fun testInsertSelectDeleteTag() = runTest {
        // Test data - create sample byte arrays
        val randomId = Random.nextLong()
        val currentTime = randomId
        val identityId = "test-identity".encodeToByteArray()
        val driveId = "test-drive".encodeToByteArray()
        val fileId = "test-file-$currentTime".encodeToByteArray()
        val tagId = "test-tag-$randomId".encodeToByteArray()

        // Start a transaction here

        // Insert a tag1
        db.driveTagIndexQueries.insertTag(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId,
            tagId = tagId
        )

        // Insert a tag2 with different tagId
        val tagId2 = "test-tag-$randomId-2".encodeToByteArray()
        db.driveTagIndexQueries.insertTag(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId,
            tagId = tagId2
        )

        // Insert a driveMainIndex record
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

        // Commit transaction here
    }
}
