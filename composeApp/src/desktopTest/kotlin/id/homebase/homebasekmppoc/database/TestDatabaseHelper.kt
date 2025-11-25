package id.homebase.homebasekmppoc.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

/**
 * Helper class for creating test databases
 * This uses JDBC which is available on the JVM for testing
 */
object TestDatabaseHelper {
    
    fun createInMemoryDatabase(): OdinDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        OdinDatabase.Schema.create(driver)
        return OdinDatabase(driver)
    }
}