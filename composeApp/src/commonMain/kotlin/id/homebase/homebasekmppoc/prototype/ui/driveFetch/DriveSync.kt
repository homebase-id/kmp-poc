package id.homebase.homebasekmppoc.prototype.ui.driveFetch

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.lib.database.OdinDatabase
import id.homebase.homebasekmppoc.prototype.lib.core.time.UnixTimeUtc
import id.homebase.homebasekmppoc.prototype.lib.database.CursorStorage
import id.homebase.homebasekmppoc.prototype.lib.database.FileHeaderProcessor
import id.homebase.homebasekmppoc.prototype.lib.drives.query.FileQueryParams
import id.homebase.homebasekmppoc.prototype.lib.drives.FileState
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchResponse
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchResultOptionsRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.SharedSecretEncryptedFileHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.TargetDrive
import id.homebase.homebasekmppoc.prototype.lib.drives.query.DriveQueryProvider
import id.homebase.homebasekmppoc.prototype.lib.drives.query.QueryBatchCursor
import kotlin.time.measureTimedValue
import kotlinx.coroutines.sync.*
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

sealed interface SyncProgress {
    data class InProgress(
        val totalCount: Int,
        val batchCount: Int,
        val latestModified: UnixTimeUtc?,
        val batchData: List<SharedSecretEncryptedFileHeader>
    ) : SyncProgress

    data class Completed(val totalCount: Int) : SyncProgress
    data class Failed(val errorMessage: String) : SyncProgress  // Or val throwable: Throwable for more detail
}

// TODO: When we update main-index-meta we should PROBABLY ignore any item with incoming.modified < db.modified
// TODO: Make a callback with memory List<> when we got data from both HERE && Websocket

class DriveSync(private val identityId : Uuid,
                private val targetDrive: TargetDrive, // TODO: <- change to driveId
                private val driveQueryProvider: DriveQueryProvider, // TODO: <- can we get rid of this?
                private val database: OdinDatabase)
{
    private var cursor: QueryBatchCursor?
    private val mutex = Mutex()
    private var batchSize = 50 // We begin with the smallest batch
    private var fileHeaderProcessor = FileHeaderProcessor(database)

    //TODO: Consider having a (readable) "last modified" which holds the largest timestamp of last-modified

    init {
        database.driveMainIndexQueries.deleteAll() // TODO: <-- don't delete all! :-)
        database.driveTagIndexQueries.deleteAll() // TODO: <-- don't delete all! :-)
        database.driveLocalTagIndexQueries.deleteAll() // TODO: <-- don't delete all! :-)
        database.keyValueQueries.deleteByKey(targetDrive.alias) // TODO: <-- don't delete the cursor

        // Load cursor from database
        val cursorStorage = CursorStorage(database, targetDrive.alias)
        cursor = cursorStorage.loadCursor()
    }

    suspend fun sync(): Flow<SyncProgress> = flow {
        if (!mutex.tryLock()) {
            emit(SyncProgress.Failed("Another sync already in progress"))
            return@flow
        }
        try {
            var totalCount = 0
            var queryBatchResponse: QueryBatchResponse? = null
            var keepGoing = true

            while (keepGoing)
            {
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

                        if (queryBatchResponse?.cursorState != null)
                            cursor = QueryBatchCursor.fromJson(queryBatchResponse.cursorState)

                        val searchResults = queryBatchResponse?.searchResults
                        if (searchResults?.isNotEmpty() == true) {
                            val batchCount = searchResults.size
                            totalCount += batchCount

                            // TODO: DECOUPLE THIS SO WE'RE NOT WAITING FOR THE DB TO SAVE
                            fileHeaderProcessor.BaseUpsertEntryZapZap(
                                identityId = identityId,
                                driveId = targetDrive.alias,
                                fileHeaders = searchResults,
                                cursor = cursor
                            )

                            // Calculate latest modified (assuming FileHeader has a 'modified: Long' field)
                            val latestModified = searchResults.last().fileMetadata.updated

                            emit(SyncProgress.InProgress(
                                totalCount = totalCount,
                                batchCount = batchCount,
                                latestModified = latestModified,
                                batchData = searchResults
                            ))
                        }

                        keepGoing = searchResults?.let { it.size >= batchSize } ?: false
                    } catch (e: Exception) {
                        emit(SyncProgress.Failed("Sync failed: ${e.message}"))
                        keepGoing = false  // Stop on error
                    }
                }

                // Adaptive batch size logic (unchanged)
//                if (durationMs.duration.inWholeMilliseconds < 600 && batchSize < 1000) {
//                    batchSize = (1 + batchSize * 1.5).toInt().coerceAtMost(1000)
//                } else if (durationMs.duration.inWholeMilliseconds > 800 && batchSize > 50) {
//                    batchSize = (batchSize * 0.7).toInt().coerceAtLeast(50)
//                }

                val batchWas = batchSize
                val targetMs = 700L  // Target time per batch; adjust if needed (e.g., 500L for more aggressive)
                if (durationMs.duration.inWholeMilliseconds > 0) {  // Avoid divide-by-zero, though unlikely
                    batchSize = (batchSize.toLong() * targetMs / durationMs.duration.inWholeMilliseconds)
                        .toInt()
                        .coerceIn(50, 1000)
                }

                Logger.d("Batch size: $batchWas, took ${durationMs.duration.inWholeMilliseconds}ms, now adjusted to: $batchSize")
            }

            emit(SyncProgress.Completed(totalCount))
        } finally {
            mutex.unlock()
        }
    }.flowOn(Dispatchers.Default)
}