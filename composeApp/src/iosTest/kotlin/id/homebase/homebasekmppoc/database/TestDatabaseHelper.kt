package id.homebase.homebasekmppoc.database

import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS test implementation using native SQLite driver
 */
actual fun createInMemoryDatabase(): OdinDatabase {
    val driver = NativeSqliteDriver(OdinDatabase.Schema, "test.db")
    return OdinDatabase(driver)
}
