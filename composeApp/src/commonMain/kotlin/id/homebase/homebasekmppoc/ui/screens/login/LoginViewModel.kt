
package id.homebase.homebasekmppoc.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.homebase.homebasekmppoc.prototype.lib.authentication.AuthState
import id.homebase.homebasekmppoc.prototype.lib.youauth.YouAuthManager
import id.homebase.homebasekmppoc.prototype.ui.app.getAppParams
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Login screen following strict MVI pattern.
 * - Single state via StateFlow
 * - Single entry point via onAction()
 * - One-off events via Channel
 */
class LoginViewModel(
    private val youAuthManager: YouAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<LoginUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        observeAuthState()
    }

    /**
     * Single entry point for all UI actions.
     */
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

    private fun performLogin() {
        val homebaseId = _uiState.value.homebaseId
        if (homebaseId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a Homebase ID") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val appParams = getAppParams()
                youAuthManager.authorize(homebaseId, viewModelScope, appParams)
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isLoading = false, errorMessage = e.message ?: "Login failed") 
                }
            }
        }
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            youAuthManager.youAuthState.collect { authState ->
                when (authState) {
                    is AuthState.Unauthenticated -> {
                        _uiState.update { 
                            it.copy(isLoading = false, isAuthenticated = false) 
                        }
                    }
                    is AuthState.Authenticating -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is AuthState.Authenticated -> {
                        _uiState.update { 
                            it.copy(isLoading = false, isAuthenticated = true) 
                        }
                        _uiEvent.send(LoginUiEvent.NavigateToHome)
                    }
                    is AuthState.Error -> {
                        _uiState.update { 
                            it.copy(isLoading = false, errorMessage = authState.message) 
                        }
                    }
                }
            }
        }
    }
}
