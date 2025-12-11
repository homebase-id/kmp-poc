package id.homebase.homebasekmppoc.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.homebase.homebasekmppoc.lib.youAuth.DrivePermissionType
import id.homebase.homebasekmppoc.lib.youAuth.TargetDriveAccessRequest
import id.homebase.homebasekmppoc.lib.youAuth.YouAuthFlowManager
import id.homebase.homebasekmppoc.lib.youAuth.YouAuthState
import id.homebase.homebasekmppoc.prototype.lib.drives.TargetDrive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid


private object AppConfig {
    const val APP_ID = "0cecc6fe033e48b19ee6a4f60318be02"
    const val APP_NAME = "Homebase - KMP POC"
}


val feedTargetDrive: TargetDrive = TargetDrive(
    alias = Uuid.parse("4db49422ebad02e99ab96e9c477d1e08"),
    type = Uuid.parse ("a3227ffba87608beeb24fee9b70d92a6")
)

var targetDriveAccessRequest : List<TargetDriveAccessRequest> = listOf(
    TargetDriveAccessRequest(
        alias = feedTargetDrive.alias.toString(),
        type = feedTargetDrive.type.toString(),
        name = "Feed Drive",
        description = " ",
        permissions = listOf(
            DrivePermissionType.Read,
            DrivePermissionType.Write,
        )

    )
)

/**
 * ViewModel for Login screen following strict MVI pattern.
 * - Single state via StateFlow
 * - Single entry point via onAction()
 * - One-off events via Channel
 */
class LoginViewModel(private val youAuthFlowManager: YouAuthFlowManager) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<LoginUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        checkExistingSession()
        observeAuthState()
    }

    /** Single entry point for all UI actions. */
    fun onAction(action: LoginUiAction) {
        when (action) {
            is LoginUiAction.HomebaseIdChanged -> {
                _uiState.update { it.copy(homebaseId = action.value, errorMessage = null) }
            }
            is LoginUiAction.LoginClicked -> {
                performLogin()
            }
            is LoginUiAction.RetryClicked -> {
                performLogin()
            }
        }
    }

    private fun checkExistingSession() {
        if (youAuthFlowManager.restoreSession()) {
            _uiState.update { it.copy(isAuthenticated = true) }
            viewModelScope.launch { _uiEvent.send(LoginUiEvent.NavigateToHome) }
        }
    }

    private fun performLogin() {
        val homebaseId = _uiState.value.homebaseId
        if (homebaseId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid Homebase ID") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                youAuthFlowManager.authorize(
                        identity = homebaseId,
                        scope = viewModelScope,
                        appId = AppConfig.APP_ID,
                        appName = AppConfig.APP_NAME,
                        drives = targetDriveAccessRequest
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Login failed")
                }
            }
        }
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            youAuthFlowManager.authState.collect { authState ->
                when (authState) {
                    is YouAuthState.Unauthenticated -> {
                        _uiState.update { it.copy(isLoading = false, isAuthenticated = false) }
                    }
                    is YouAuthState.Authenticating -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is YouAuthState.Authenticated -> {
                        _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
                        _uiEvent.send(LoginUiEvent.NavigateToHome)
                    }
                    is YouAuthState.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = authState.message)
                        }
                    }
                }
            }
        }
    }
}
