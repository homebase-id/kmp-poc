package id.homebase.homebasekmppoc.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.lib.config.getPermissionExtensionConfig
import id.homebase.homebasekmppoc.lib.youauth.OdinClientFactory
import id.homebase.homebasekmppoc.lib.youauth.PermissionExtensionManager
import id.homebase.homebasekmppoc.lib.youauth.SecurityContextProvider
import id.homebase.homebasekmppoc.lib.youauth.YouAuthFlowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for Home screen following strict MVI pattern.
 * - Single state via StateFlow
 * - Single entry point via onAction()
 * - One-off events via Channel
 */
class HomeViewModel(private val youAuthFlowManager: YouAuthFlowManager,
                    private val securityContextProvider: SecurityContextProvider) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<HomeUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

//    init {
//        // Check for missing permissions on init (runs in background, doesn't block UI)
//        checkMissingPermissions()
//    }

//    /** Check if the app has all required permissions. Runs in background without blocking UI. */
//    private fun checkMissingPermissions() {
//        viewModelScope.launch(Dispatchers.Default) {
//            try {
//                val odinClient = OdinClientFactory.createFromStorage()
//                if (odinClient == null) {
//                    Logger.w(TAG) { "No authenticated client available for permission check" }
//                    return@launch
//                }
//
//                val config = getPermissionExtensionConfig()
//
//
//                val hostIdentity = odinClient.getHostIdentity()
//                val manager = PermissionExtensionManager.create(odinClient, hostIdentity)
//                val result = manager.getMissingPermissions(config)
//
//                if (result != null && result.hasMissingPermissions) {
//                    Logger.i(TAG) {
//                        "Missing permissions detected: ${result.missingDrives.size} drives, ${result.missingPermissions.size} permissions"
//                    }
//                    // Switch to Main thread for UI state update
//                    withContext(Dispatchers.Main) {
//                        _uiState.update { state ->
//                            state.copy(
//                                    showPermissionDialog = true,
//                                    permissionExtensionUrl = result.extendPermissionUrl,
//                                    appName = config.appName
//                            )
//                        }
//                    }
//                }
//            } catch (e: Exception) {
//                Logger.e(TAG, e) { "Error checking permissions" }
//            }
//        }
//    }

    /** Single entry point for all UI actions. */
    fun onAction(action: HomeUiAction) {
        when (action) {
            is HomeUiAction.DriveFetchClicked -> {
                sendEvent(HomeUiEvent.NavigateToDriveFetch)
            }
            is HomeUiAction.DatabaseClicked -> {
                sendEvent(HomeUiEvent.NavigateToDatabase)
            }
            is HomeUiAction.WebSocketClicked -> {
                sendEvent(HomeUiEvent.NavigateToWebSocket)
            }
            is HomeUiAction.VideoClicked -> {
                sendEvent(HomeUiEvent.NavigateToVideo)
            }
            is HomeUiAction.CdnTestClicked -> {
                sendEvent(HomeUiEvent.NavigateToCdnTest)
            }
            is HomeUiAction.DriveUploadClicked -> {
                sendEvent(HomeUiEvent.NavigateToDriveUpload)
            }
            is HomeUiAction.FFmpegTestClicked -> {
                sendEvent(HomeUiEvent.NavigateToFFmpegTest)
            }
            is HomeUiAction.ChatListClicked -> {
                sendEvent(HomeUiEvent.NavigateToChatList)
            }
            is HomeUiAction.LogoutClicked -> {
                performLogout()
            }
            is HomeUiAction.ExtendPermissionsClicked -> {
                handleExtendPermissions()
            }
            is HomeUiAction.DismissPermissionDialog -> {
                _uiState.update { it.copy(showPermissionDialog = false) }
            }
        }
    }

    private fun handleExtendPermissions() {
        val url = _uiState.value.permissionExtensionUrl
        if (url != null) {
            _uiState.update { it.copy(showPermissionDialog = false) }
            sendEvent(HomeUiEvent.OpenPermissionExtensionBrowser(url))
        }
    }

    private fun performLogout() {
        viewModelScope.launch {
            youAuthFlowManager.logout()
            _uiEvent.send(HomeUiEvent.NavigateToLogin)
        }
    }

    private fun sendEvent(event: HomeUiEvent) {
        viewModelScope.launch { _uiEvent.send(event) }
    }
}
