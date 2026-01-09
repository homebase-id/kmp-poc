package id.homebase.homebasekmppoc.prototype.ui.driveFetch

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.prototype.lib.core.time.UnixTimeUtc
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import id.homebase.homebasekmppoc.prototype.lib.drives.query.DriveQueryProvider
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.sync.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class OutboxSync(
    private val driveQueryProvider: DriveQueryProvider, // TODO: <- can we get rid of this?
    private val databaseManager: DatabaseManager
) {
    private val MAX_SENDING_THREADS = 3
    private val semaphore = Semaphore(MAX_SENDING_THREADS)
    private val activeThreads = atomic(0)
    private val totalSent = atomic(0)
    private val counterMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    //TODO: Consider having a (readable) "last modified" which holds the largest timestamp of last-modified

    // The send() function spawns a thread when it acquires the lock.
    // Then send() returns true if it begins processing in a thread, and false if
    // another thread is already processing.
    // Then the call immediately knows if a worker thread has been spawned.
    //
    suspend fun send(): Boolean {
        if (!semaphore.tryAcquire()) {
            return false
        }

        scope.launch {
            try {
                counterMutex.withLock {
                    if (activeThreads.incrementAndGet() == 1) {
                        EventBusFlow.emit(BackendEvent.OutboxUpdate.ProcessingStarted);
                    }
                }
                outboxSend()
            } finally {
                // After loop, check if this is the final thread
                counterMutex.withLock {
                    if (activeThreads.decrementAndGet() == 0) {
                        val n = totalSent.getAndSet(0)
                        EventBusFlow.emit(BackendEvent.OutboxUpdate.Completed(n))
                    }
                }
                semaphore.release()
            }
        }
        return true
    }

    private suspend fun outboxSend() {
        while (true) {
            Logger.i("Popping Outbox")

            val checkOutStamp = UnixTimeUtc.now()
            val outboxRecord = databaseManager.outbox.checkout(checkOutStamp)

            if (outboxRecord == null) {
                Logger.i("No more items in outbox")
                break;
            }

            // Doesn't matter if it's not fully thread safe, semaphore ultimate guard
            if (activeThreads.value < MAX_SENDING_THREADS)
                this.send() // Try to spawn a thread for parallel outbox processing

            try {
                // We sent the item, send an event
                EventBusFlow.emit(BackendEvent.OutboxUpdate.Sending(outboxRecord.driveId, outboxRecord.fileId))
                Logger.i("Log the data from the outboxRecord here...")

                // Try to upload the item over the network (this will emit events)

                // if successful we remove it from the database
                databaseManager.outbox.deleteByRowId(outboxRecord.rowId)

                // We sent the item, send an event
                EventBusFlow.emit(BackendEvent.OutboxUpdate.Sent(outboxRecord.driveId, outboxRecord.fileId))
                totalSent.incrementAndGet()
            } catch (e: Exception) {
                val n = 30*outboxRecord.checkOutCount
                Logger.e("Failed upload for ${outboxRecord.fileId}, retry in $n seconds (attempt ${outboxRecord.checkOutCount + 1})", e)
                databaseManager.outbox.checkInFailed(outboxRecord.checkOutStamp!!,
                    UnixTimeUtc.now().addSeconds(n.toLong()).seconds )
                EventBusFlow.emit(BackendEvent.OutboxUpdate.Failed(e.message ?: "Unknown error"))
            }
        }
    }
}