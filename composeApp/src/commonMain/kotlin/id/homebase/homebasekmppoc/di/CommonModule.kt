package id.homebase.homebasekmppoc.di

import id.homebase.homebasekmppoc.lib.websockets.AlwaysOnlineNetworkMonitor
import id.homebase.homebasekmppoc.prototype.lib.websockets.NetworkMonitor
import org.koin.dsl.module

val commonModule = module {
    single<NetworkMonitor> { AlwaysOnlineNetworkMonitor() }
}
