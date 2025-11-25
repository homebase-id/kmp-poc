package id.homebase.homebasekmppoc.database

/**
 * Creates an in-memory test database
 * Platform-specific implementations provide appropriate SQLDelight drivers
 */
expect fun createInMemoryDatabase(): OdinDatabase
