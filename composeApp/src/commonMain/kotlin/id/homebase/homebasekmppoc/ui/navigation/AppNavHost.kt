package id.homebase.homebasekmppoc.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.savedstate.read
import id.homebase.homebasekmppoc.lib.youAuth.YouAuthFlowManager
import id.homebase.homebasekmppoc.lib.youAuth.YouAuthState
import id.homebase.homebasekmppoc.prototype.generateUuidBytes
import id.homebase.homebasekmppoc.prototype.lib.authentication.AuthenticationManager
import id.homebase.homebasekmppoc.prototype.ui.cdn.CdnTestPage
import id.homebase.homebasekmppoc.prototype.ui.db.DbPage
import id.homebase.homebasekmppoc.prototype.ui.driveFetch.DriveFetchPage
import id.homebase.homebasekmppoc.prototype.ui.driveFetch.FileDetailPage
import id.homebase.homebasekmppoc.prototype.ui.driveFetch.FileDetailViewModel
import id.homebase.homebasekmppoc.prototype.ui.driveUpload.DriveUploadScreen
import id.homebase.homebasekmppoc.prototype.ui.driveUpload.DriveUploadUiAction
import id.homebase.homebasekmppoc.prototype.ui.driveUpload.DriveUploadUiEvent
import id.homebase.homebasekmppoc.prototype.ui.driveUpload.DriveUploadViewModel
import id.homebase.homebasekmppoc.prototype.ui.video.VideoPlayerTestPage
import id.homebase.homebasekmppoc.prototype.ui.ws.WebsocketPage
import id.homebase.homebasekmppoc.ui.screens.home.HomeScreen
import id.homebase.homebasekmppoc.ui.screens.home.HomeUiEvent
import id.homebase.homebasekmppoc.ui.screens.home.HomeViewModel
import id.homebase.homebasekmppoc.ui.screens.login.LoginScreen
import id.homebase.homebasekmppoc.ui.screens.login.LoginUiEvent
import id.homebase.homebasekmppoc.ui.screens.login.LoginViewModel
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.uuid.Uuid

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
                        is HomeUiEvent.NavigateToCdnTest -> navController.navigate(Route.CdnTest)
                        is HomeUiEvent.NavigateToDriveUpload ->
                            navController.navigate(Route.DriveUpload)

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
                    navController.navigate(Route.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            ) {
                DriveFetchPage(
                    youAuthFlowManager = youAuthFlowManager,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToFileDetail = { driveId, fileId ->
                        navController.navigate(Route.FileDetail(driveId, fileId))
                    }
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

        // Video route - uses shared auth from YouAuthFlowManager
        composable<Route.Video> { VideoPlayerTestPage(youAuthFlowManager) }

        // CdnTest route - uses shared auth from YouAuthFlowManager
        composable<Route.CdnTest> { CdnTestPage(youAuthFlowManager) }

        // DriveUpload route - uses MVI pattern with ViewModel
        composable<Route.DriveUpload> {
            AuthenticatedRouteWithFlowManager(
                authState = youAuthFlowManager.authState,
                onUnauthenticated = {
                    navController.navigate(Route.Login) { popUpTo(0) { inclusive = true } }
                }
            ) {
                val viewModel = koinViewModel<DriveUploadViewModel>()
                val state by viewModel.uiState.collectAsState()
                val coroutineScope = rememberCoroutineScope()

                // Handle events from ViewModel
                ObserveAsEvents(viewModel.uiEvent) { event ->
                    when (event) {
                        is DriveUploadUiEvent.OpenImagePicker -> {
                            coroutineScope.launch {
                                try {
                                    val file = FileKit.openFilePicker(type = FileKitType.Image)
                                    if (file != null) {
                                        viewModel.onAction(
                                            DriveUploadUiAction.ImagePicked(
                                                bytes = file.readBytes(),
                                                name = file.name
                                            )
                                        )
                                    } else {
                                        viewModel.onAction(DriveUploadUiAction.ImagePickCancelled)
                                    }
                                } catch (e: Exception) {
                                    viewModel.onAction(
                                        DriveUploadUiAction.ImagePickFailed(
                                            e.message ?: "Unknown error"
                                        )
                                    )
                                }
                            }
                        }

                        is DriveUploadUiEvent.ShowSuccess -> {
                            // TODO: Show snackbar
                        }

                        is DriveUploadUiEvent.ShowError -> {
                            // TODO: Show snackbar
                        }
                    }
                }

                DriveUploadScreen(
                    state = state,
                    onAction = viewModel::onAction,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable<Route.FileDetail> { backStackEntry ->
            val driveId = backStackEntry.arguments
                ?.read { getString("driveId") }
                ?: error("driveId missing")

            val fileId = backStackEntry.arguments
                ?.read { getString("fileId") }
                ?: error("fileId missing")

            val viewModel = koinViewModel<FileDetailViewModel>(
                parameters = { parametersOf(Uuid.parse(driveId), Uuid.parse(fileId)) }
            )

            FileDetailPage(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
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
