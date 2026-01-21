package id.homebase.homebasekmppoc.prototype.lib.drives

import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import id.homebase.homebasekmppoc.prototype.lib.drives.query.DriveQueryProvider
import id.homebase.homebasekmppoc.prototype.lib.eventbus.EventBus
import id.homebase.homebasekmppoc.prototype.ui.driveFetch.DriveSync
import kotlinx.coroutines.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class DriveSyncManager(
    private val driveQueryProvider: DriveQueryProvider,
    private val databaseManager: DatabaseManager,
    private val eventBus: EventBus,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val debounceWindow: Duration = 1.seconds
) {

    private val activeSyncs = mutableMapOf<Uuid, Job>()
    private var debounceJob: Job? = null

    /**
     * Called when websocket reports "connected"
     * Fire-and-forget safe.
     */
    fun onConnected(
        identityId: Uuid,
        drives: List<TargetDrive>
    ) {
        // Debounce reconnect storms
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(debounceWindow)
            syncAll(identityId, drives)
        }
    }

    private fun syncAll(
        identityId: Uuid,
        drives: List<TargetDrive>
    ) {
        for (drive in drives) {
            val driveId = drive.alias
            // Prevent overlapping syncs per drive
            if (activeSyncs[driveId]?.isActive == true) continue

            val job = scope.launch {
                try {
                    DriveSync(
                        identityId = identityId,
                        driveId = driveId,
                        driveQueryProvider = driveQueryProvider,
                        databaseManager = databaseManager,
                        eventBus = eventBus
                    ).sync()
                } finally {
                    activeSyncs.remove(driveId)
                }
            }

            activeSyncs[driveId] = job
        }
    }

    fun cancelAll() {
        debounceJob?.cancel()
        activeSyncs.values.forEach { it.cancel() }
        activeSyncs.clear()
    }
}
