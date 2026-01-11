package id.homebase.homebasekmppoc.prototype.ui.driveFetch

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.prototype.lib.database.CursorStorage
import id.homebase.homebasekmppoc.prototype.lib.database.MainIndexMetaHelpers
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import id.homebase.homebasekmppoc.prototype.lib.drives.query.FileQueryParams
import id.homebase.homebasekmppoc.prototype.lib.drives.FileState
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchResponse
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchResultOptionsRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.query.DriveQueryProvider
import id.homebase.homebasekmppoc.prototype.lib.drives.query.QueryBatchCursor
import id.homebase.homebasekmppoc.prototype.lib.eventbus.BackendEvent
import id.homebase.homebasekmppoc.prototype.lib.eventbus.EventBus
import kotlin.time.measureTimedValue
import kotlinx.coroutines.sync.*
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking


class DriveSync(
    private val identityId: Uuid,
    private val driveId: Uuid,
    private val driveQueryProvider: DriveQueryProvider, // TODO: <- can we get rid of this?
    private val databaseManager: DatabaseManager,
    private val eventBus: EventBus
) {
    private var cursor: QueryBatchCursor?
    private val mutex = Mutex()
    private var batchSize = 50 // We begin with the smallest batch
    private var fileHeaderProcessor = MainIndexMetaHelpers.HomebaseFileProcessor(databaseManager)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    //TODO: Consider having a (readable) "last modified" which holds the largest timestamp of last-modified

    init {
        // Load cursor from database
        val cursorStorage = CursorStorage(databaseManager, driveId)
        cursor = cursorStorage.loadCursor()

        // temp hack
        runBlocking { testHack() }
    }

    suspend fun testHack()
    {
        // Temp hack, remove soon.
        DatabaseManager.appDb.driveMainIndex.deleteAll() // TODO: <-- don't delete all! :-)
        DatabaseManager.appDb.driveTagIndex.deleteAll() // TODO: <-- don't delete all! :-)
        DatabaseManager.appDb.driveLocalTagIndex.deleteAll() // TODO: <-- don't delete all! :-)
        DatabaseManager.appDb.keyValue.deleteByKey(driveId) // TODO: <-- don't delete the cursor
        val cursorStorage = CursorStorage(databaseManager, driveId)
        cursorStorage.deleteCursor();
        cursor = null
    }

    // I remain tempted to let the sync() function spawn a thread
    // when it acquires the lock. Then sync() should return true
    // if it begins syncing, and false if another thread is already
    // syncing. Then the call immediately knows what is going on.
    fun sync(): Boolean {
        if (!mutex.tryLock()) {
            return false
        }
        scope.launch {
            try {
                performSync()
            } finally {
                mutex.unlock()
            }
        }
        return true
    }

    private suspend fun performSync() {
        var totalCount = 0
        var queryBatchResponse: QueryBatchResponse? = null
        var keepGoing = true

        eventBus.emit(BackendEvent.DriveSyncEvent.Started(driveId))

        while (keepGoing) {
            Logger.i("Querying host for ${batchSize} rows")
            val request = QueryBatchRequest(
                queryParams = FileQueryParams(
                    fileState = listOf(FileState.Active) // <-- TODO: We want them all, not just "active"?
                ),
                resultOptionsRequest = QueryBatchResultOptionsRequest(
                    maxRecords = batchSize,
                    includeMetadataHeader = true,
                    cursorState = cursor?.toJson()
                )
            )

            var recordsRead = 0
            val durationMs = measureTimedValue {
                try {
                    queryBatchResponse = driveQueryProvider.queryBatch(driveId, request)

                    if (queryBatchResponse.cursorState != null)
                        cursor = QueryBatchCursor.fromJson(queryBatchResponse.cursorState)

                    val searchResults = queryBatchResponse.searchResults
                    if (searchResults.isNotEmpty()) {
                        recordsRead = searchResults.size
                        totalCount += recordsRead

                        // Run DB operation in background without waiting - fire and forget
                        scope.launch {
                            try {
                                val dbMs = measureTimedValue {
                                    fileHeaderProcessor.baseUpsertEntryZapZap(
                                        identityId = identityId,
                                        driveId = driveId,
                                        fileHeaders = searchResults,
                                        cursor = cursor
                                    )
                                }
                                Logger.i("DB insert time $dbMs for ${searchResults.size} rows")
                            } catch (e: Exception) {
                                Logger.e("DB upsert failed for batch: ${e.message}")
                            }
                        }

                        val latestModified = searchResults.last().fileMetadata.updated

                        eventBus.emit(
                            BackendEvent.DriveSyncEvent.BatchReceived(
                            driveId = driveId,
                            totalCount = totalCount,
                            batchCount = recordsRead,
                            latestModified = latestModified,
                            batchData = searchResults
                        ))
                    }

                    // TODO: The BE should return the moreRows boolean from QueryBatch.
                    keepGoing = searchResults.size >= batchSize
                } catch (e: Exception) {
                    eventBus.emit(BackendEvent.DriveSyncEvent.Failed(driveId, "Sync failed: ${e.message}"))
                    keepGoing = false
                }
            }

            if (recordsRead > 0) {
                val batchWas = batchSize
                val targetMs = 700L
                if (durationMs.duration.inWholeMilliseconds > 0) {
                    batchSize =
                        (batchSize.toLong() * targetMs / durationMs.duration.inWholeMilliseconds)
                            .toInt()
                            .coerceIn(50, 1000)
                }
                Logger.d("Batch size: $batchWas, took ${durationMs.duration.inWholeMilliseconds}ms, now adjusted to: $batchSize")
            }
        }

        eventBus.emit(BackendEvent.DriveSyncEvent.Completed(driveId, totalCount))
    }
}