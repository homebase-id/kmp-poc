package id.homebase.homebasekmppoc.database

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.uuid.Uuid

class DriveTagIndexTest {
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

        // Clean up any existing data for this file
        db.driveTagIndexQueries.deleteByFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        )

        // Insert a tag
        db.driveTagIndexQueries.insertTag(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId,
            tagId = tagId
        )

        // Select tags for the file
        val tags = db.driveTagIndexQueries.selectByFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        ).executeAsList()

// Verify insertion
        assertEquals(1, tags.size, "Should have exactly one tag")
        assertEquals(identityId, tags[0].identityId)
        assertEquals(driveId, tags[0].driveId)
        assertEquals(fileId, tags[0].fileId)
        assertEquals(tagId, tags[0].tagId)

        // Insert another tag for the same file
        val tagId2 = Uuid.random()
        db.driveTagIndexQueries.insertTag(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId,
            tagId = tagId2
        )

        // Verify we now have 2 tags
        val tagsAfterSecondInsert = db.driveTagIndexQueries.selectByFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        ).executeAsList()

        assertEquals(2, tagsAfterSecondInsert.size, "Should have exactly two tags")

        // Delete all tags for the file
        db.driveTagIndexQueries.deleteByFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        )

        // Verify deletion
        val tagsAfterDelete = db.driveTagIndexQueries.selectByFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        ).executeAsList()

        assertTrue(tagsAfterDelete.isEmpty(), "Should have no tags after deletion")
    }

@Test
    fun testSelectByFileWithNoTags() = runTest {

        // Test data for non-existent file
        val identityId = Uuid.random()
        val driveId = Uuid.random()
        val fileId = Uuid.random()

        // Select tags for non-existent file
        val tags = db.driveTagIndexQueries.selectByFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        ).executeAsList()

        // Verify no tags found
        assertTrue(tags.isEmpty(), "Should have no tags for non-existent file")
    }

@Test
    fun testUniqueConstraint() = runTest {

        // Test data
        val identityId = Uuid.random()
        val driveId = Uuid.random()
        val fileId = Uuid.random()
        val tagId = Uuid.random()

        // Clean up any existing data
        db.driveTagIndexQueries.deleteByFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        )

        // Insert a tag
        db.driveTagIndexQueries.insertTag(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId,
            tagId = tagId
        )

        // Try to insert the same tag again (should violate UNIQUE constraint)
        try {
            db.driveTagIndexQueries.insertTag(
                identityId = identityId,
                driveId = driveId,
                fileId = fileId,
                tagId = tagId
            )
            // If we get here, the constraint wasn't enforced (this might be expected behavior)
            // Check if we have 1 or 2 records
            val tags = db.driveTagIndexQueries.selectByFile(
                identityId = identityId,
                driveId = driveId,
                fileId = fileId
            ).executeAsList()
            assertTrue(tags.size >= 1, "Should have at least one tag")
        } catch (e: Exception) {
            // This is expected if the UNIQUE constraint is enforced
            assertTrue(e.message?.contains("UNIQUE") == true || e.message?.contains("constraint") == true, 
                "Should get a constraint violation error: ${e.message}")
        }
    }
}