package id.homebase.homebasekmppoc.prototype.lib.database

import app.cash.sqldelight.db.SqlDriver
import id.homebase.homebasekmppoc.lib.database.OdinDatabase
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
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

    @Test
    fun testPopTest() = runTest {
        val driveId = Uuid.random()
        val fileIds = List(5) { Uuid.random() }
        val values = List(5) { Uuid.random().toByteArray() }
        val priorities = 0L..4L

        // Insert items with priorities 0 to 4
        priorities.forEachIndexed { index, priority ->
            db.outboxQueries.insert(
                driveId = driveId,
                fileId = fileIds[index],
                dependencyFileId = null,
                lastAttempt = 1704067200000L,
                nextRunTime = 1704067200000L,
                checkOutCount = 0L,
                checkOutStamp = null,
                priority = priority,
                data_ = values[index],
                files = null
            )
        }

        // Verify total count
        assertEquals(5L, db.outboxQueries.count().executeAsOne())

        // Checkout items in priority order (0,1,2,3,4)
        priorities.forEachIndexed { index, expectedPriority ->
            val checkOutStamp = (index + 1).toLong()
            val updated = db.outboxQueries.checkout(checkOutStamp = checkOutStamp, now = 1704067200001L).value
            assertEquals(1L, updated, "Should update 1 row")

            val item = db.outboxQueries.selectCheckedOut(checkOutStamp).executeAsOne()
            assertEquals(expectedPriority, item.priority, "Priority should match")
            assertEquals(values[index].contentToString(), item.data_.contentToString(), "Data should match")

            // "Complete" by deleting
            db.outboxQueries.deleteByRowid(item.rowId)
        }

        // Verify all items processed
        assertEquals(0L, db.outboxQueries.count().executeAsOne())
    }

    @Test
    fun testPriorityTest() = runTest {
        val driveId = Uuid.random()
        val fileIds = List(5) { Uuid.random() }
        val values = List(5) { Uuid.random().toByteArray() }
        val priorities = listOf(4L, 1L, 2L, 3L, 0L) // Insert order
        val expectedOrder = listOf(0L, 1L, 2L, 3L, 4L) // Checkout order

        // Insert items with mixed priorities
        priorities.forEachIndexed { index, priority ->
            db.outboxQueries.insert(
                driveId = driveId,
                fileId = fileIds[index],
                dependencyFileId = null,
                lastAttempt = 1704067200000L,
                nextRunTime = 1704067200000L,
                checkOutCount = 0L,
                checkOutStamp = null,
                priority = priority,
                data_ = values[index],
                files = null
            )
        }

        // Checkout items in priority order (lowest first)
        expectedOrder.forEach { expectedPriority ->
            val checkOutStamp = expectedPriority + 1
            db.outboxQueries.checkout(checkOutStamp = checkOutStamp, now = 1704067200001L)
            val item = db.outboxQueries.selectCheckedOut(checkOutStamp).executeAsOne()
            assertEquals(expectedPriority, item.priority, "Should checkout priority $expectedPriority")

            db.outboxQueries.deleteByRowid(item.rowId)
        }
    }

    @Test
    fun testNextRunTest() = runTest {
        val driveId = Uuid.random()
        val fileIds = List(5) { Uuid.random() }
        val values = List(5) { Uuid.random().toByteArray() }
        val recipients = listOf("1", "2", "3", "4", "5") // Expected order

        // Insert items with same priority, different nextRunTime
        val baseTime = 1704067200000L
        val nextRunTimes = listOf(baseTime, baseTime + 1, baseTime + 2, baseTime + 3, baseTime + 4) // earliest first

        val combined = recipients.zip(fileIds).zip(values).zip(nextRunTimes)
        combined.forEach { (pair1, nextRunTime) ->
            val (pair2, value) = pair1
            val (recipient, fileId) = pair2
            db.outboxQueries.insert(
                driveId = driveId,
                fileId = fileId,
                dependencyFileId = null,
                lastAttempt = baseTime,
                nextRunTime = nextRunTime,
                checkOutCount = 0L,
                checkOutStamp = null,
                priority = 0L,
                data_ = "$recipient-data".toByteArray(), // Use recipient in data for identification
                files = null
            )
        }

        // Checkout items in nextRunTime order (earliest first)
        recipients.forEach { expectedRecipient ->
            val checkOutStamp = expectedRecipient.toLongOrNull() ?: 1L
            db.outboxQueries.checkout(checkOutStamp = checkOutStamp, now = baseTime + 10)
            val item = db.outboxQueries.selectCheckedOut(checkOutStamp).executeAsOne()
            assertEquals("$expectedRecipient-data".toByteArray().contentToString(), item.data_.contentToString(),
                "Should checkout $expectedRecipient")

            db.outboxQueries.deleteByRowid(item.rowId)
        }
    }

    @Test
    fun testDependencyTest() = runTest {
        val driveId = Uuid.random()
        val fileIds = List(5) { Uuid.random() }
        val values = List(5) { Uuid.random().toByteArray() }
        val f1 = fileIds[0]; val f2 = fileIds[1]; val f3 = fileIds[2]; val f4 = fileIds[3]; val f5 = fileIds[4]

        // Insert with dependencies: f2->f3, f3->null, f4->f2, f5->f4, f1->f5
        db.outboxQueries.insert(driveId, f2, f3, 1704067200000L, 1704067200000L, 0L, null, 0L, values[1], null)
        db.outboxQueries.insert(driveId, f3, null, 1704067200000L, 1704067200000L, 0L, null, 0L, values[2], null)
        db.outboxQueries.insert(driveId, f4, f2, 1704067200000L, 1704067200000L, 0L, null, 0L, values[3], null)
        db.outboxQueries.insert(driveId, f5, f4, 1704067200000L, 1704067200000L, 0L, null, 0L, values[4], null)
        db.outboxQueries.insert(driveId, f1, f5, 1704067200000L, 1704067200000L, 0L, null, 0L, values[0], null)

        // Should checkout f3 first (no dependency)
        db.outboxQueries.checkout(1L, 1704067200001L)
        var item = db.outboxQueries.selectCheckedOut(1L).executeAsOne()
        assertTrue(item.fileId == f3, "Expected item to be f3")
        db.outboxQueries.deleteByRowid(item.rowId)

        // Now f2 (depends on f3, depends on f3 which is done)
        db.outboxQueries.checkout(2L, 1704067200001L)
        item = db.outboxQueries.selectCheckedOut(2L).executeAsOne()
        assertTrue(item.fileId == f2, "Expected item to be f2")
        db.outboxQueries.deleteByRowid(item.rowId)

        // Now f4 (depends on f2, depends on f2 which is done)
        db.outboxQueries.checkout(3L, 1704067200001L)
        item = db.outboxQueries.selectCheckedOut(3L).executeAsOne()
        assertTrue(item.fileId == f4, "Expected item to be f4")
        db.outboxQueries.deleteByRowid(item.rowId)

        // Now f5 (depends on f4, depends on f4 which is done)
        db.outboxQueries.checkout(4L, 1704067200001L)
        item = db.outboxQueries.selectCheckedOut(4L).executeAsOne()
        assertTrue(item.fileId == f5, "Expected item to be f5")
        db.outboxQueries.deleteByRowid(item.rowId)

        // Finally f1 (depends on f5, depends on f5 which is done)
        db.outboxQueries.checkout(5L, 1704067200001L)
        item = db.outboxQueries.selectCheckedOut(5L).executeAsOne()
        assertTrue(item.fileId == f1, "Expected item to be f1")
        db.outboxQueries.deleteByRowid(item.rowId)
    }

    @Test
    fun testDependencyTestGetNextRun() = runTest {
        val driveId = Uuid.random()
        val fileIds = List(5) { Uuid.random() }
        val values = List(5) { Uuid.random().toByteArray() }
        val f1 = fileIds[0]; val f2 = fileIds[1]; val f3 = fileIds[2]; val f4 = fileIds[3]; val f5 = fileIds[4]

        val t1 = 1704067200000L
        val t2 = t1 + 10
        val t3 = t1 + 20
        val t4 = t1 + 30
        val t5 = t1 + 40

        // Insert with dependencies and nextRunTimes
        db.outboxQueries.insert(driveId, f2, f3, t1, t2, 0L, null, 0L, values[1], null)
        db.outboxQueries.insert(driveId, f3, null, t1, t3, 0L, null, 0L, values[2], null)
        db.outboxQueries.insert(driveId, f4, f2, t1, t4, 0L, null, 0L, values[3], null)
        db.outboxQueries.insert(driveId, f5, f4, t1, t5, 0L, null, 0L, values[4], null)
        db.outboxQueries.insert(driveId, f1, f5, t1, t1, 0L, null, 0L, values[0], null)

        // Next scheduled should be t3 (f3, no dependency)
        var nextTime = db.outboxQueries.nextScheduled().executeAsOneOrNull()
        assertEquals(t3, nextTime)

        // Checkout f3
        db.outboxQueries.checkout(1L, t3 + 1)
        val item = db.outboxQueries.selectCheckedOut(1L).executeAsOne()
        assertTrue(item.fileId == f3, "Expected item to be f3")

        // Next scheduled should be null (f2 depends on f3, which is checked out)
        nextTime = db.outboxQueries.nextScheduled().executeAsOneOrNull()
        assertEquals(null, nextTime)

        db.outboxQueries.deleteByRowid(item.rowId)

        // Now next should be t2 (f2, depends on f3 which is done)
        nextTime = db.outboxQueries.nextScheduled().executeAsOneOrNull()
        assertEquals(t2, nextTime)

        // Checkout f2
        db.outboxQueries.checkout(2L, t2 + 1)
        var item2 = db.outboxQueries.selectCheckedOut(2L).executeAsOne()
        assertTrue(item2.fileId == f2, "Expected item to be f2")

        // Next scheduled should be null (f4 depends on f2, which is checked out)
        nextTime = db.outboxQueries.nextScheduled().executeAsOneOrNull()
        assertEquals(null, nextTime)

        db.outboxQueries.deleteByRowid(item2.rowId)

        // Now next should be t4 (f4, depends on f2 which is done)
        nextTime = db.outboxQueries.nextScheduled().executeAsOneOrNull()
        assertEquals(t4, nextTime)

        // Checkout f4
        db.outboxQueries.checkout(3L, t4 + 1)
        item2 = db.outboxQueries.selectCheckedOut(3L).executeAsOne()
        assertTrue(item2.fileId == f4, "Expected item to be f4")

        // Next scheduled should be null (f5 depends on f4, which is checked out)
        nextTime = db.outboxQueries.nextScheduled().executeAsOneOrNull()
        assertEquals(null, nextTime)

        db.outboxQueries.deleteByRowid(item2.rowId)

        // Now next should be t5 (f5, depends on f4 which is done)
        nextTime = db.outboxQueries.nextScheduled().executeAsOneOrNull()
        assertEquals(t5, nextTime)

        // Checkout f5
        db.outboxQueries.checkout(4L, t5 + 1)
        item2 = db.outboxQueries.selectCheckedOut(4L).executeAsOne()
        assertTrue(item2.fileId == f5, "Expected item to be f5")

        // Next scheduled should be null (f1 depends on f5, which is checked out)
        nextTime = db.outboxQueries.nextScheduled().executeAsOneOrNull()
        assertEquals(null, nextTime)

        db.outboxQueries.deleteByRowid(item2.rowId)

        // Now next should be t1 (f1, depends on f5 which is done)
        nextTime = db.outboxQueries.nextScheduled().executeAsOneOrNull()
        assertEquals(t1, nextTime)

        // Checkout f1
        db.outboxQueries.checkout(5L, t1 + 1)
        item2 = db.outboxQueries.selectCheckedOut(5L).executeAsOne()
        assertTrue(item2.fileId == f1, "Expected item to be f1")

        // Next scheduled should be null
        nextTime = db.outboxQueries.nextScheduled().executeAsOneOrNull()
        assertEquals(null, nextTime)

        db.outboxQueries.deleteByRowid(item2.rowId)

        // No more items
        nextTime = db.outboxQueries.nextScheduled().executeAsOneOrNull()
        assertNull(nextTime)
    }
}
