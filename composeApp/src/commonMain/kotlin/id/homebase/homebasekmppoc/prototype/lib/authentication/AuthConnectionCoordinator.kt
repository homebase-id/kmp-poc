package id.homebase.homebasekmppoc.prototype.lib.authentication

import id.homebase.homebasekmppoc.lib.youauth.YouAuthState
import id.homebase.homebasekmppoc.prototype.lib.base.CredentialsManager
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import id.homebase.homebasekmppoc.prototype.lib.eventbus.appEventBus
import id.homebase.homebasekmppoc.prototype.lib.websockets.NetworkMonitor
import id.homebase.homebasekmppoc.prototype.lib.websockets.OdinWebSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class AuthConnectionCoordinator(
    val credentialsManager: CredentialsManager,
    val networkMonitor: NetworkMonitor,
) {
    private var wsClient: OdinWebSocketClient? = null

    suspend fun onAuthStateChanged(state: YouAuthState) {
        when (state) {
            is YouAuthState.Authenticated -> connect(state)
            else -> disconnect()
        }
    }

    suspend fun onNetworkChanged(isOnline: Boolean) {
        if (!isOnline) disconnect()
        else wsClient?.connect()
    }

    private suspend fun connect(state: YouAuthState.Authenticated) {
        if (wsClient != null) return

        val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

        wsClient = OdinWebSocketClient(
            credentialsManager,
            scope,
            appEventBus,
            DatabaseManager.appDb
        ).also { it.connect() }
    }

    private fun disconnect() {
        wsClient?.close()
        wsClient = null
    }
}
