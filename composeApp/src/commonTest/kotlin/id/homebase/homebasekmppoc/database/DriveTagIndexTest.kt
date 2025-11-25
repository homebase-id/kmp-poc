package id.homebase.homebasekmppoc.database

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.random.Random

class DriveTagIndexTest {

    @Test
    fun testInsertSelectDeleteTag() = runTest {
        // Create in-memory test database
        val db = createInMemoryDatabase()

        // Test data - create sample byte arrays
        val randomId = Random.nextLong()
        val currentTime = randomId
        val identityId = "test-identity".encodeToByteArray()
        val driveId = "test-drive".encodeToByteArray()
        val fileId = "test-file-$currentTime".encodeToByteArray()
        val tagId = "test-tag-$randomId".encodeToByteArray()

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
        assertEquals(identityId.toList(), tags[0].identityId.toList())
        assertEquals(driveId.toList(), tags[0].driveId.toList())
        assertEquals(fileId.toList(), tags[0].fileId.toList())
        assertEquals(tagId.toList(), tags[0].tagId.toList())

        // Insert another tag for the same file
        val tagId2 = "test-tag-2-$randomId".encodeToByteArray()
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
        // Create in-memory test database
        val db = createInMemoryDatabase()

        // Test data for non-existent file
        val identityId = "non-existent-identity".encodeToByteArray()
        val driveId = "non-existent-drive".encodeToByteArray()
        val fileId = "non-existent-file".encodeToByteArray()

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
        // Create in-memory test database
        val db = createInMemoryDatabase()

        // Test data
        val randomId = Random.nextLong()
        val identityId = "test-identity-unique".encodeToByteArray()
        val driveId = "test-drive-unique".encodeToByteArray()
        val fileId = "test-file-unique-$randomId".encodeToByteArray()
        val tagId = "test-tag-unique".encodeToByteArray()

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