package id.homebase.homebasekmppoc.lib.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.uuid.Uuid

class KeyValueWrapper(
    driver: SqlDriver,
    keyValueAdapter: KeyValue.Adapter,
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

    fun upsertValue(
        key: Uuid,
        data: ByteArray,
    ): QueryResult<Long> = delegate.upsertValue(key, data)

    fun deleteByKey(
        key: Uuid,
    ): QueryResult<Long> = delegate.deleteByKey(key)
}