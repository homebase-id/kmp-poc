package id.homebase.homebasekmppoc

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import id.homebase.homebasekmppoc.di.allModules
import id.homebase.homebasekmppoc.prototype.lib.youauth.YouAuthManager
import id.homebase.homebasekmppoc.ui.navigation.AppNavHost
import id.homebase.homebasekmppoc.ui.theme.HomebaseTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

/** Main application entry point. Sets up Koin DI, theme, and navigation. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    KoinApplication(application = { modules(allModules) }) {
        HomebaseTheme {
            val navController = rememberNavController()
            val youAuthManager: YouAuthManager = koinInject()

            AppNavHost(
                    navController = navController,
                    youAuthManager = youAuthManager,
                    isAuthenticated = false // TODO: Check secure storage for persisted auth
            )
        }
    }
}
