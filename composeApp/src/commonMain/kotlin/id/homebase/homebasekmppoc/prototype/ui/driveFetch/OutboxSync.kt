package id.homebase.homebasekmppoc.prototype.ui.driveFetch

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.lib.database.Outbox
import id.homebase.homebasekmppoc.prototype.lib.core.time.UnixTimeUtc
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import id.homebase.homebasekmppoc.prototype.lib.eventbus.BackendEvent
import id.homebase.homebasekmppoc.prototype.lib.eventbus.EventBus
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.*

interface OutboxUploader {
    suspend fun upload(outboxRecord: Outbox, eventBus : EventBus): Unit
}

class OutboxSync(
    private val databaseManager: DatabaseManager,
    private val uploader: OutboxUploader,
    private val eventBus: EventBus,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val scope: CoroutineScope = CoroutineScope(dispatcher + SupervisorJob()))
{
    private val MAX_SENDING_THREADS = 3
    private val WAIT_INCREMENT_SECONDS = 30
    private val semaphore = Semaphore(MAX_SENDING_THREADS)
    private val activeThreads = atomic(0)
    private val totalSent = atomic(0)
    private val counterMutex = Mutex()

    // The send() function spawns a thread when it acquires the lock.
    // Then send() returns true if it begins processing in a thread, and false if
    // another thread is already processing.
    // Then the call immediately knows if a worker thread has been spawned.
    //
    suspend fun send(): Boolean {
        if (!semaphore.tryAcquire()) {
            return false
        }

        scope.launch(dispatcher) {
            try {
                counterMutex.withLock {
                    if (activeThreads.incrementAndGet() == 1) {
                        eventBus.emit(BackendEvent.OutboxEvent.Started)
                    }
                }
                outboxSend()
            } finally {
                // After loop, check if this is the final thread
                var nextSend : UnixTimeUtc? = null
                try {
                    counterMutex.withLock {
                        if (activeThreads.decrementAndGet() == 0) {
                            val n = totalSent.getAndSet(0)
                            nextSend = databaseManager.outbox.nextScheduled()
                            eventBus.emit(BackendEvent.OutboxEvent.Completed(n))
                        }
                    }
                }
                finally {
                    semaphore.release()
                }
                if (nextSend != null)
                {
                    val delay = nextSend!!.milliseconds - UnixTimeUtc.now().milliseconds
                    delay(delay) // Put the thread to sleep
                    send()
                }
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
                eventBus.emit(BackendEvent.OutboxEvent.ItemStarted(outboxRecord.driveId, outboxRecord.fileId))
                Logger.i("Log the data from the outboxRecord here...")

                uploader.upload(outboxRecord)

                // if successful we remove it from the database
                databaseManager.outbox.deleteByRowId(outboxRecord.rowId)

                // We sent the item, send an event
                eventBus.emit(BackendEvent.OutboxEvent.ItemCompleted(outboxRecord.driveId, outboxRecord.fileId))
                totalSent.incrementAndGet()
            } catch (e: Exception) {
                val n = WAIT_INCREMENT_SECONDS*outboxRecord.checkOutCount
                Logger.w("Failed upload for ${outboxRecord.fileId}, retry in $n seconds (attempt ${outboxRecord.checkOutCount + 1})", e)
                databaseManager.outbox.checkInFailed(outboxRecord.checkOutStamp!!,
                    UnixTimeUtc.now().addSeconds(n.toLong()).seconds )
                eventBus.emit(BackendEvent.OutboxEvent.Failed(e.message ?: "Unknown error"))
            }
        }
    }
}
