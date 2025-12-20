package id.homebase.homebasekmppoc.prototype.lib.database

import app.cash.sqldelight.db.SqlDriver
import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.lib.database.AppNotifications
import id.homebase.homebasekmppoc.lib.database.DriveLocalTagIndex
import id.homebase.homebasekmppoc.lib.database.DriveMainIndex
import id.homebase.homebasekmppoc.lib.database.DriveTagIndex
import id.homebase.homebasekmppoc.lib.database.KeyValue
import id.homebase.homebasekmppoc.lib.database.OdinDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

object DatabaseManager {
    private var database: OdinDatabase? = null
    private var driver: SqlDriver? = null
    private val logger = Logger.withTag("DatabaseManager")
    private val writeMutex = Mutex()

    val dbDispatcher = Dispatchers.IO.limitedParallelism(1)

    // Nested transaction tracking
    private var nestedTransactionCount = 0
    private var transactionRollbackRequested = false

    fun initialize(driverFactory: DatabaseDriverFactory) {
        if (database == null) {
            logger.i { "Initializing thread-safe database..." }
            driver = driverFactory.createDriver()
            
            // Create adapters for UUID columns
            val driveTagIndexAdapter = DriveTagIndex.Adapter(
                identityIdAdapter = UuidAdapter,
                driveIdAdapter = UuidAdapter,
                fileIdAdapter = UuidAdapter,
                tagIdAdapter = UuidAdapter
            )
            
            val driveLocalTagIndexAdapter = DriveLocalTagIndex.Adapter(
                identityIdAdapter = UuidAdapter,
                driveIdAdapter = UuidAdapter,
                fileIdAdapter = UuidAdapter,
                tagIdAdapter = UuidAdapter
            )
            
            val driveMainIndexAdapter = DriveMainIndex.Adapter(
                identityIdAdapter = UuidAdapter,
                driveIdAdapter = UuidAdapter,
                fileIdAdapter = UuidAdapter,
                globalTransitIdAdapter = UuidAdapter,
                groupIdAdapter = UuidAdapter,
                uniqueIdAdapter = UuidAdapter
            )
            
            val keyValueAdapter = KeyValue.Adapter(
                keyAdapter = UuidAdapter
            )
            
            val appNotificationsAdapter = AppNotifications.Adapter(
                identityIdAdapter = UuidAdapter,
                notificationIdAdapter = UuidAdapter
            )
            
            database = OdinDatabase(driver!!, appNotificationsAdapter, driveLocalTagIndexAdapter, driveMainIndexAdapter, driveTagIndexAdapter, keyValueAdapter)
            logger.i { "Database initialized successfully" }
        } else {
            logger.w { "Database already initialized" }
        }
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

    fun getDatabase(): OdinDatabase {
        return database ?: throw IllegalStateException("Database not initialized. Call initialize() first.")
    }

    fun getDriver(): SqlDriver {
        return driver ?: throw IllegalStateException("Database not initialized. Call initialize() first.")
    }

    fun isInitialized(): Boolean = database != null

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
    }

    /**
     * Initialize DatabaseManager with a custom driver creation function.
     * Primarily intended for tests that need in-memory databases.
     */
    fun initialize(driverCreator: () -> SqlDriver) {
        if (database == null) {
            logger.i { "Initializing thread-safe database with custom driver..." }
            driver = driverCreator()
            
            // Create adapters for UUID columns
            val driveTagIndexAdapter = DriveTagIndex.Adapter(
                identityIdAdapter = UuidAdapter,
                driveIdAdapter = UuidAdapter,
                fileIdAdapter = UuidAdapter,
                tagIdAdapter = UuidAdapter
            )
            
            val driveLocalTagIndexAdapter = DriveLocalTagIndex.Adapter(
                identityIdAdapter = UuidAdapter,
                driveIdAdapter = UuidAdapter,
                fileIdAdapter = UuidAdapter,
                tagIdAdapter = UuidAdapter
            )
            
            val driveMainIndexAdapter = DriveMainIndex.Adapter(
                identityIdAdapter = UuidAdapter,
                driveIdAdapter = UuidAdapter,
                fileIdAdapter = UuidAdapter,
                globalTransitIdAdapter = UuidAdapter,
                groupIdAdapter = UuidAdapter,
                uniqueIdAdapter = UuidAdapter
            )
            
            val keyValueAdapter = KeyValue.Adapter(
                keyAdapter = UuidAdapter
            )
            
            val appNotificationsAdapter = AppNotifications.Adapter(
                identityIdAdapter = UuidAdapter,
                notificationIdAdapter = UuidAdapter
            )
            
            database = OdinDatabase(driver!!, appNotificationsAdapter, driveLocalTagIndexAdapter, driveMainIndexAdapter, driveTagIndexAdapter, keyValueAdapter)
            logger.i { "Database initialized successfully with custom driver" }
        } else {
            logger.w { "Database already initialized" }
        }
    }
}