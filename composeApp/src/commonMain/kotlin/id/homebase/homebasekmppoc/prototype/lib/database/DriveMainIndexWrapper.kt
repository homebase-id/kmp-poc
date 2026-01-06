package id.homebase.homebasekmppoc.lib.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import kotlin.Any
import kotlin.Long
import kotlin.uuid.Uuid

class DriveMainIndexWrapper(
    driver: SqlDriver,
    driveMainIndexAdapter: DriveMainIndex.Adapter,
    private val databaseManager: DatabaseManager,
) {
    private val delegate = DriveMainIndexQueries(driver, driveMainIndexAdapter)

    fun <T : Any> selectByIdentityAndDriveAndFile(
        identityId: Uuid,
        driveId: Uuid,
        fileId: Uuid,
        mapper: (
            rowId: Long,
            identityId: Uuid,
            driveId: Uuid,
            fileId: Uuid?,
            uniqueId: Uuid?,
            globalTransitId: Uuid?,
            senderId: String?,
            groupId: Uuid?,
            fileType: Long,
            dataType: Long,
            archivalStatus: Long,
            historyStatus: Long,
            userDate: Long,
            created: Long,
            modified: Long,
            fileSystemType: Long,
            jsonHeader: String,
        ) -> T,
    ): Query<T> = delegate.selectByIdentityAndDriveAndFile(identityId, driveId, fileId, mapper)

    fun selectByIdentityAndDriveAndFile(
        identityId: Uuid,
        driveId: Uuid,
        fileId: Uuid,
    ): Query<DriveMainIndex> = delegate.selectByIdentityAndDriveAndFile(identityId, driveId, fileId)

    fun selectByIdentityAndDriveAndUnique(
        identityId: Uuid,
        driveId: Uuid,
        uniqueId: Uuid,
    ): Query<DriveMainIndex> = delegate.selectByIdentityAndDriveAndUnique(identityId, driveId, uniqueId)


    fun selectByIdentityAndDriveAndGlobal(
        identityId: Uuid,
        driveId: Uuid,
        globalTransitId: Uuid,
    ): Query<DriveMainIndex> = delegate.selectByIdentityAndDriveAndGlobal(identityId, driveId, globalTransitId)


    fun <T : Any> selectAll(
        mapper: (
            rowId: Long,
            identityId: Uuid,
            driveId: Uuid,
            fileId: Uuid?,
            uniqueId: Uuid?,
            globalTransitId: Uuid?,
            senderId: String?,
            groupId: Uuid?,
            fileType: Long,
            dataType: Long,
            archivalStatus: Long,
            historyStatus: Long,
            userDate: Long,
            created: Long,
            modified: Long,
            fileSystemType: Long,
            jsonHeader: String,
        ) -> T,
    ): Query<T> = delegate.selectAll(mapper)

    fun selectAll(): Query<DriveMainIndex> = delegate.selectAll()

    fun countAll(): Query<Long> = delegate.countAll()

    suspend fun upsertDriveMainIndex(
        identityId: Uuid,
        driveId: Uuid,
        fileId: Uuid?,
        uniqueId: Uuid?,
        globalTransitId: Uuid?,
        groupId: Uuid?,
        senderId: String?,
        fileType: Long,
        dataType: Long,
        archivalStatus: Long,
        historyStatus: Long,
        userDate: Long,
        created: Long,
        modified: Long,
        fileSystemType: Long,
        jsonHeader: String,
    ): Long
    {
        return databaseManager.withWriteValue {
            delegate.upsertDriveMainIndex(identityId, driveId, fileId, uniqueId, globalTransitId, groupId, senderId, fileType, dataType, archivalStatus, historyStatus, userDate, created, modified, fileSystemType, jsonHeader).value
        }
    }

    suspend fun deleteAll(): Long
    {
        return databaseManager.withWriteValue { delegate.deleteAll().value }
    }

    suspend fun deleteBy(
        identityId: Uuid,
        driveId: Uuid,
        fileId: Uuid,
    ): Long
    {
        return databaseManager.withWriteValue { delegate.deleteBy(identityId, driveId, fileId).value }
    }
}