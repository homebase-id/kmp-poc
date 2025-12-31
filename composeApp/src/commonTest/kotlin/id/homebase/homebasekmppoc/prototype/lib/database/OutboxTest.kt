package id.homebase.homebasekmppoc.prototype.lib.database

import app.cash.sqldelight.db.SqlDriver
import id.homebase.homebasekmppoc.lib.database.OdinDatabase
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.uuid.Uuid

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
        val checkOutCount = 1L
        val data = "test data".toByteArray()
        val files = "test files".toByteArray()
        val checkOutStamp = 1L

        // Insert into outbox
        db.outboxQueries.insert(
            driveId = Uuid.random(),
            fileId = Uuid.random(),
            dependencyFileId = Uuid.random(),
            lastAttempt = lastAttempt,
            nextRunTime = lastAttempt,
            checkOutCount = checkOutCount,
            checkOutStamp = null,
            priority = 0L,
            data_ = data,
            files = files
        )

        // Get count to verify insertion
        val countAfterInsert = db.outboxQueries.count().executeAsOne()
        assertEquals(1L, countAfterInsert, "Should have exactly one item in outbox")

        // Checkout the item
        db.outboxQueries.checkout(checkOutStamp = checkOutStamp, now = lastAttempt + 1)

        // Select the checked out item
        val nextItem = db.outboxQueries.selectCheckedOut(checkOutStamp).executeAsOne()

        // Verify the retrieved item
        assertNotNull(nextItem, "Should retrieve the inserted item")
        assertEquals(lastAttempt, nextItem.lastAttempt, "Last attempt should match")
        assertEquals(checkOutCount, nextItem.checkOutCount, "Check out count should match")
        assertEquals(data.contentToString(), nextItem.data_.contentToString(), "Data should match")
        assertEquals(files.contentToString(), nextItem.files.contentToString(), "Files should match")

        // Delete the item
        db.outboxQueries.deleteByRowid(nextItem.rowId)

        // Verify deletion
        val countAfterDelete = db.outboxQueries.count().executeAsOne()
        assertEquals(0L, countAfterDelete, "Should have no items after deletion")

        // Try to checkout from empty outbox
        val updatedRows = db.outboxQueries.checkout(checkOutStamp = 2L, now = lastAttempt + 1).value
        assertEquals(0L, updatedRows, "Should update 0 rows from empty outbox")
    }

    @Test
    fun testCheckoutWithEmptyOutbox() = runTest {
        // Try to checkout from empty outbox
        val updatedRows = db.outboxQueries.checkout(checkOutStamp = 3L, now = 1704067200001L).value

        // Verify no rows updated
        assertEquals(0L, updatedRows, "Should update 0 rows from empty outbox")

        // Verify count is zero
        val count = db.outboxQueries.count().executeAsOne()
        assertEquals(0L, count, "Count should be zero for empty outbox")
    }

    @Test
    fun testMultipleItemsSequentialOrdering() = runTest {
        // Test data
        val lastAttempt = 1704067200000L
        val checkOutCount = 1L
        val data = "test data".toByteArray()

        // Insert item
        db.outboxQueries.insert(
            driveId = Uuid.random(),
            fileId = Uuid.random(),
            dependencyFileId = Uuid.random(),
            lastAttempt = lastAttempt,
            nextRunTime = lastAttempt,
            checkOutCount = checkOutCount,
            checkOutStamp = null,
            priority = 0L,
            data_ = data,
            files = null
        )

        // Verify count
        val count = db.outboxQueries.count().executeAsOne()
        assertEquals(1L, count, "Should have one item in outbox")

        // Checkout and select item
        db.outboxQueries.checkout(checkOutStamp = 4L, now = lastAttempt + 1)
        val item = db.outboxQueries.selectCheckedOut(4L).executeAsOne()
        assertEquals(data.contentToString(), item.data_.contentToString(),
            "Item should be the inserted one")

        // Delete item and verify outbox is empty
        db.outboxQueries.deleteByRowid(item.rowId)
        val updatedRows = db.outboxQueries.checkout(checkOutStamp = 5L, now = lastAttempt + 1).value
        assertEquals(0L, updatedRows, "Should update 0 rows when item is deleted")
    }

    @Test
    fun testOutboxItemWithNullFiles() = runTest {
        // Test data with null files
        val lastAttempt = 1704067200000L
        val checkOutCount = 1L
        val data = "test data".toByteArray()

        // Insert item with null files
        db.outboxQueries.insert(
            driveId = Uuid.random(),
            fileId = Uuid.random(),
            dependencyFileId = Uuid.random(),
            lastAttempt = lastAttempt,
            nextRunTime = lastAttempt,
            checkOutCount = checkOutCount,
            checkOutStamp = null,
            priority = 0L,
            data_ = data,
            files = null
        )

        // Checkout and select
        db.outboxQueries.checkout(checkOutStamp = 8L, now = lastAttempt + 1)
        val item = db.outboxQueries.selectCheckedOut(8L).executeAsOne()
        assertNotNull(item, "Should retrieve the inserted item")
        assertEquals(lastAttempt, item.lastAttempt, "Last attempt should match")
        assertEquals(checkOutCount, item.checkOutCount, "Check out count should match")
        assertEquals(data.contentToString(), item.data_.contentToString(), "Data should match")
        assertNull(item.files, "Files should be null")

        // Clean up
        db.outboxQueries.deleteByRowid(item.rowId)
    }

    @Test
    fun testUpdateCheckOutCount() = runTest {
        // Insert initial item
        val initialLastAttempt = 1704067200000L
        val initialCheckOutCount = 1L
        val data = "test data".toByteArray()

        db.outboxQueries.insert(
            driveId = Uuid.random(),
            fileId = Uuid.random(),
            dependencyFileId = Uuid.random(),
            lastAttempt = initialLastAttempt,
            nextRunTime = initialLastAttempt,
            checkOutCount = initialCheckOutCount,
            checkOutStamp = null,
            priority = 0L,
            data_ = data,
            files = null
        )

        // Checkout and select the item
        db.outboxQueries.checkout(checkOutStamp = 9L, now = initialLastAttempt + 1)
        val item = db.outboxQueries.selectCheckedOut(9L).executeAsOne()
        assertEquals(initialCheckOutCount, item.checkOutCount, "Initial check out count should be 1")

        // Simulate an update by inserting a new record with updated check out count
        // (Note: Outbox.sq doesn't have an update operation, so we'd typically delete and reinsert)
        val updatedLastAttempt = 1704153600000L
        val updatedCheckOutCount = 2L

        db.outboxQueries.deleteByRowid(item.rowId)
        db.outboxQueries.insert(
            driveId = Uuid.random(),
            fileId = Uuid.random(),
            dependencyFileId = Uuid.random(),
            lastAttempt = updatedLastAttempt,
            nextRunTime = updatedLastAttempt,
            checkOutCount = updatedCheckOutCount,
            checkOutStamp = null,
            priority = 0L,
            data_ = data,
            files = null
        )

        // Checkout and verify the update
        db.outboxQueries.checkout(checkOutStamp = 10L, now = updatedLastAttempt + 1)
        val updatedItem = db.outboxQueries.selectCheckedOut(10L).executeAsOne()
        assertEquals(updatedCheckOutCount, updatedItem.checkOutCount, "Check out count should be updated")
        assertEquals(updatedLastAttempt, updatedItem.lastAttempt, "Last attempt should be updated")
        assertEquals(data.contentToString(), updatedItem.data_.contentToString(), "Data should remain the same")
    }
}
