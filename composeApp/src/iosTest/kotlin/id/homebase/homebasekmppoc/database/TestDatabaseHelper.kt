package id.homebase.homebasekmppoc.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.JournalMode
import co.touchlab.sqliter.SynchronousFlag
import co.touchlab.sqliter.interop.Logger
import platform.Foundation.NSUUID


/**
 * iOS test implementation using native SQLite driver with in-memory database
 */
//actual fun createInMemoryDatabase(): OdinDatabase {
//    // Use inMemoryDriver helper which properly configures NativeSqliteDriver
//    // for in-memory testing. This still uses NativeSqliteDriver internally,
//    // just like production, but with in-memory storage.
//    val driver = inMemoryDriver(OdinDatabase.Schema,)
//    return OdinDatabase(driver)
//}

/**
 * Silent logger for tests to suppress SQLite console output
 */
private object SilentLogger : Logger {
    override val eActive: Boolean = false
    override val vActive: Boolean = false

    override fun trace(message: String) {}
    override fun vWrite(message: String) {}
    override fun eWrite(message: String, exception: Throwable?) {}
}

actual fun createInMemoryDatabase(): SqlDriver {
    val uuid = NSUUID.UUID().UUIDString
    val dbName = "test-$uuid.db"

    val driver = NativeSqliteDriver(
        schema = OdinDatabase.Schema,
        name = dbName,
        onConfiguration = { config ->
            config.copy(
                // 1. journalMode is a top-level property of config
                journalMode = JournalMode.WAL,

                // 2. extendedConfig holds the other settings
                extendedConfig = DatabaseConfiguration.Extended(
                    foreignKeyConstraints = true,
                    synchronousFlag = SynchronousFlag.NORMAL
                ),

                // 3. Disable logging to prevent excessive console output during constraint violations
                // SEB:NOTE - This is SUPER DUPER strange. This keeps the the IOS tests that deal with
                // constraint violations from crashing the test suite. Without this, the tests randomly crashes
                // the test runner. Adding this silent logger prevents that from happening.
                // It makes absolutely no sense why logging would cause that.
                // Alternative workaround is to skip running gradle test for ios. Look at file
                // run-all-tests.sh for an example of that.
                loggingConfig = DatabaseConfiguration.Logging(
                    logger = SilentLogger
                )
            )
        }
    )
    return driver
}
