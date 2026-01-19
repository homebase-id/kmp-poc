package id.homebase.homebasekmppoc.prototype.ui.driveFetch

import androidx.compose.material.rememberDrawerState
import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.prototype.lib.core.SecureByteArray
import id.homebase.homebasekmppoc.prototype.lib.crypto.KeyHeader
import id.homebase.homebasekmppoc.prototype.lib.database.MainIndexMetaHelpers
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import id.homebase.homebasekmppoc.prototype.lib.database.createInMemoryDatabase
import id.homebase.homebasekmppoc.prototype.lib.drives.FileState
import id.homebase.homebasekmppoc.prototype.lib.drives.FileSystemType
import id.homebase.homebasekmppoc.prototype.lib.drives.ServerMetadata
import id.homebase.homebasekmppoc.prototype.lib.drives.HomebaseFile
import id.homebase.homebasekmppoc.prototype.lib.drives.files.ArchivalStatus
import id.homebase.homebasekmppoc.prototype.lib.drives.files.AppFileMetaData
import id.homebase.homebasekmppoc.prototype.lib.drives.files.FileMetadata
import id.homebase.homebasekmppoc.prototype.lib.drives.files.LocalAppMetadata
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/**
 * Comprehensive thread safety test for DriveSync database operations.
 *
 * This test spawns multiple competing threads to ensure that concurrent database
 * writes are properly handled and maintain data integrity.
 */
open class DriveSyncThreadSafetyTest {
    @Test
    fun testConcurrentDatabaseWrites_threadSafety() = runTest {
        DatabaseManager { createInMemoryDatabase() }.use { dbm ->
            val fileHeaderProcessor = MainIndexMetaHelpers.HomebaseFileProcessor(dbm)
            val identityId = Uuid.random()
            val driveId = Uuid.random()
            // Test parameters
            val threadCount = 100
            val rowsPerThread = 10
            val totalExpectedRows = threadCount * rowsPerThread

            Logger.i("Starting concurrent database test with $threadCount threads, $rowsPerThread rows each")

            // Generate all test data first
            val allHeaders = mutableListOf<HomebaseFile>()
            repeat(threadCount) { threadIndex ->
                repeat(rowsPerThread) { rowIndex ->
                    allHeaders.add(createTestFileHeader(threadIndex, rowIndex))
                }
            }

            // Track completion
            val completionMutex = Mutex()
            var completedThreads = 0
            // Spawn multiple competing threads to process data concurrently
            // Each thread will use DatabaseManager.withWriteTransaction independently
            val jobs = List(threadCount) { threadIndex ->
                CoroutineScope(Dispatchers.Default).async {
                    try {
                        // Each thread processes its portion of headers
                        val startIndex = threadIndex * rowsPerThread
                        val endIndex = (threadIndex + 1) * rowsPerThread
                        val threadHeaders = allHeaders.subList(startIndex, endIndex)

                        // Use direct database operations (no additional transaction)
                        fileHeaderProcessor.performBaseUpsert(
                            identityId = identityId,
                            driveId = driveId,
                            fileHeaders = threadHeaders,
                            cursor = null
                        )

                        completionMutex.withLock {
                            completedThreads++
                            Logger.d("Thread $threadIndex completed with ${threadHeaders.size} rows")
                        }

                    } catch (e: Exception) {
                        Logger.e("Thread $threadIndex failed: ${e.message}")
                        throw e
                    }
                }
            }

            // Wait for all threads to complete
            runBlocking {
                jobs.awaitAll()
            }

            // Verify all threads completed successfully
            assertEquals(threadCount, completedThreads, "All threads should have completed")

            // Verify database integrity
            val actualRowCount = dbm.driveMainIndex.countAll()
            assertEquals(
                totalExpectedRows.toLong(), actualRowCount,
                "Database should contain exactly $totalExpectedRows rows, but has $actualRowCount"
            )

            // Verify no duplicate rows (should be unique file IDs)
            val allDbRecords = dbm.driveMainIndex.selectAll()
            val duplicateFileIds = allDbRecords
                .groupBy { it.fileId }
                .filter { it.value.size > 1 }
                .keys

            assertTrue(
                duplicateFileIds.isEmpty(),
                "No duplicate file IDs should exist. Duplicates: $duplicateFileIds"
            )

            Logger.i("Thread safety test completed successfully:")
            Logger.i("- Threads: $threadCount")
            Logger.i("- Rows per thread: $rowsPerThread")
            Logger.i("- Total expected rows: $totalExpectedRows")
            Logger.i("- Actual database rows: $actualRowCount")
            Logger.i("- Unique file IDs: ${allDbRecords.size}")
        }
    }


    private fun createTestFileHeader(threadIndex: Int, rowIndex: Int): HomebaseFile {
        val now = Clock.System.now()
        val uniqueId = Uuid.random()
        val driveId = Uuid.random()

        return HomebaseFile(
            fileId = uniqueId,
            driveId = driveId,
            fileState = FileState.Active,
            fileSystemType = FileSystemType.Standard,
            keyHeader = KeyHeader(
                iv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12),
                aesKey = SecureByteArray(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))
            ),
            fileMetadata = FileMetadata(
                created = id.homebase.homebasekmppoc.prototype.lib.core.time.UnixTimeUtc(now),
                updated = id.homebase.homebasekmppoc.prototype.lib.core.time.UnixTimeUtc(now),
                senderOdinId = "sender-$threadIndex-$rowIndex",
                originalAuthor = "author-$threadIndex-$rowIndex",
                appData = AppFileMetaData(
                    tags = listOf(Uuid.random(), Uuid.random()),
                    fileType = threadIndex % 10,
                    dataType = rowIndex % 5,
                    uniqueId = uniqueId,
                    groupId = Uuid.random(),
                    userDate = now.epochSeconds,
                    archivalStatus = ArchivalStatus.None
                ),
                localAppData = LocalAppMetadata(
                    tags = listOf(Uuid.random())
                )
            ),
            serverMetadata = ServerMetadata(
                fileSystemType = FileSystemType.Standard,
                fileByteCount = 1024L * (rowIndex + 1)
            )
        )
    }
}