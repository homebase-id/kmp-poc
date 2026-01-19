package id.homebase.homebasekmppoc.lib.websockets

import id.homebase.homebasekmppoc.prototype.lib.websockets.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow

class AlwaysOnlineNetworkMonitor : NetworkMonitor {
    override val isOnline = MutableStateFlow(true)
}
