package id.homebase.homebasekmppoc.prototype.ui.driveFetch

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.prototype.lib.core.time.UnixTimeUtc
import id.homebase.homebasekmppoc.prototype.lib.database.MainIndexMetaHelpers
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import id.homebase.homebasekmppoc.prototype.lib.drives.query.FileQueryParams
import id.homebase.homebasekmppoc.prototype.lib.drives.FileState
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchResponse
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchResultOptionsRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.query.DriveQueryProvider
import id.homebase.homebasekmppoc.prototype.lib.drives.query.QueryBatchCursor
import kotlin.time.measureTimedValue
import kotlinx.coroutines.sync.*
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlin.IllegalArgumentException

class OutboxSync(
    private val driveQueryProvider: DriveQueryProvider, // TODO: <- can we get rid of this?
    private val databaseManager: DatabaseManager
) {
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    //TODO: Consider having a (readable) "last modified" which holds the largest timestamp of last-modified

    init {
    }

    // The send() function spawns a thread when it acquires the lock.
    // Then send() returns true if it begins processing in a thread, and false if
    // another thread is already processing.
    // Then the call immediately knows if a worker thread has been spawned.
    //
    fun send(): Boolean {
        if (!mutex.tryLock()) {
            return false
        }
        scope.launch {
            try {
                outboxSend()
            } finally {
                mutex.unlock()
            }
        }
        return true
    }

    private suspend fun outboxSend() {
        var totalCount = 0

        EventBusFlow.emit(BackendEvent.OutboxUpdate.ProcessingStarted);

        while (true) {
            Logger.i("Popping Outbox")

            val checkOutStamp = UnixTimeUtc.now()
            val outboxRecord = databaseManager.outbox.checkout(checkOutStamp)

            if (outboxRecord == null) {
                Logger.i("No more items in outbox")
                EventBusFlow.emit(BackendEvent.OutboxUpdate.Completed(totalCount));
                break;
            }

            try {
                // We sent the item, send an event
                EventBusFlow.emit(BackendEvent.OutboxUpdate.Sending(outboxRecord.driveId, outboxRecord.fileId))
                Logger.i("Log the data from the outboxRecord here...")

                // Try to upload the item over the network (this will emit events)

                // if successful we remove it from the database
                databaseManager.outbox.deleteByRowId(outboxRecord.rowId)

                // We sent the item, send an event
                EventBusFlow.emit(BackendEvent.OutboxUpdate.Sent(outboxRecord.driveId, outboxRecord.fileId))
                totalCount++
            } catch (e: Exception) {
                Logger.e("Outbox sending failed", e)
                EventBusFlow.emit(BackendEvent.OutboxUpdate.Failed(e.message ?: "Unknown error"))
                throw e
            }
        }
    }
}