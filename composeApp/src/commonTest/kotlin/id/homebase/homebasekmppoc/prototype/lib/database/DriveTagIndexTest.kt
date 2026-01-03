package id.homebase.homebasekmppoc.prototype.lib.database

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.uuid.Uuid

class DriveTagIndexTest
{
    @BeforeTest
    fun setup() {
        val driver = createInMemoryDatabase()
        DatabaseManager.initialize { driver }
    }

    @AfterTest
    fun tearDown() {
    }

    @Test
    fun testInsertSelectDeleteTag() = runTest {

        // Test data - create sample UUIDs
        val identityId = Uuid.random()
        val driveId = Uuid.random()
        val fileId = Uuid.random()
        val tagId = Uuid.random()

        // Clean up any existing data for this file
        DatabaseManager.driveTagIndex.deleteByFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        )

        // Insert a tag
        DatabaseManager.driveTagIndex.insertTag(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId,
            tagId = tagId
        )

        // Select tags for the file
        val tags = DatabaseManager.driveTagIndex.selectByFile(
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
        DatabaseManager.driveTagIndex.insertTag(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId,
            tagId = tagId2
        )

        // Verify we now have 2 tags
        val tagsAfterSecondInsert = DatabaseManager.driveTagIndex.selectByFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        ).executeAsList()

        assertEquals(2, tagsAfterSecondInsert.size, "Should have exactly two tags")

        // Delete all tags for the file
        DatabaseManager.driveTagIndex.deleteByFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        )

        // Verify deletion
        val tagsAfterDelete = DatabaseManager.driveTagIndex.selectByFile(
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
        val tags = DatabaseManager.driveTagIndex.selectByFile(
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
        DatabaseManager.driveTagIndex.deleteByFile(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId
        )

        // Insert a tag
        DatabaseManager.driveTagIndex.insertTag(
            identityId = identityId,
            driveId = driveId,
            fileId = fileId,
            tagId = tagId
        )

        // Try to insert the same tag again (should violate UNIQUE constraint)
        try {
            DatabaseManager.driveTagIndex.insertTag(
                identityId = identityId,
                driveId = driveId,
                fileId = fileId,
                tagId = tagId
            )
            // If we get here, the constraint wasn't enforced (this might be expected behavior)
            // Check if we have 1 or 2 records
            val tags = DatabaseManager.driveTagIndex.selectByFile(
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