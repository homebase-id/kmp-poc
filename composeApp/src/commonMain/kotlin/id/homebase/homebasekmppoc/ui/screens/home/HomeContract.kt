package id.homebase.homebasekmppoc.ui.screens.home

/** Single immutable state for Home screen. */
data class HomeUiState(val isLoading: Boolean = false)

/** All possible user actions on Home screen. */
sealed interface HomeUiAction {
    data object DriveFetchClicked : HomeUiAction
    data object DatabaseClicked : HomeUiAction
    data object WebSocketClicked : HomeUiAction
    data object VideoClicked : HomeUiAction
    data object LogoutClicked : HomeUiAction
}

/** One-off events for side effects (navigation). */
sealed interface HomeUiEvent {
    data object NavigateToDriveFetch : HomeUiEvent
    data object NavigateToDatabase : HomeUiEvent
    data object NavigateToWebSocket : HomeUiEvent
    data object NavigateToVideo : HomeUiEvent
    data object NavigateToLogin : HomeUiEvent
}
