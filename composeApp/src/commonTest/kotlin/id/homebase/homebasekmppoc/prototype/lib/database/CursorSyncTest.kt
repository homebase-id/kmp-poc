package id.homebase.homebasekmppoc.prototype.lib.database

import app.cash.sqldelight.db.SqlDriver
import id.homebase.homebasekmppoc.lib.database.OdinDatabase
import id.homebase.homebasekmppoc.prototype.lib.core.time.UnixTimeUtc
import id.homebase.homebasekmppoc.prototype.lib.drives.query.QueryBatchCursor
import id.homebase.homebasekmppoc.prototype.lib.drives.query.TimeRowCursor
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CursorSyncTest {
    private var driver: SqlDriver? = null
    private lateinit var db: OdinDatabase
    private lateinit var cursorSync: CursorSync

@BeforeTest
    fun setup() {
        driver = createInMemoryDatabase()
        db = TestDatabaseFactory.createTestDatabase(driver)
        cursorSync = CursorSync(db)
    }

    @AfterTest
    fun tearDown() {
        driver?.close()
    }

    @Test
    fun testSaveAndLoadQueryBatchCursor_withAllFields() = runTest {
        // Create a QueryBatchCursor with all fields populated
        val originalCursor = QueryBatchCursor(
            pagingCursor = TimeRowCursor(
                time = UnixTimeUtc(1704067200000L), // 2024-01-01 00:00:00 UTC
                rowId = 12345L
            ),
            stopAtBoundary = TimeRowCursor(
                time = UnixTimeUtc(1704153600000L), // 2024-01-02 00:00:00 UTC
                rowId = 67890L
            ),
            nextBoundaryCursor = TimeRowCursor(
                time = UnixTimeUtc(1704240000000L), // 2024-01-03 00:00:00 UTC
                rowId = 11111L
            )
        )

        // Save the cursor
        cursorSync.saveCursor(originalCursor)

        // Load the cursor back
        val loadedCursor = cursorSync.loadCursor()

        // Verify that cursor was loaded successfully
        assertNotNull(loadedCursor, "Cursor should be loaded after saving")

        // Verify all fields are loaded correctly
        assertNotNull(loadedCursor!!.pagingCursor, "Paging cursor should not be null")
        assertNotNull(loadedCursor.stopAtBoundary, "Stop at boundary cursor should not be null")
        assertNotNull(loadedCursor.nextBoundaryCursor, "Next boundary cursor should not be null")

        // Verify paging cursor fields
        assertEquals(
            originalCursor.pagingCursor!!.time,
            loadedCursor.pagingCursor!!.time,
            "Paging cursor time should match"
        )
        assertEquals(
            originalCursor.pagingCursor!!.rowId,
            loadedCursor.pagingCursor!!.rowId,
            "Paging cursor row ID should match"
        )

        // Verify stop at boundary cursor fields
        assertEquals(
            originalCursor.stopAtBoundary!!.time,
            loadedCursor.stopAtBoundary!!.time,
            "Stop at boundary cursor time should match"
        )
        assertEquals(
            originalCursor.stopAtBoundary!!.rowId,
            loadedCursor.stopAtBoundary!!.rowId,
            "Stop at boundary cursor row ID should match"
        )

        // Verify next boundary cursor fields
        assertEquals(
            originalCursor.nextBoundaryCursor!!.time,
            loadedCursor.nextBoundaryCursor!!.time,
            "Next boundary cursor time should match"
        )
        assertEquals(
            originalCursor.nextBoundaryCursor!!.rowId,
            loadedCursor.nextBoundaryCursor!!.rowId,
            "Next boundary cursor row ID should match"
        )
    }

    @Test
    fun testLoadCursor_whenNoCursorExists_returnsNull() = runTest {
        // Try to load cursor when none exists
        val loadedCursor = cursorSync.loadCursor()

        // Verify that null is returned
        assertNull(loadedCursor, "Should return null when no cursor exists")
    }

    @Test
    fun testSaveAndLoadQueryBatchCursor_withNullFields() = runTest {
        // Create a QueryBatchCursor with some null fields
        val originalCursor = QueryBatchCursor(
            pagingCursor = TimeRowCursor(
                time = UnixTimeUtc(1704067200000L),
                rowId = null
            ),
            stopAtBoundary = null,
            nextBoundaryCursor = null
        )

        // Save the cursor
        cursorSync.saveCursor(originalCursor)

        // Load the cursor back
        val loadedCursor = cursorSync.loadCursor()

        // Verify that cursor was loaded successfully
        assertNotNull(loadedCursor, "Cursor should be loaded after saving")

        // Verify paging cursor fields
        assertNotNull(loadedCursor!!.pagingCursor, "Paging cursor should not be null")
        assertEquals(
            originalCursor.pagingCursor!!.time,
            loadedCursor.pagingCursor!!.time,
            "Paging cursor time should match"
        )
        assertEquals(
            originalCursor.pagingCursor!!.rowId,
            loadedCursor.pagingCursor!!.rowId,
            "Paging cursor row ID should match"
        )

        // Verify that null fields remain null
        assertNull(loadedCursor.stopAtBoundary, "Stop at boundary cursor should remain null")
        assertNull(loadedCursor.nextBoundaryCursor, "Next boundary cursor should remain null")
    }

    @Test
    fun testDeleteCursor() = runTest {
        // Create and save a cursor
        val originalCursor = QueryBatchCursor(
            pagingCursor = TimeRowCursor(
                time = UnixTimeUtc(1704067200000L),
                rowId = 12345L
            ),
            stopAtBoundary = null,
            nextBoundaryCursor = null
        )
        cursorSync.saveCursor(originalCursor)

        // Verify cursor exists
        assertNotNull(cursorSync.loadCursor(), "Cursor should exist after saving")

        // Delete the cursor
        cursorSync.deleteCursor()

        // Verify cursor is deleted
        assertNull(cursorSync.loadCursor(), "Cursor should be null after deletion")
    }

    @Test
    fun testUpdateCursor() = runTest {
        // Create and save initial cursor
        val initialCursor = QueryBatchCursor(
            pagingCursor = TimeRowCursor(
                time = UnixTimeUtc(1704067200000L),
                rowId = 12345L
            ),
            stopAtBoundary = null,
            nextBoundaryCursor = null
        )
        cursorSync.saveCursor(initialCursor)

        // Verify initial cursor
        val loadedInitial = cursorSync.loadCursor()
        assertNotNull(loadedInitial)
        assertEquals(1704067200000L, loadedInitial!!.pagingCursor!!.time.milliseconds)

        // Update with new cursor
        val updatedCursor = QueryBatchCursor(
            pagingCursor = TimeRowCursor(
                time = UnixTimeUtc(1704153600000L), // Different time
                rowId = 54321L // Different row ID
            ),
            stopAtBoundary = TimeRowCursor(
                time = UnixTimeUtc(1704240000000L),
                rowId = 98765L
            ),
            nextBoundaryCursor = TimeRowCursor(
                time = UnixTimeUtc(1704326400000L),
                rowId = 11111L
            )
        )
        cursorSync.saveCursor(updatedCursor)

        // Verify cursor was updated
        val loadedUpdated = cursorSync.loadCursor()
        assertNotNull(loadedUpdated)
        assertEquals(1704153600000L, loadedUpdated!!.pagingCursor!!.time.milliseconds, "Paging cursor should be updated")
        assertEquals(54321L, loadedUpdated.pagingCursor!!.rowId, "Paging cursor row ID should be updated")
        assertEquals(1704240000000L, loadedUpdated.stopAtBoundary!!.time.milliseconds, "Stop at boundary should be updated")
        assertEquals(98765L, loadedUpdated.stopAtBoundary!!.rowId, "Stop at boundary row ID should be updated")
        assertEquals(1704326400000L, loadedUpdated.nextBoundaryCursor!!.time.milliseconds, "Next boundary should be updated")
        assertEquals(11111L, loadedUpdated.nextBoundaryCursor!!.rowId, "Next boundary row ID should be updated")
    }
}