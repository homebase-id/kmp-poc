package id.homebase.homebasekmppoc.prototype.lib.database

import app.cash.sqldelight.db.SqlDriver
import id.homebase.homebasekmppoc.lib.database.AppNotifications
import id.homebase.homebasekmppoc.lib.database.DriveLocalTagIndex
import id.homebase.homebasekmppoc.lib.database.DriveMainIndex
import id.homebase.homebasekmppoc.lib.database.DriveTagIndex
import id.homebase.homebasekmppoc.lib.database.KeyValue
import id.homebase.homebasekmppoc.lib.database.OdinDatabase

/**
 * Factory for creating test databases with all necessary adapters pre-configured.
 * Centralizes adapter definitions to avoid duplication across test files.
 */
object TestDatabaseFactory {
    
    // Shared adapters - defined once here and reused across all tests
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

    private val driveMainIndexAdapter = DriveMainIndex.Adapter(
        identityIdAdapter = UuidAdapter,
        driveIdAdapter = UuidAdapter,
        fileIdAdapter = UuidAdapter,
        globalTransitIdAdapter = UuidAdapter,
        groupIdAdapter = UuidAdapter,
        uniqueIdAdapter = UuidAdapter
    )

    private val keyValueAdapter = KeyValue.Adapter(
        keyAdapter = UuidAdapter
    )

    private val appNotificationsAdapter = AppNotifications.Adapter(
        identityIdAdapter = UuidAdapter,
        notificationIdAdapter = UuidAdapter
    )

    /**
     * Creates a test database with all adapters pre-configured.
     * Uses the platform-specific in-memory driver.
     * 
     * @param driver Optional custom SQL driver. If null, uses in-memory database.
     * @return Configured OdinDatabase instance ready for testing
     */
    fun createTestDatabase(driver: SqlDriver? = null): OdinDatabase {
        val sqlDriver = driver ?: createInMemoryDatabase()
        
        return OdinDatabase.Companion(
            sqlDriver,
            appNotificationsAdapter,
            driveLocalTagIndexAdapter,
            driveMainIndexAdapter,
            driveTagIndexAdapter,
            keyValueAdapter
        )
    }
}