package id.homebase.homebasekmppoc.lib.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import kotlin.Any
import kotlin.Long
import kotlin.uuid.Uuid

class DriveTagIndexWrapper(
    driver: SqlDriver,
    driveTagIndexAdapter: DriveTagIndex.Adapter,
) {
    private val delegate = DriveTagIndexQueries(driver, driveTagIndexAdapter)

//    fun <T : Any> selectByFile(
//        identityId: Uuid,
//        driveId: Uuid,
//        fileId: Uuid,
//        mapper: (
//            rowId: Long,
//            identityId: Uuid,
//            driveId: Uuid,
//            fileId: Uuid,
//            tagId: Uuid,
//        ) -> T,
//    ): Query<T> = delegate.selectByFile(identityId, driveId, fileId, mapper)

    fun selectByFile(
        identityId: Uuid,
        driveId: Uuid,
        fileId: Uuid,
    ): Query<DriveTagIndex> = delegate.selectByFile(identityId, driveId, fileId)

    fun countAll(): Query<Long> = delegate.countAll()

    suspend fun insertTag(
        identityId: Uuid,
        driveId: Uuid,
        fileId: Uuid,
        tagId: Uuid,
    ): Long {
        return DatabaseManager.withWriteValue { delegate.insertTag(identityId, driveId, fileId, tagId).value }
    }

    suspend fun deleteByFile(
        identityId: Uuid,
        driveId: Uuid,
        fileId: Uuid,
    ): Long
    {
        return DatabaseManager.withWriteValue { delegate.deleteByFile(identityId, driveId, fileId).value }
    }

    suspend fun deleteAll(): Long {
        return DatabaseManager.withWriteValue { delegate.deleteAll().value }
    }
}