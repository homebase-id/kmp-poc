package id.homebase.homebasekmppoc.database

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.inMemoryDriver

/**
 * iOS test implementation using native SQLite driver with in-memory database
 */
actual fun createInMemoryDatabase(): OdinDatabase {
    // Use inMemoryDriver helper which properly configures NativeSqliteDriver
    // for in-memory testing. This still uses NativeSqliteDriver internally,
    // just like production, but with in-memory storage.
    val driver = inMemoryDriver(OdinDatabase.Schema)
    return OdinDatabase(driver)
}
