package id.homebase.homebasekmppoc.ui.screens.login

/** Single immutable state for Login screen. */
data class LoginUiState(
        val homebaseId: String = "frodo.baggins.demo.rocks",
        //val homebaseId: String = "frodo.dotyou.cloud",
        val isLoading: Boolean = false,
        val isAuthenticated: Boolean = false,
        val errorMessage: String? = null
)

/** All possible user actions on Login screen. */
sealed interface LoginUiAction {
    data class HomebaseIdChanged(val value: String) : LoginUiAction
    data object LoginClicked : LoginUiAction
    data object RetryClicked : LoginUiAction
}

/** One-off events for side effects (navigation, snackbars). */
sealed interface LoginUiEvent {
    data object NavigateToHome : LoginUiEvent
    data class ShowError(val message: String) : LoginUiEvent
}
