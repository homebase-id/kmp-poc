package id.homebase.homebasekmppoc.prototype.lib.drives

import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import id.homebase.homebasekmppoc.prototype.lib.drives.query.DriveQueryProvider
import id.homebase.homebasekmppoc.prototype.lib.eventbus.EventBus
import id.homebase.homebasekmppoc.prototype.ui.driveFetch.DriveSync
import kotlinx.coroutines.*
import kotlin.uuid.Uuid

class DriveSyncManager(
    private val drives: List<DriveSync>,  // TODO: Todd <- or is this list a global singleton and not a parameter?
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
        drives: List<DriveSync>
    ) {
        syncAll(identityId, drives)
    }

    private fun syncAll(
        identityId: Uuid,
        drives: List<DriveSync>
    ) {
        // Any sync jobs created will be F&F
        for (drive in drives) {
            val job = drive.sync()
        }
    }

    fun cancelAll(drives: List<DriveSync>) {
        for (drive in drives) {
             drive.cancel() // Not active, see function
        }
    }
}
