package id.homebase.homebasekmppoc.prototype.ui.driveFetch

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.lib.database.OdinDatabase
import id.homebase.homebasekmppoc.prototype.lib.database.CursorStorage
import id.homebase.homebasekmppoc.prototype.lib.database.FileHeaderProcessor
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

// TODO: When we update main-index-meta we should PROBABLY ignore any item with incoming.modified < db.modified
// TODO: Make a callback with memory List<> when we got data from both HERE && Websocket

class DriveSync(
    private val identityId: Uuid,
    private val driveId: Uuid,
    private val driveQueryProvider: DriveQueryProvider, // TODO: <- can we get rid of this?
    private val database: OdinDatabase
) {
    private var cursor: QueryBatchCursor?
    private val mutex = Mutex()
    private var batchSize = 50 // We begin with the smallest batch
    private var fileHeaderProcessor = FileHeaderProcessor(database)

    //TODO: Consider having a (readable) "last modified" which holds the largest timestamp of last-modified

    init {
        database.driveMainIndexQueries.deleteAll() // TODO: <-- don't delete all! :-)
        database.driveTagIndexQueries.deleteAll() // TODO: <-- don't delete all! :-)
        database.driveLocalTagIndexQueries.deleteAll() // TODO: <-- don't delete all! :-)
        database.keyValueQueries.deleteByKey(driveId) // TODO: <-- don't delete the cursor

        // Load cursor from database
        val cursorStorage = CursorStorage(database, driveId)
        cursor = cursorStorage.loadCursor()
    }

    suspend fun sync(
        onProgressUX: (fetchedCount: Int) -> Unit = { _ -> }
    ): QueryBatchResponse? {
        if (mutex.tryLock() == false) {
            // -1 means another thread is already syncing
            onProgressUX(-1)
            return null
        } else {
            //
            // NEXT: Make local QueryBatch algo and have the FE use it
            //

            // TODO: Consider spawning this set of work as a thread ... but might be fragile with the Mutex
            try {
                var totalCount = 0
                var queryBatchResponse: QueryBatchResponse? = null
                var keepGoing = true

                while (keepGoing) {
                    val request =
                        QueryBatchRequest(
                            queryParams = FileQueryParams(fileState = listOf(FileState.Active)),
                            resultOptionsRequest =
                                QueryBatchResultOptionsRequest(
                                    maxRecords = batchSize,
                                    includeMetadataHeader = true,
                                    cursorState = cursor?.toJson()
                                )
                        )

                    val durationMs = measureTimedValue {
                        queryBatchResponse = driveQueryProvider.queryBatch(driveId, request)

                        if (queryBatchResponse?.cursorState != null)
                            cursor = QueryBatchCursor.fromJson(queryBatchResponse.cursorState)

                        if (queryBatchResponse?.searchResults?.isNotEmpty() == true) {
                            totalCount += queryBatchResponse!!.searchResults.size

                            // TODO: Consider commiting every NNNN rows or SS seconds - but also consider maybe it's good for the FE to get data faster?
                            fileHeaderProcessor.BaseUpsertEntryZapZap(
                                identityId = identityId,
                                driveId = driveId,
                                fileHeaders = queryBatchResponse.searchResults,
                                cursor = cursor
                            )

                            // UX callback after we submit to the DB because then the UX can choose to query
                            onProgressUX(totalCount)
                        }

                        keepGoing =
                            queryBatchResponse?.searchResults?.let { it.size >= batchSize }
                                ?: false
                    }

                    // Adaptive package size
                    if ((durationMs.duration.inWholeMilliseconds < 300) && (batchSize < 1000)) {
                        batchSize = (1 + batchSize * 1.5).toInt().coerceAtMost(1000)
                    } else if ((durationMs.duration.inWholeMilliseconds > 800) && (batchSize > 50)
                    ) {
                        batchSize = (batchSize * 0.7).toInt().coerceAtLeast(50)
                    }

                    // Optional: log for debugging
                    Logger.d(
                        "Batch size: $batchSize, took ${durationMs.duration.inWholeMilliseconds}ms"
                    )
                }

                // TODO: We need a way to communitcate "done", for now it is 9,999,999
                onProgressUX(9999999)

                // TODO: Remove return type, add function for FE to queryBatch from local SQLite
                return queryBatchResponse
            } finally {
                mutex.unlock()  // Always unlock if we acquired it
            }
        }
    }
}