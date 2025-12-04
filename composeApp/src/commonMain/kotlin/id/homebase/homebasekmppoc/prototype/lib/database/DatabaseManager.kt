package id.homebase.homebasekmppoc.prototype.lib.database

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.lib.database.AppNotifications
import id.homebase.homebasekmppoc.lib.database.DriveLocalTagIndex
import id.homebase.homebasekmppoc.lib.database.DriveMainIndex
import id.homebase.homebasekmppoc.lib.database.DriveTagIndex
import id.homebase.homebasekmppoc.lib.database.KeyValue
import id.homebase.homebasekmppoc.lib.database.OdinDatabase

object DatabaseManager {
    private var database: OdinDatabase? = null
    private val logger = Logger.withTag("DatabaseManager")

    fun initialize(driverFactory: DatabaseDriverFactory) {
        if (database == null) {
            logger.i { "Initializing database..." }
            val driver = driverFactory.createDriver()
            
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
            
            database = OdinDatabase(driver, appNotificationsAdapter, driveLocalTagIndexAdapter, driveMainIndexAdapter, driveTagIndexAdapter, keyValueAdapter)
            logger.i { "Database initialized successfully" }
        } else {
            logger.w { "Database already initialized" }
        }
    }

    fun getDatabase(): OdinDatabase {
        return database ?: throw IllegalStateException("Database not initialized. Call initialize() first.")
    }

    fun isInitialized(): Boolean = database != null
}
