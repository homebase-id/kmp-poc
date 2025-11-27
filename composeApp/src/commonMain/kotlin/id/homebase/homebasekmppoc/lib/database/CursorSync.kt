package id.homebase.homebasekmppoc.lib.database

import id.homebase.homebasekmppoc.lib.drives.query.QueryBatchCursor
import kotlin.uuid.Uuid

/**
 * Handles cursor synchronization operations using KeyValue database API.
 * Provides functionality to load and save QueryBatchCursor for efficient data synchronization.
 */
class CursorSync(
    private val database: OdinDatabase
) {
    companion object {
        // Private predefined Guid for cursor operations
        private val CURSOR_GUID = Uuid.parse("f28bd9a7-fe1f-4242-a0e7-7d93e5919250")
    }
    
    /**
     * Load QueryBatchCursor for the predefined cursor Guid
     * Returns null if no cursor is found in the database
     */
    fun loadCursor(): QueryBatchCursor? {
        return database.keyValueQueries.selectByKey(CURSOR_GUID)
            .executeAsOneOrNull()
            ?.let { QueryBatchCursor.fromJson(it.data_.decodeToString()) }
    }
    
    /**
     * Save QueryBatchCursor for the predefined cursor Guid
     */
    fun saveCursor(cursor: QueryBatchCursor) {
        database.keyValueQueries.upsertValue(
            key = CURSOR_GUID,
            data_ = cursor.toJson().encodeToByteArray()
        )
    }
    
    /**
     * Delete cursor position for the predefined cursor Guid
     */
    fun deleteCursor() {
        database.keyValueQueries.deleteByKey(CURSOR_GUID)
    }
}