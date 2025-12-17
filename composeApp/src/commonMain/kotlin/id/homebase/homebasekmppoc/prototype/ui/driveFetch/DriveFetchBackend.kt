package id.homebase.homebasekmppoc.prototype.ui.driveFetch

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.lib.database.OdinDatabase
import id.homebase.homebasekmppoc.prototype.lib.database.CursorStorage
import id.homebase.homebasekmppoc.prototype.lib.drives.FileQueryParams
import id.homebase.homebasekmppoc.prototype.lib.drives.FileState
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchResponse
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchResultOptionsRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.TargetDrive
import id.homebase.homebasekmppoc.prototype.lib.drives.query.DriveQueryProvider
import id.homebase.homebasekmppoc.prototype.lib.drives.query.QueryBatchCursor
import kotlin.time.measureTimedValue

// TODO: TargetDrive should really just be a uuid driveId, but we need to update the QueryBatchRequest
class DriveFetchBackend(private val driveQueryProvider: DriveQueryProvider,
                        private val targetDrive: TargetDrive,
                        private val database: OdinDatabase)
{
    private var cursor: String? = null

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
        var totalCount = 0
        var batchSize = 50
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

                    // Create FileMetadataProcessor instance to test BaseUpsertEntryZapZap
                    // Call BaseUpsertEntryZapZap function mainIndexMeta.kt line 127

                    //                            processor.BaseUpsertEntryZapZap(
                    //                                identityId = identityId,
                    //                                driveId = driveId,
                    //                                fileHeader =
                    // queryBatchResponse.searchResults,
                    //                                cursor = cursor
                    //                            )
                    //
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
}