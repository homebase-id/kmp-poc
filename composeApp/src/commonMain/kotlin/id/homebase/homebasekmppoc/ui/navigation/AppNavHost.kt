package id.homebase.homebasekmppoc.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.homebase.homebasekmppoc.prototype.lib.authentication.AuthenticationManager
import id.homebase.homebasekmppoc.prototype.lib.youauth.YouAuthManager
import id.homebase.homebasekmppoc.prototype.ui.db.DbPage
import id.homebase.homebasekmppoc.prototype.ui.driveFetch.DriveFetchPage
import id.homebase.homebasekmppoc.prototype.ui.video.VideoPlayerTestPage
import id.homebase.homebasekmppoc.prototype.ui.ws.WebsocketPage
import id.homebase.homebasekmppoc.ui.screens.HomeScreen
import id.homebase.homebasekmppoc.ui.screens.LoginScreen

/**
 * Main navigation host for the application. Handles routing between all screens with auth
 * protection where needed.
 *
 * @param navController Navigation controller for managing back stack
 * @param youAuthManager Manager for YouAuth authentication flow
 * @param isAuthenticated Whether user is currently authenticated (checked on app start)
 */
@Composable
fun AppNavHost(
        navController: NavHostController = rememberNavController(),
        youAuthManager: YouAuthManager,
        isAuthenticated: Boolean = false
) {
    // Determine start destination based on auth state
    val startDestination =
            remember(isAuthenticated) { if (isAuthenticated) Route.Home else Route.Login }

    NavHost(navController = navController, startDestination = startDestination) {
        // Login route
        composable<Route.Login> {
            LoginScreen(
                    youAuthManager = youAuthManager,
                    onLoginSuccess = {
                        navController.navigate(Route.Home) {
                            popUpTo(Route.Login) { inclusive = true }
                        }
                    }
            )
        }

        // Protected Home route
        composable<Route.Home> {
            AuthenticatedRoute(
                    authState = youAuthManager.youAuthState,
                    onUnauthenticated = {
                        navController.navigate(Route.Login) { popUpTo(0) { inclusive = true } }
                    }
            ) {
                HomeScreen(
                        navController = navController,
                        onLogout = {
                            youAuthManager.logout()
                            navController.navigate(Route.Login) { popUpTo(0) { inclusive = true } }
                        }
                )
            }
        }

        // Protected DriveFetch route (uses prototype for now)
        composable<Route.DriveFetch> {
            AuthenticatedRoute(
                    authState = youAuthManager.youAuthState,
                    onUnauthenticated = {
                        navController.navigate(Route.Login) { popUpTo(0) { inclusive = true } }
                    }
            ) { DriveFetchPage(youAuthManager) }
        }

        // Database route (uses prototype, no auth required for testing)
        composable<Route.Database> { DbPage() }

        // WebSocket route (uses prototype, no auth required for testing)
        composable<Route.WebSocket> {
            val wsAuthManager = remember { AuthenticationManager() }
            WebsocketPage(wsAuthManager)
        }

        // Video route (uses prototype, no auth required for testing)
        composable<Route.Video> {
            val videoAuthManager = remember { AuthenticationManager() }
            VideoPlayerTestPage(videoAuthManager)
        }
    }
}
