package id.homebase.homebasekmppoc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.rememberNavController
import id.homebase.homebasekmppoc.di.allModules
import id.homebase.homebasekmppoc.lib.youauth.OdinClientFactory
import id.homebase.homebasekmppoc.lib.youauth.YouAuthFlowManager
import id.homebase.homebasekmppoc.prototype.lib.authentication.AuthConnectionCoordinator
import id.homebase.homebasekmppoc.ui.navigation.AppNavHost
import id.homebase.homebasekmppoc.ui.theme.HomebaseTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

/** Main application entry point. Sets up Koin DI, theme, and navigation. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun App() {
    KoinApplication(application = { modules(allModules) }) {
        HomebaseTheme {
            val navController = rememberNavController()
            val youAuthFlowManager: YouAuthFlowManager = koinInject()
            val coordinator: AuthConnectionCoordinator = koinInject()
//            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                launch {
                    youAuthFlowManager.authState.collect {
                        coordinator.onAuthStateChanged(it)
                    }
                }
                launch {
                    coordinator.networkMonitor.isOnline.collect {
                        coordinator.onNetworkChanged(it)
                    }
                }
            }

            AppNavHost(
                    navController = navController,
                    youAuthFlowManager = youAuthFlowManager,
                    isAuthenticated = OdinClientFactory.hasStoredCredentials()
            )
        }
    }
}
