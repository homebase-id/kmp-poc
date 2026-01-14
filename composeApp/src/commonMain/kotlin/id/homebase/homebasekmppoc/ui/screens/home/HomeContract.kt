package id.homebase.homebasekmppoc.ui.screens.home

/** Single immutable state for Home screen. */
data class HomeUiState(
        val isLoading: Boolean = false,
        val showPermissionDialog: Boolean = false,
        val permissionExtensionUrl: String? = null,
        val appName: String = "Homebase KMP"
)

/** All possible user actions on Home screen. */
sealed interface HomeUiAction {
    data object DriveFetchClicked : HomeUiAction
    data object DatabaseClicked : HomeUiAction
    data object WebSocketClicked : HomeUiAction
    data object VideoClicked : HomeUiAction
    data object CdnTestClicked : HomeUiAction
    data object DriveUploadClicked : HomeUiAction
    data object FFmpegTestClicked : HomeUiAction
    data object ChatListClicked : HomeUiAction
    data object LogoutClicked : HomeUiAction
    data object ExtendPermissionsClicked : HomeUiAction
    data object DismissPermissionDialog : HomeUiAction
}

/** One-off events for side effects (navigation). */
sealed interface HomeUiEvent {
    data object NavigateToDriveFetch : HomeUiEvent
    data object NavigateToDatabase : HomeUiEvent
    data object NavigateToWebSocket : HomeUiEvent
    data object NavigateToVideo : HomeUiEvent
    data object NavigateToCdnTest : HomeUiEvent
    data object NavigateToDriveUpload : HomeUiEvent
    data object NavigateToFFmpegTest : HomeUiEvent
    data object NavigateToChatList : HomeUiEvent
    data object NavigateToLogin : HomeUiEvent
    data class OpenPermissionExtensionBrowser(val url: String) : HomeUiEvent
}
