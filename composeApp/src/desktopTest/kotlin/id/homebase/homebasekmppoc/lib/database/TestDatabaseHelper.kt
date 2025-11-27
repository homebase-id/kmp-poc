package id.homebase.homebasekmppoc.lib.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

/**
 * Desktop test implementation using SQLite JDBC driver
 */
actual fun createInMemoryDatabase(): SqlDriver {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    OdinDatabase.Schema.create(driver)
    return driver
}