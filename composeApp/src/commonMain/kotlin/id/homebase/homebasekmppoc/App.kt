package id.homebase.homebasekmppoc

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import id.homebase.homebasekmppoc.di.allModules
import id.homebase.homebasekmppoc.lib.youAuth.OdinClientFactory
import id.homebase.homebasekmppoc.lib.youAuth.YouAuthFlowManager
import id.homebase.homebasekmppoc.ui.navigation.AppNavHost
import id.homebase.homebasekmppoc.ui.theme.HomebaseTheme
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

            AppNavHost(
                    navController = navController,
                    youAuthFlowManager = youAuthFlowManager,
                    isAuthenticated = OdinClientFactory.hasStoredCredentials()
            )
        }
    }
}
