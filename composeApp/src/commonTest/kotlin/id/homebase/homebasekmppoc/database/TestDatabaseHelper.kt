package id.homebase.homebasekmppoc.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Creates an in-memory test database
 * Platform-specific implementations provide appropriate SQLDelight drivers
 */
expect fun createInMemoryDatabase(): SqlDriver
