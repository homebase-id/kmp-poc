package id.homebase.homebasekmppoc.database

import co.touchlab.kermit.Logger

object DatabaseManager {
    private var database: OdinDatabase? = null
    private val logger = Logger.withTag("DatabaseManager")

    fun initialize(driverFactory: DatabaseDriverFactory) {
        if (database == null) {
            logger.i { "Initializing database..." }
            val driver = driverFactory.createDriver()
            database = OdinDatabase(driver)
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
