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
import id.homebase.homebasekmppoc.prototype.lib.drives.TargetDrive
import id.homebase.homebasekmppoc.prototype.lib.drives.query.DriveQueryProvider
import id.homebase.homebasekmppoc.prototype.lib.drives.query.QueryBatchCursor
import kotlin.time.measureTimedValue
import kotlinx.coroutines.sync.*
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking

// TODO: When we update main-index-meta we should PROBABLY ignore any item with incoming.modified < db.modified

class DriveSync(
    private val identityId: Uuid,
    private val driveId: Uuid,
    private val driveQueryProvider: DriveQueryProvider, // TODO: <- can we get rid of this?)
) {
    private var cursor: QueryBatchCursor?
    private val mutex = Mutex()
    private var batchSize = 50 // We begin with the smallest batch
    private var fileHeaderProcessor = MainIndexMetaHelpers.HomebaseFileProcessor()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    //TODO: Consider having a (readable) "last modified" which holds the largest timestamp of last-modified

    init {
        // Temp hack, remove soon.
        // Load cursor from database
        runBlocking { initialize() }
        val cursorStorage = CursorStorage(driveId)
        cursor = cursorStorage.loadCursor()
    }

    suspend fun initialize()
    {
        // Temp hack, remove soon.
        // Load cursor from database

        DatabaseManager.driveMainIndex.deleteAll() // TODO: <-- don't delete all! :-)
        DatabaseManager.driveTagIndex.deleteAll() // TODO: <-- don't delete all! :-)
        DatabaseManager.driveLocalTagIndex.deleteAll() // TODO: <-- don't delete all! :-)
        DatabaseManager.keyValue.deleteByKey(driveId) // TODO: <-- don't delete the cursor
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

        EventBusFlow.emit(BackendEvent.SyncUpdate.SyncStarted(driveId));

        while (keepGoing) {
            val request = QueryBatchRequest(
                queryParams = FileQueryParams(
                    fileState = listOf(FileState.Active)
                ),
                resultOptionsRequest = QueryBatchResultOptionsRequest(
                    maxRecords = batchSize,
                    includeMetadataHeader = true,
                    cursorState = cursor?.toJson()
                )
            )

            val durationMs = measureTimedValue {
                try {
                    queryBatchResponse = driveQueryProvider.queryBatch(driveId, request)

                    if (queryBatchResponse.cursorState != null)
                        cursor = QueryBatchCursor.fromJson(queryBatchResponse.cursorState)

                    val searchResults = queryBatchResponse.searchResults
                    if (searchResults.isNotEmpty()) {
                        val batchCount = searchResults.size
                        totalCount += batchCount

                        // Run DB operation in background without waiting - fire and forget
                        CoroutineScope(Dispatchers.Default).launch {
                            try {
                                val dbMs = measureTimedValue {
                                    fileHeaderProcessor.baseUpsertEntryZapZap(
                                        identityId = identityId,
                                        driveId = driveId,
                                        fileHeaders = searchResults,
                                        cursor = cursor
                                    )
                                }
                                Logger.i("DB insert time ${dbMs} for ${searchResults.size} rows")
                            } catch (e: Exception) {
                                Logger.e("DB upsert failed for batch: ${e.message}")
                            }
                        }

                        val latestModified = searchResults.last().fileMetadata.updated

                        EventBusFlow.emit(BackendEvent.SyncUpdate.BatchReceived(
                            driveId = driveId,
                            totalCount = totalCount,
                            batchCount = batchCount,
                            latestModified = latestModified,
                            batchData = searchResults
                        ))
                    }

                    keepGoing = searchResults?.let { it.size >= batchSize } ?: false
                } catch (e: Exception) {
                    EventBusFlow.emit(BackendEvent.SyncUpdate.Failed(driveId, "Sync failed: ${e.message}"))
                    keepGoing = false
                }
            }

            val batchWas = batchSize
            val targetMs = 700L
            if (durationMs.duration.inWholeMilliseconds > 0) {
                batchSize = (batchSize.toLong() * targetMs / durationMs.duration.inWholeMilliseconds)
                    .toInt()
                    .coerceIn(50, 1000)
            }
            Logger.d("Batch size: $batchWas, took ${durationMs.duration.inWholeMilliseconds}ms, now adjusted to: $batchSize")
        }

        EventBusFlow.emit(BackendEvent.SyncUpdate.Completed(driveId, totalCount))
    }
}