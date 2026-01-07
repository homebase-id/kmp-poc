package id.homebase.homebasekmppoc.prototype.lib.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlPreparedStatement
import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.lib.database.AppNotifications
import id.homebase.homebasekmppoc.lib.database.DriveLocalTagIndex
import id.homebase.homebasekmppoc.lib.database.DriveMainIndex
import id.homebase.homebasekmppoc.lib.database.DriveTagIndex
import id.homebase.homebasekmppoc.lib.database.KeyValue
import id.homebase.homebasekmppoc.lib.database.OdinDatabase
import id.homebase.homebasekmppoc.lib.database.Outbox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import id.homebase.homebasekmppoc.lib.database.AppNotificationsWrapper
import id.homebase.homebasekmppoc.lib.database.DriveMainIndexWrapper
import id.homebase.homebasekmppoc.lib.database.DriveTagIndexWrapper
import id.homebase.homebasekmppoc.lib.database.DriveLocalTagIndexWrapper
import id.homebase.homebasekmppoc.lib.database.KeyValueWrapper
import id.homebase.homebasekmppoc.lib.database.OutboxWrapper

// Adapters as top-level constants (stateless, shared)
private val appNotificationsAdapter = AppNotifications.Adapter(
    identityIdAdapter = UuidAdapter,
    notificationIdAdapter = UuidAdapter
)
private val driveMainIndexAdapter = DriveMainIndex.Adapter(
    identityIdAdapter = UuidAdapter,
    driveIdAdapter = UuidAdapter,
    fileIdAdapter = UuidAdapter,
    globalTransitIdAdapter = UuidAdapter,
    groupIdAdapter = UuidAdapter,
    uniqueIdAdapter = UuidAdapter
)
private val driveTagIndexAdapter = DriveTagIndex.Adapter(
    identityIdAdapter = UuidAdapter,
    driveIdAdapter = UuidAdapter,
    fileIdAdapter = UuidAdapter,
    tagIdAdapter = UuidAdapter
)
private val driveLocalTagIndexAdapter = DriveLocalTagIndex.Adapter(
    identityIdAdapter = UuidAdapter,
    driveIdAdapter = UuidAdapter,
    fileIdAdapter = UuidAdapter,
    tagIdAdapter = UuidAdapter
)
private val keyValueAdapter = KeyValue.Adapter(
    keyAdapter = UuidAdapter
)
private val outboxAdapter = Outbox.Adapter(
    driveIdAdapter = UuidAdapter,
    fileIdAdapter = UuidAdapter,
    dependencyFileIdAdapter = UuidAdapter
)

class DatabaseManager(driverProvider: () -> SqlDriver) : AutoCloseable
{
    private val logger = Logger.withTag("DatabaseManager")
    private var database: OdinDatabase
    private var driver: SqlDriver
    private val dbDispatcher = Dispatchers.IO.limitedParallelism(1)

    companion object {
        lateinit var instance: DatabaseManager
        private set

        fun initialize(driverProvider: () -> SqlDriver) {
            if (::instance.isInitialized) throw IllegalStateException("Already initialized")
            instance = DatabaseManager(driverProvider)
        }
        val appDb: DatabaseManager get() = instance
    }

    // Lazy wrappers
    public val keyValue: KeyValueWrapper by lazy { KeyValueWrapper(driver, keyValueAdapter, this) }
    public val appNotifications: AppNotificationsWrapper by lazy { AppNotificationsWrapper(driver, appNotificationsAdapter, this) }
    public val driveMainIndex: DriveMainIndexWrapper by lazy { DriveMainIndexWrapper(driver, driveMainIndexAdapter, this) }
    public val driveTagIndex: DriveTagIndexWrapper by lazy { DriveTagIndexWrapper(driver, driveTagIndexAdapter, this) }
    public val driveLocalTagIndex: DriveLocalTagIndexWrapper by lazy { DriveLocalTagIndexWrapper(driver, driveLocalTagIndexAdapter, this) }
    public val outbox: OutboxWrapper by lazy { OutboxWrapper(driver, outboxAdapter, this) }

    init {
        driver = driverProvider()
        database = OdinDatabase(
            driver,
            appNotificationsAdapter,
            driveLocalTagIndexAdapter,
            driveMainIndexAdapter,
            driveTagIndexAdapter,
            keyValueAdapter,
            outboxAdapter
        )
        logger.i { "Database initialized" }
    }

    suspend fun <R> executeReadQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)? = null
    ): QueryResult<R> = driver.executeQuery(identifier, sql, mapper, parameters, binders)

    suspend fun withWriteTransaction(block: (OdinDatabase) -> Unit) {
        withContext(dbDispatcher) {
            database.transaction { block(database) }
        }
    }

    suspend fun withWrite(block: (OdinDatabase) -> Unit) {
        withContext(dbDispatcher) { block(database) }
    }

    suspend fun <R> withWriteValue(block: (OdinDatabase) -> R): R = withContext(dbDispatcher) {
        block(database)
    }

    override fun close() {
        driver.close()
        logger.i { "Database closed" }
    }
}
