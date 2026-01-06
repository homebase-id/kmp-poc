package id.homebase.homebasekmppoc.lib.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.uuid.Uuid

class OutboxWrapper(
    driver: SqlDriver,
    outboxAdapter: Outbox.Adapter,
) {
    private val delegate = OutboxQueries(driver, outboxAdapter)

    fun checkout(
        checkOutStamp: Long,
        now: Long,
    ): QueryResult<Long> = delegate.checkout(checkOutStamp, now)

    fun nextScheduled(): Query<Long> = delegate.nextScheduled()

    fun <T : Any> selectCheckedOut(
        checkOutStamp: Long,
        mapper: (
            rowId: Long,
            driveId: Uuid,
            fileId: Uuid,
            dependencyFileId: Uuid?,
            lastAttempt: Long,
            nextRunTime: Long,
            checkOutCount: Long,
            checkOutStamp: Long?,
            priority: Long,
            data: ByteArray,
            files: ByteArray?,
        ) -> T,
    ): Query<T> = delegate.selectCheckedOut(checkOutStamp, mapper)

    fun selectCheckedOut(
        checkOutStamp: Long,
    ): Query<SelectCheckedOut> = delegate.selectCheckedOut(checkOutStamp)

    fun count(): Query<Long> = delegate.count()

    fun insert(
        driveId: Uuid,
        fileId: Uuid,
        dependencyFileId: Uuid?,
        lastAttempt: Long,
        nextRunTime: Long,
        checkOutCount: Long,
        checkOutStamp: Long?,
        priority: Long,
        data: ByteArray,
        files: ByteArray?,
    ): QueryResult<Long> = delegate.insert(driveId, fileId, dependencyFileId, lastAttempt, nextRunTime, checkOutCount, checkOutStamp, priority, data, files)

    fun checkInFailed(
        checkOutStamp: Long,
        nextRunTime: Long,
    ): QueryResult<Long> = delegate.checkInFailed(checkOutStamp, nextRunTime)

    fun clearCheckedOut(): QueryResult<Long> = delegate.clearCheckedOut()

    fun deleteByRowid(
        rowId: Long,
    ): QueryResult<Long> = delegate.deleteByRowid(rowId)

    fun deleteBy(
        driveId: Uuid,
        fileId: Uuid,
    ): QueryResult<Long> = delegate.deleteBy(driveId, fileId)
}