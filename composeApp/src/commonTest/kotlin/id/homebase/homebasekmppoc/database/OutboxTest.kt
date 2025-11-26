package id.homebase.homebasekmppoc.database

import app.cash.sqldelight.db.SqlDriver
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

class OutboxTest {
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
    fun testInsertSelectDeleteOutboxItem() = runTest {
        // Test data
        val lastAttempt = 1704067200000L // Unix timestamp
        val attemptCount = 1L
        val data = "test data".toByteArray()
        val files = "test files".toByteArray()

        // Insert into outbox
        db.outboxQueries.insert(
            lastAttempt = lastAttempt,
            attemptCount = attemptCount,
            data_ = data,
            files = files
        )

        // Get count to verify insertion
        val countAfterInsert = db.outboxQueries.count().executeAsOne()
        assertEquals(1L, countAfterInsert, "Should have exactly one item in outbox")

        // Select next item (should be the one we just inserted)
        val nextItem = db.outboxQueries.selectNext().executeAsOne()
        
        // Verify the retrieved item
        assertNotNull(nextItem, "Should retrieve the inserted item")
        assertEquals(lastAttempt, nextItem.lastAttempt, "Last attempt should match")
        assertEquals(attemptCount, nextItem.attemptCount, "Attempt count should match")
        assertEquals(data.contentToString(), nextItem.data_.contentToString(), "Data should match")
        assertEquals(files.contentToString(), nextItem.files.contentToString(), "Files should match")

        // Delete the item
        db.outboxQueries.delete(nextItem.rowId)

        // Verify deletion
        val countAfterDelete = db.outboxQueries.count().executeAsOne()
        assertEquals(0L, countAfterDelete, "Should have no items after deletion")
        
        val nextItemAfterDelete = db.outboxQueries.selectNext().executeAsOneOrNull()
        assertNull(nextItemAfterDelete, "Should return null when outbox is empty")
    }

    @Test
    fun testSelectNextWithEmptyOutbox() = runTest {
        // Try to select next item from empty outbox
        val nextItem = db.outboxQueries.selectNext().executeAsOneOrNull()
        
        // Verify no item found
        assertNull(nextItem, "Should return null when outbox is empty")
        
        // Verify count is zero
        val count = db.outboxQueries.count().executeAsOne()
        assertEquals(0L, count, "Count should be zero for empty outbox")
    }

    @Test
    fun testMultipleItemsSequentialOrdering() = runTest {
        // Test data for multiple items
        val items = listOf(
            Triple(1704067200000L, 1L, "first".toByteArray()),
            Triple(1704153600000L, 2L, "second".toByteArray()),
            Triple(1704240000000L, 1L, "third".toByteArray())
        )

        // Insert multiple items
        items.forEach { (lastAttempt, attemptCount, data) ->
            db.outboxQueries.insert(
                lastAttempt = lastAttempt,
                attemptCount = attemptCount,
                data_ = data,
                files = null
            )
        }

        // Verify count
        val count = db.outboxQueries.count().executeAsOne()
        assertEquals(3L, count, "Should have three items in outbox")

        // Verify sequential ordering (should be in order of insertion/rowId)
        val firstItem = db.outboxQueries.selectNext().executeAsOne()
        assertEquals("first".toByteArray().contentToString(), firstItem.data_.contentToString(),
            "First item should be the first inserted")

        // Delete first item and verify next is second
        db.outboxQueries.delete(firstItem.rowId)
        val secondItem = db.outboxQueries.selectNext().executeAsOne()
        assertEquals("second".toByteArray().contentToString(), secondItem.data_.contentToString(),
            "Second item should be next after deleting first")

        // Delete second item and verify next is third
        db.outboxQueries.delete(secondItem.rowId)
        val thirdItem = db.outboxQueries.selectNext().executeAsOne()
        assertEquals("third".toByteArray().contentToString(), thirdItem.data_.contentToString(),
            "Third item should be next after deleting second")

        // Delete third item and verify outbox is empty
        db.outboxQueries.delete(thirdItem.rowId)
        val noItem = db.outboxQueries.selectNext().executeAsOneOrNull()
        assertNull(noItem, "Should return null when all items are deleted")
    }

    @Test
    fun testOutboxItemWithNullFiles() = runTest {
        // Test data with null files
        val lastAttempt = 1704067200000L
        val attemptCount = 1L
        val data = "test data".toByteArray()

        // Insert item with null files
        db.outboxQueries.insert(
            lastAttempt = lastAttempt,
            attemptCount = attemptCount,
            data_ = data,
            files = null
        )

        // Retrieve and verify
        val item = db.outboxQueries.selectNext().executeAsOne()
        assertNotNull(item, "Should retrieve the inserted item")
        assertEquals(lastAttempt, item.lastAttempt, "Last attempt should match")
        assertEquals(attemptCount, item.attemptCount, "Attempt count should match")
        assertEquals(data.contentToString(), item.data_.contentToString(), "Data should match")
        assertNull(item.files, "Files should be null")

        // Clean up
        db.outboxQueries.delete(item.rowId)
    }

    @Test
    fun testUpdateAttemptCount() = runTest {
        // Insert initial item
        val initialLastAttempt = 1704067200000L
        val initialAttemptCount = 1L
        val data = "test data".toByteArray()
        
        db.outboxQueries.insert(
            lastAttempt = initialLastAttempt,
            attemptCount = initialAttemptCount,
            data_ = data,
            files = null
        )

        // Retrieve the item
        val item = db.outboxQueries.selectNext().executeAsOne()
        assertEquals(initialAttemptCount, item.attemptCount, "Initial attempt count should be 1")

        // Simulate an update by inserting a new record with updated attempt count
        // (Note: Outbox.sq doesn't have an update operation, so we'd typically delete and reinsert)
        val updatedLastAttempt = 1704153600000L
        val updatedAttemptCount = 2L
        
        db.outboxQueries.delete(item.rowId)
        db.outboxQueries.insert(
            lastAttempt = updatedLastAttempt,
            attemptCount = updatedAttemptCount,
            data_ = data,
            files = null
        )

        // Verify the update
        val updatedItem = db.outboxQueries.selectNext().executeAsOne()
        assertEquals(updatedAttemptCount, updatedItem.attemptCount, "Attempt count should be updated")
        assertEquals(updatedLastAttempt, updatedItem.lastAttempt, "Last attempt should be updated")
        assertEquals(data.contentToString(), updatedItem.data_.contentToString(), "Data should remain the same")
    }
}