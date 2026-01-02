package id.homebase.homebasekmppoc.lib.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.uuid.Uuid

class DriveTagIndexWrapper(
    driver: SqlDriver,
    driveTagIndexAdapter: DriveTagIndex.Adapter,
) {
    private val delegate = DriveTagIndexQueries(driver, driveTagIndexAdapter)

    fun <T : Any> selectByFile(
        identityId: Uuid,
        driveId: Uuid,
        fileId: Uuid,
        mapper: (
            rowId: Long,
            identityId: Uuid,
            driveId: Uuid,
            fileId: Uuid,
            tagId: Uuid,
        ) -> T,
    ): Query<T> = delegate.selectByFile(identityId, driveId, fileId, mapper)

    fun selectByFile(
        identityId: Uuid,
        driveId: Uuid,
        fileId: Uuid,
    ): Query<DriveTagIndex> = delegate.selectByFile(identityId, driveId, fileId)

    fun countAll(): Query<Long> = delegate.countAll()

    fun insertTag(
        identityId: Uuid,
        driveId: Uuid,
        fileId: Uuid,
        tagId: Uuid,
    ): QueryResult<Long> = delegate.insertTag(identityId, driveId, fileId, tagId)

    fun deleteByFile(
        identityId: Uuid,
        driveId: Uuid,
        fileId: Uuid,
    ): QueryResult<Long> = delegate.deleteByFile(identityId, driveId, fileId)

    fun deleteAll(): QueryResult<Long> = delegate.deleteAll()
}