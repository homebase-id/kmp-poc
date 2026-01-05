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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import id.homebase.homebasekmppoc.lib.database.AppNotificationsWrapper
import id.homebase.homebasekmppoc.lib.database.DriveMainIndexWrapper
import id.homebase.homebasekmppoc.lib.database.DriveTagIndexWrapper
import id.homebase.homebasekmppoc.lib.database.DriveLocalTagIndexWrapper
import id.homebase.homebasekmppoc.lib.database.KeyValueWrapper
import id.homebase.homebasekmppoc.lib.database.OutboxWrapper

object DatabaseManager {
    private var initialized: Boolean = false
    private var database: OdinDatabase? = null
    private var driver: SqlDriver? = null
    private val logger = Logger.withTag("DatabaseManager")
    private val writeMutex = Mutex()

    val dbDispatcher = Dispatchers.IO.limitedParallelism(1)

    // Nested transaction tracking
    private var nestedTransactionCount = 0
    private var transactionRollbackRequested = false

    // WRAPPED SQLDELIGHT CLASSES
    private var appNotificationsAdapter: AppNotifications.Adapter? = null
    private var driveMainIndexAdapter: DriveMainIndex.Adapter? = null
    private var driveTagIndexAdapter: DriveTagIndex.Adapter? = null
    private var driveLocalTagIndexAdapter: DriveLocalTagIndex.Adapter? = null
    private var keyValueAdapter: KeyValue.Adapter? = null
    private var outboxAdapter: Outbox.Adapter? = null

    private fun myInitialize(driver : SqlDriver)
    {
        if (!initialized)
        {
            initialized = true
            logger.i { "Initializing thread-safe database..." }

            // Create adapters for UUID columns
            val driveTagIndexAdapter = DriveTagIndex.Adapter(
                identityIdAdapter = UuidAdapter,
                driveIdAdapter = UuidAdapter,
                fileIdAdapter = UuidAdapter,
                tagIdAdapter = UuidAdapter
            )
            this.driveTagIndexAdapter = driveTagIndexAdapter

            val driveLocalTagIndexAdapter = DriveLocalTagIndex.Adapter(
                identityIdAdapter = UuidAdapter,
                driveIdAdapter = UuidAdapter,
                fileIdAdapter = UuidAdapter,
                tagIdAdapter = UuidAdapter
            )
            this.driveLocalTagIndexAdapter = driveLocalTagIndexAdapter

            val driveMainIndexAdapter = DriveMainIndex.Adapter(
                identityIdAdapter = UuidAdapter,
                driveIdAdapter = UuidAdapter,
                fileIdAdapter = UuidAdapter,
                globalTransitIdAdapter = UuidAdapter,
                groupIdAdapter = UuidAdapter,
                uniqueIdAdapter = UuidAdapter
            )
            this.driveMainIndexAdapter = driveMainIndexAdapter

            val outboxAdapter = Outbox.Adapter(
                driveIdAdapter = UuidAdapter,
                fileIdAdapter = UuidAdapter,
                dependencyFileIdAdapter = UuidAdapter
            )
            this.outboxAdapter = outboxAdapter

            val keyValueAdapter = KeyValue.Adapter(
                keyAdapter = UuidAdapter
            )
            this.keyValueAdapter = keyValueAdapter

            val appNotificationsAdapter = AppNotifications.Adapter(
                identityIdAdapter = UuidAdapter,
                notificationIdAdapter = UuidAdapter
            )
            this.appNotificationsAdapter = appNotificationsAdapter

            database = OdinDatabase(driver!!, appNotificationsAdapter, driveLocalTagIndexAdapter, driveMainIndexAdapter, driveTagIndexAdapter, keyValueAdapter, outboxAdapter)
            logger.i { "Database initialized successfully" }
        } else {
            logger.w { "Database already initialized" }
        }
    }

    fun initialize(driverFactory: DatabaseDriverFactory)
    {
        driver = driverFactory.createDriver()
        myInitialize(driver!!);
    }

    /**
     * Initialize DatabaseManager with a custom driver creation function.
     * Primarily intended for tests that need in-memory databases.
     */
    fun initialize(driverCreator: () -> SqlDriver) {
        driver = driverCreator()
        myInitialize(driver!!)
    }


    suspend fun <R> executeReadQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)? = null
    ): QueryResult<R>
    {
        return getDriver().executeQuery(identifier, sql, mapper, parameters, binders);
    }

    suspend fun withWriteTransaction(block: (OdinDatabase) -> Unit) {
        withContext(dbDispatcher) {
            val db = getDatabase()
            db.transaction {
                block(db)
            }
        }
    }

    suspend fun withWrite(block: (OdinDatabase) -> Unit) {
        withContext(dbDispatcher) {
            val db = getDatabase()
            block(db)
        }
    }

    suspend fun <R> withWriteValue(block: (OdinDatabase) -> R): R = withContext(dbDispatcher) {
        block(getDatabase())
    }

    // Any function wanting to write should use withWrite... to get to the database
    private fun getDatabase(): OdinDatabase {
        return database ?: throw IllegalStateException("Database not initialized. Call initialize() first.")
    }

    private fun getDriver(): SqlDriver {
        return driver ?: throw IllegalStateException("Database not initialized. Call initialize() first.")
    }

    fun isInitialized(): Boolean = database != null

    val driveTagIndex: DriveTagIndexWrapper by lazy {
        DriveTagIndexWrapper(getDriver(), driveTagIndexAdapter!!)
    }

    val driveLocalTagIndex: DriveLocalTagIndexWrapper by lazy {
        DriveLocalTagIndexWrapper(getDriver(), driveLocalTagIndexAdapter!!)
    }

    val outbox: OutboxWrapper by lazy {
        OutboxWrapper(getDriver(), outboxAdapter!!)
    }

    val keyValue: KeyValueWrapper by lazy {
        KeyValueWrapper(getDriver(), keyValueAdapter!!)
    }

    val appNotifications: AppNotificationsWrapper by lazy {
        AppNotificationsWrapper(getDriver(), appNotificationsAdapter!!)
    }

    val driveMainIndex: DriveMainIndexWrapper by lazy {
        DriveMainIndexWrapper(getDriver(), driveMainIndexAdapter!!)
    }


    /**
     * Close database and reset the manager.
     * Primarily intended for test cleanup to ensure test isolation.
     */
    fun close() {
        driver?.close()
        database = null
        driver = null
        nestedTransactionCount = 0
        transactionRollbackRequested = false
        logger.i { "Database closed and manager reset" }
        initialized = false;
    }
}