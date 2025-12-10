package id.homebase.homebasekmppoc.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.homebase.homebasekmppoc.lib.youAuth.YouAuthFlowManager
import id.homebase.homebasekmppoc.lib.youAuth.YouAuthState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Default app configuration - can be moved to a config file
private object AppConfig {
    const val APP_ID = "32f0bdbf-017f-4fc0-8004-2d4631182d1e"
    const val APP_NAME = "Homebase - Photos"
}

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
            _uiState.update { it.copy(errorMessage = "Please enter a Homebase ID") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                youAuthFlowManager.authorize(
                        identity = homebaseId,
                        scope = viewModelScope,
                        appId = AppConfig.APP_ID,
                        appName = AppConfig.APP_NAME
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
