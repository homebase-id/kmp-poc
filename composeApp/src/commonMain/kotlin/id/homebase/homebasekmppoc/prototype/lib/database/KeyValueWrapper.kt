package id.homebase.homebasekmppoc.lib.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import kotlin.Any
import kotlin.Long
import kotlin.uuid.Uuid

class KeyValueWrapper(
    driver: SqlDriver,
    keyValueAdapter: KeyValue.Adapter,
    private val databaseManager: DatabaseManager,
) {
    private val delegate = KeyValueQueries(driver, keyValueAdapter)

    fun <T : Any> selectByKey(
        key: Uuid,
        mapper: (
            key: Uuid,
            data: ByteArray,
        ) -> T,
    ): Query<T> = delegate.selectByKey(key, mapper)

    fun selectByKey(
        key: Uuid,
    ): Query<KeyValue> = delegate.selectByKey(key)

    suspend fun upsertValue(
        key: Uuid,
        data: ByteArray,
    ): Long
    {
        return databaseManager.withWriteValue { delegate.upsertValue(key, data).value }
    }

    suspend fun deleteByKey(
        key: Uuid,
    ): Long
    {
        return databaseManager.withWriteValue { delegate.deleteByKey(key).value }
    }
}