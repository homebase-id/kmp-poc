package id.homebase.homebasekmppoc.prototype.lib.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import id.homebase.homebasekmppoc.lib.database.OdinDatabase

/**
 * Desktop test implementation using SQLite JDBC driver
 */
actual fun createInMemoryDatabase(): SqlDriver {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    OdinDatabase.Companion.Schema.create(driver)
    return driver
}