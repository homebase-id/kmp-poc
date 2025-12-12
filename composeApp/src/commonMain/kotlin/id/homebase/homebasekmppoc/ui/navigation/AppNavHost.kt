package id.homebase.homebasekmppoc.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.homebase.homebasekmppoc.lib.youAuth.YouAuthFlowManager
import id.homebase.homebasekmppoc.lib.youAuth.YouAuthState
import id.homebase.homebasekmppoc.prototype.lib.authentication.AuthenticationManager
import id.homebase.homebasekmppoc.prototype.ui.db.DbPage
import id.homebase.homebasekmppoc.prototype.ui.driveFetch.DriveFetchPage
import id.homebase.homebasekmppoc.prototype.ui.video.VideoPlayerTestPage
import id.homebase.homebasekmppoc.prototype.ui.ws.WebsocketPage
import id.homebase.homebasekmppoc.ui.screens.home.HomeScreen
import id.homebase.homebasekmppoc.ui.screens.home.HomeUiEvent
import id.homebase.homebasekmppoc.ui.screens.home.HomeViewModel
import id.homebase.homebasekmppoc.ui.screens.login.LoginScreen
import id.homebase.homebasekmppoc.ui.screens.login.LoginUiEvent
import id.homebase.homebasekmppoc.ui.screens.login.LoginViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Main navigation host for the application. Handles routing between all screens with auth
 * protection where needed.
 *
 * @param navController Navigation controller for managing back stack
 * @param youAuthFlowManager Manager for YouAuth authentication flow
 * @param isAuthenticated Whether user is currently authenticated (checked on app start)
 */
@Composable
fun AppNavHost(
        navController: NavHostController = rememberNavController(),
        youAuthFlowManager: YouAuthFlowManager,
        isAuthenticated: Boolean = false
) {
    // Determine start destination based on auth state
    val startDestination =
            remember(isAuthenticated) { if (isAuthenticated) Route.Home else Route.Login }

    NavHost(navController = navController, startDestination = startDestination) {
        // Login route
        composable<Route.Login> {
            val viewModel = koinViewModel<LoginViewModel>()
            val state by viewModel.uiState.collectAsState()

            // Observe navigation events
            ObserveAsEvents(viewModel.uiEvent) { event ->
                when (event) {
                    is LoginUiEvent.NavigateToHome -> {
                        navController.navigate(Route.Home) {
                            popUpTo(Route.Login) { inclusive = true }
                        }
                    }
                    is LoginUiEvent.ShowError -> {
                        // TODO: Show snackbar
                    }
                }
            }

            LoginScreen(state = state, onAction = viewModel::onAction)
        }

        // Protected Home route
        composable<Route.Home> {
            AuthenticatedRouteWithFlowManager(
                    authState = youAuthFlowManager.authState,
                    onUnauthenticated = {
                        navController.navigate(Route.Login) { popUpTo(0) { inclusive = true } }
                    }
            ) {
                val viewModel = koinViewModel<HomeViewModel>()
                val state by viewModel.uiState.collectAsState()

                ObserveAsEvents(viewModel.uiEvent) { event ->
                    when (event) {
                        is HomeUiEvent.NavigateToDriveFetch ->
                                navController.navigate(Route.DriveFetch)
                        is HomeUiEvent.NavigateToDatabase -> navController.navigate(Route.Database)
                        is HomeUiEvent.NavigateToWebSocket ->
                                navController.navigate(Route.WebSocket)
                        is HomeUiEvent.NavigateToVideo -> navController.navigate(Route.Video)
                        is HomeUiEvent.NavigateToLogin -> {
                            navController.navigate(Route.Login) { popUpTo(0) { inclusive = true } }
                        }
                    }
                }

                HomeScreen(state = state, onAction = viewModel::onAction)
            }
        }

        // Protected DriveFetch route
        composable<Route.DriveFetch> {
            AuthenticatedRouteWithFlowManager(
                    authState = youAuthFlowManager.authState,
                    onUnauthenticated = {
                        navController.navigate(Route.Login) { popUpTo(0) { inclusive = true } }
                    }
            ) {
                DriveFetchPage(
                        youAuthFlowManager = youAuthFlowManager,
                        onNavigateBack = { navController.popBackStack() }
                )
            }
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

/** Wrapper for routes that require authentication using YouAuthFlowManager. */
@Composable
private fun AuthenticatedRouteWithFlowManager(
        authState: kotlinx.coroutines.flow.StateFlow<YouAuthState>,
        onUnauthenticated: () -> Unit,
        content: @Composable () -> Unit
) {
    val currentAuthState by authState.collectAsState()

    when (currentAuthState) {
        is YouAuthState.Authenticated -> content()
        is YouAuthState.Unauthenticated -> onUnauthenticated()
        is YouAuthState.Authenticating -> {
            // Show loading or nothing while authenticating
        }
        is YouAuthState.Error -> onUnauthenticated()
    }
}
