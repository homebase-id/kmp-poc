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
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
)
{
    /**
     * Called when websocket reports "connected"
     * Fire-and-forget safe.
     */
    fun onConnected(
        identityId: Uuid,
        drives: List<TargetDrive>
    ) {
        syncAll(identityId, drives)
    }

    private fun syncAll(
        identityId: Uuid,
        drives: List<TargetDrive>
    ) {
        for (drive in drives) {
            val job = DriveSync(
                identityId = identityId,
                driveId = drive.alias,
                driveQueryProvider = driveQueryProvider,
                databaseManager = databaseManager,
                eventBus = eventBus
            ).sync()

            // Later, if we want to keep track of running jobs, we should
            // push job onto a (thread safe?) stack if job is not null.
        }
    }

    fun cancelAll() {
        // If at a later time we want to cancel running syncs then
        // we need a thread safe stack of running jobs, and we need to
        // somehow pop jobs when they are completed.
    }
}
