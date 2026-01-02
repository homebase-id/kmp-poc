package id.homebase.homebasekmppoc.lib.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import kotlin.Any
import kotlin.Long
import kotlin.uuid.Uuid

class AppNotificationsWrapper(
    driver: SqlDriver,
    appNotificationsAdapter: AppNotifications.Adapter,
) {
    private val delegate = AppNotificationsQueries(driver, appNotificationsAdapter)

    fun <T : Any> selectByNotificationId(
        identityId: Uuid,
        notificationId: Uuid,
        mapper: (
            rowId: Long,
            identityId: Uuid,
            notificationId: Uuid,
            unread: Long,
            senderId: String?,
            timestamp: Long,
            data: ByteArray?,
            created: Long,
            modified: Long,
        ) -> T,
    ): Query<T> = delegate.selectByNotificationId(identityId, notificationId, mapper)

    fun selectByNotificationId(
        identityId: Uuid,
        notificationId: Uuid,
    ): Query<AppNotifications> = delegate.selectByNotificationId(identityId, notificationId)

    fun <T : Any> selectFirstPage(
        identityId: Uuid,
        limit: Long,
        mapper: (
            rowId: Long,
            identityId: Uuid,
            notificationId: Uuid,
            unread: Long,
            senderId: String?,
            timestamp: Long,
            data: ByteArray?,
            created: Long,
            modified: Long,
        ) -> T,
    ): Query<T> = delegate.selectFirstPage(identityId, limit, mapper)

    fun selectFirstPage(
        identityId: Uuid,
        limit: Long,
    ): Query<AppNotifications> = delegate.selectFirstPage(identityId, limit)

    fun <T : Any> selectNextPage(
        identityId: Uuid,
        rowId: Long,
        limit: Long,
        mapper: (
            rowId: Long,
            identityId: Uuid,
            notificationId: Uuid,
            unread: Long,
            senderId: String?,
            timestamp: Long,
            data: ByteArray?,
            created: Long,
            modified: Long,
        ) -> T,
    ): Query<T> = delegate.selectNextPage(identityId, rowId, limit, mapper)

    fun selectNextPage(
        identityId: Uuid,
        rowId: Long,
        limit: Long,
    ): Query<AppNotifications> = delegate.selectNextPage(identityId, rowId, limit)

    fun insertNotification(
        identityId: Uuid,
        notificationId: Uuid,
        unread: Long,
        senderId: String?,
        timestamp: Long,
        data: ByteArray?,
        created: Long,
        modified: Long,
    ): QueryResult<Long> = delegate.insertNotification(identityId, notificationId, unread, senderId, timestamp, data, created, modified)

    suspend fun deleteAll(
        identityId: Uuid,
    ): Long {
        return DatabaseManager.withWriteValue { db -> delegate.deleteAll(identityId).value }
    }

    suspend fun deleteByNotificationId(
        identityId: Uuid,
        notificationId: Uuid,
    ): Long {
        return DatabaseManager.withWriteValue { db ->
            delegate.deleteByNotificationId(identityId, notificationId).value
        }
    }
}