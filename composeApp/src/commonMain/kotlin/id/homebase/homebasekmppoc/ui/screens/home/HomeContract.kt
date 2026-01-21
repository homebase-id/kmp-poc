package id.homebase.homebasekmppoc.ui.screens.home

import kotlin.time.Instant
import kotlin.uuid.Uuid

/** Single immutable state for Home screen. */

enum class ConnectionStatus {
    Connecting,
    Online,
    Offline
}

enum class DriveSyncStatus {
    Syncing,
    Completed,
    Failed
}

data class DriveSyncItem(
    val driveId: Uuid,
    val status: DriveSyncStatus,
    val totalCount: Int? = null,
    val errorMessage: String? = null,
    val lastSyncAt: Instant? = null
)

data class HomeUiState(
    val identity: String? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Offline,
    val showPermissionDialog: Boolean = false,
    val permissionExtensionUrl: String? = null,
    val appName: String = "",
    val syncingDrives: List<DriveSyncItem> = emptyList()
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
