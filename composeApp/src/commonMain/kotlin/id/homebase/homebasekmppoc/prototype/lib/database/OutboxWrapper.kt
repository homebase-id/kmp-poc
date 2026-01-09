package id.homebase.homebasekmppoc.lib.database

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import id.homebase.homebasekmppoc.prototype.lib.core.time.UnixTimeUtc
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import kotlin.Any
import kotlin.Long
import kotlin.uuid.Uuid

class OutboxWrapper(
    driver: SqlDriver,
    outboxAdapter: Outbox.Adapter,
    private val databaseManager: DatabaseManager
) {
    private val delegate = OutboxQueries(driver, outboxAdapter)

    suspend fun checkout(
        checkOutStamp: UnixTimeUtc
    ): Outbox?
    {
        return databaseManager.withWriteValue { delegate.checkout(checkOutStamp.milliseconds, UnixTimeUtc.now().milliseconds).executeAsOneOrNull() }
    }

    fun nextScheduled(): UnixTimeUtc?
    {
        val n = delegate.nextScheduled().executeAsOneOrNull()

        return if (n == null) null else return UnixTimeUtc(n)
    }

    fun selectCheckedOut(
        checkOutStamp: Long,
    ): Outbox? = delegate.selectCheckedOut(checkOutStamp).executeAsOneOrNull()

    fun count(): Long = delegate.count().executeAsOne()

    suspend fun insert(
        driveId: Uuid,
        fileId: Uuid,
        dependencyFileId: Uuid?,
        priority: Long,
        lastAttempt: Long,
        nextRunTime: Long,
        checkOutCount: Long,
        checkOutStamp: Long?,
        uploadType: Long,
        json: ByteArray,
        files: ByteArray?,
    ): Long {
        return databaseManager.withWriteValue {
            delegate.insert(
                driveId,
                fileId,
                dependencyFileId,
                priority,
                lastAttempt,
                nextRunTime,
                checkOutCount,
                checkOutStamp,
                uploadType,
                json,
                files
            ).value
        }
    }

    suspend fun checkInFailed(
        checkOutStamp: Long,
        nextRunTime: Long,
    ): Long {
        return databaseManager.withWriteValue {
            delegate.checkInFailed(checkOutStamp, nextRunTime).value
        }
    }


    suspend fun clearCheckedOut(): Long
    {
        return databaseManager.withWriteValue {
            delegate.clearCheckedOut().value
        }
    }

    suspend fun deleteByRowId(
        rowId: Long,
    ): Long
    {
        return databaseManager.withWriteValue {
            delegate.deleteByRowId(rowId).value
        }
    }
}