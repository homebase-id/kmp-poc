package id.homebase.homebasekmppoc.prototype.ui.driveFetch

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.lib.database.OdinDatabase
import id.homebase.homebasekmppoc.prototype.lib.database.CursorStorage
import id.homebase.homebasekmppoc.prototype.lib.database.FileHeaderProcessor
import id.homebase.homebasekmppoc.prototype.lib.drives.FileQueryParams
import id.homebase.homebasekmppoc.prototype.lib.drives.FileState
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchResponse
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchResultOptionsRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.TargetDrive
import id.homebase.homebasekmppoc.prototype.lib.drives.query.DriveQueryProvider
import id.homebase.homebasekmppoc.prototype.lib.drives.query.QueryBatchCursor
import kotlin.time.measureTimedValue
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.*
import kotlin.uuid.Uuid


// TODO: TargetDrive should really just be a uuid driveId, but we need to update the QueryBatchRequest
class DriveSync(private val identityId : Uuid,
                private val targetDrive: TargetDrive,
                private val driveQueryProvider: DriveQueryProvider, // TODO: <- can we get rid of this?
                private val database: OdinDatabase)
{
    private var cursor: String? = null
    private val mutex = Mutex()
    private var batchSize = 50 // We begin with the smallest batch
    private var fileHeaderProcessor = FileHeaderProcessor(database)


    init {
        // Add your constructor logic here
        // For example:
        Logger.d("DriveFetchBackend initialized with targetDrive: $targetDrive")

        // Load cursor from database
        val cursorStorage = CursorStorage(database, targetDrive.alias)
        val loadedCursor: QueryBatchCursor? = cursorStorage.loadCursor()
        cursor = loadedCursor?.toJson()
    }

    suspend fun fetchFiles(
        onProgressUX: (fetchedCount: Int) -> Unit = { _ -> }): QueryBatchResponse?
    {
        if (mutex.tryLock() == false)
        {
            // -1 means another thread is already syncing
            onProgressUX(-1)
            return null
        }
        else
        {
            // Mutex locked

            try
            {
                var totalCount = 0
                var queryBatchResponse: QueryBatchResponse? = null
                var keepGoing = true

                while (keepGoing) {
                    val request =
                        QueryBatchRequest(
                            queryParams =
                                FileQueryParams(
                                    targetDrive = targetDrive,
                                    fileState = listOf(FileState.Active)
                                ),
                            resultOptionsRequest =
                                QueryBatchResultOptionsRequest(
                                    maxRecords = batchSize,
                                    includeMetadataHeader = true,
                                    cursorState = cursor
                                )
                        )

                    val durationMs = measureTimedValue {
                        queryBatchResponse = driveQueryProvider.queryBatch(request)

                        if (queryBatchResponse?.cursorState != null)
                            cursor = queryBatchResponse?.cursorState

                        if (queryBatchResponse?.searchResults?.isNotEmpty() == true) {
                            totalCount += queryBatchResponse!!.searchResults.size

                            // UX callback
                            onProgressUX(totalCount)

                            // fileHeaderProcessor.BaseUpsertEntryZapZap(Uuid.random(), targetDrive.alias, queryBatchResponse.searchResults, cursor)
                        }

                        keepGoing =
                            queryBatchResponse?.searchResults?.let { it.size >= batchSize }
                                ?: false
                    }

                    // Adaptive package size
                    if ((durationMs.duration.inWholeMilliseconds < 300) && (batchSize < 1000)) {
                        batchSize =  (1 + batchSize * 1.5).toInt().coerceAtMost(1000)
                    } else if ((durationMs.duration.inWholeMilliseconds > 800) && (batchSize > 50)
                    ) {
                        batchSize = (batchSize * 0.7).toInt().coerceAtLeast(50)
                    }

                    // Optional: log for debugging
                    Logger.d(
                        "Batch size: $batchSize, took ${durationMs.duration.inWholeMilliseconds}ms"
                    )
                }

                return queryBatchResponse
            }
            finally
            {
                mutex.unlock()  // Always unlock if we acquired it
            }
        }
    }
}