package id.homebase.homebasekmppoc.prototype.lib.authentication

import id.homebase.homebasekmppoc.lib.config.chatTargetDrive
import id.homebase.homebasekmppoc.lib.youauth.YouAuthState
import id.homebase.homebasekmppoc.prototype.lib.base.CredentialsManager
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import id.homebase.homebasekmppoc.prototype.lib.drives.DriveSyncManager
import id.homebase.homebasekmppoc.prototype.lib.drives.query.DriveQueryProvider
import id.homebase.homebasekmppoc.prototype.lib.eventbus.appEventBus
import id.homebase.homebasekmppoc.prototype.lib.websockets.NetworkMonitor
import id.homebase.homebasekmppoc.prototype.lib.websockets.OdinWebSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

class AuthConnectionCoordinator(
    val credentialsManager: CredentialsManager,
    val networkMonitor: NetworkMonitor,
    val driveQueryProvider: DriveQueryProvider
) {
    private var wsClient: OdinWebSocketClient? = null
    private val drives = listOf(chatTargetDrive)

    private val ioScope = CoroutineScope(Dispatchers.IO)

    private val driveSyncManager = DriveSyncManager(
        driveQueryProvider = driveQueryProvider,
        databaseManager = DatabaseManager.appDb,
        scope = ioScope,
        eventBus = appEventBus
    )


    suspend fun onAuthStateChanged(state: YouAuthState) {
        when (state) {
            is YouAuthState.Authenticated -> connect(state)
            else -> disconnect()
        }
    }

    suspend fun onNetworkChanged(isOnline: Boolean) {
        if (!isOnline) disconnect()
        else wsClient?.start()
    }

    private fun connect(state: YouAuthState.Authenticated) {
        if (wsClient != null) return

        wsClient = OdinWebSocketClient(
            credentialsManager,
            ioScope,
            appEventBus,
            DatabaseManager.appDb,
            drives,
            onConnected = {
                handleWsConnect()
            },
            onDisconnected = {
                driveSyncManager.cancelAll()
            }
        ).also { it.start() }
    }

    private fun handleWsConnect() {
        ioScope.launch {
            val creds = credentialsManager.getActiveCredentials() ?: return@launch

            driveSyncManager.onConnected(
                identityId = creds.getIdentityId(),
                drives = drives
            )
        }
    }


    private fun disconnect() {
        wsClient?.close()
        wsClient = null
    }
}
