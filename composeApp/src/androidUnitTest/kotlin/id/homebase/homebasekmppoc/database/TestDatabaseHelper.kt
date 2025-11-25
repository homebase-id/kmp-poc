package id.homebase.homebasekmppoc.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

/**
 * Android test implementation using SQLite JDBC driver
 */
actual fun createInMemoryDatabase(): OdinDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    OdinDatabase.Schema.create(driver)
    return OdinDatabase(driver)
}
