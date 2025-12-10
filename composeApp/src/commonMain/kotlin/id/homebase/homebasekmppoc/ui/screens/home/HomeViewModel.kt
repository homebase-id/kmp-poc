package id.homebase.homebasekmppoc.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.homebase.homebasekmppoc.prototype.lib.youauth.YouAuthManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Home screen following strict MVI pattern.
 * - Single state via StateFlow
 * - Single entry point via onAction()
 * - One-off events via Channel
 */
class HomeViewModel(private val youAuthManager: YouAuthManager) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<HomeUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

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
            is HomeUiAction.LogoutClicked -> {
                performLogout()
            }
        }
    }

    private fun performLogout() {
        viewModelScope.launch {
            youAuthManager.logout()
            _uiEvent.send(HomeUiEvent.NavigateToLogin)
        }
    }

    private fun sendEvent(event: HomeUiEvent) {
        viewModelScope.launch { _uiEvent.send(event) }
    }
}
