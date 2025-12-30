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

// TODO: When we update main-index-meta we should PROBABLY ignore any item with incoming.modified < db.modified

class DriveSync(private val identityId : Uuid,
                private val targetDrive: TargetDrive, // TODO: <- change to driveId
                private val driveQueryProvider: DriveQueryProvider, // TODO: <- can we get rid of this?)
)
{
    private var cursor: QueryBatchCursor?
    private val mutex = Mutex()
    private var batchSize = 50 // We begin with the smallest batch
    private lateinit var fileHeaderProcessor: MainIndexMetaHelpers.HomebaseFileProcessor
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    //TODO: Consider having a (readable) "last modified" which holds the largest timestamp of last-modified

    init {
        fileHeaderProcessor = MainIndexMetaHelpers.HomebaseFileProcessor(DatabaseManager)

        // Temp hack, remove soon.
        val database = DatabaseManager.getDatabase()
        database.driveMainIndexQueries.deleteAll() // TODO: <-- don't delete all! :-)
        database.driveTagIndexQueries.deleteAll() // TODO: <-- don't delete all! :-)
        database.driveLocalTagIndexQueries.deleteAll() // TODO: <-- don't delete all! :-)
        database.keyValueQueries.deleteByKey(targetDrive.alias) // TODO: <-- don't delete the cursor

        // Load cursor from database
        val cursorStorage = CursorStorage(database, targetDrive.alias)
        cursor = cursorStorage.loadCursor()
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
                var totalCount = 0
                var queryBatchResponse: QueryBatchResponse? = null
                var keepGoing = true

                EventBusFlow.emit(BackendEvent.SyncUpdate.SyncStarted(targetDrive.alias));

                while (keepGoing) {
                    val request = QueryBatchRequest(
                        queryParams = FileQueryParams(
                            targetDrive = targetDrive,
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
                            queryBatchResponse = driveQueryProvider.queryBatch(request)

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
                                                driveId = targetDrive.alias,
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
                                    driveId = targetDrive.alias,
                                    totalCount = totalCount,
                                    batchCount = batchCount,
                                    latestModified = latestModified,
                                    batchData = searchResults
                                ))
                            }

                            keepGoing = searchResults?.let { it.size >= batchSize } ?: false
                        } catch (e: Exception) {
                            EventBusFlow.emit(BackendEvent.SyncUpdate.Failed(targetDrive.alias, "Sync failed: ${e.message}"))
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

                EventBusFlow.emit(BackendEvent.SyncUpdate.Completed(targetDrive.alias, totalCount))
            } finally {
                mutex.unlock()
            }
        }
        return true
    }
}