package id.homebase.homebasekmppoc.prototype.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.homebase.homebasekmppoc.prototype.lib.drives.files.BytesResponse
import id.homebase.homebasekmppoc.prototype.lib.drives.files.DriveFileProvider
import id.homebase.homebasekmppoc.prototype.lib.drives.files.HomebaseFile
import id.homebase.homebasekmppoc.prototype.lib.drives.files.PayloadOperationOptions
import kotlin.uuid.Uuid
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatMessageDetailViewModel(
        val driveId: Uuid,
        val fileId: Uuid,
        private val driveFileProvider: DriveFileProvider?
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatMessageDetailUiState())
    val uiState: StateFlow<ChatMessageDetailUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<ChatMessageDetailUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onAction(action: ChatMessageDetailUiAction) {
        when (action) {
            ChatMessageDetailUiAction.BackClicked ->
                    sendEvent(ChatMessageDetailUiEvent.NavigateBack)
            ChatMessageDetailUiAction.GetFileHeaderClicked -> loadHeader()
            is ChatMessageDetailUiAction.ViewPayloadClicked -> loadPayload(action)
        }
    }

    private fun loadHeader() {
        if (driveFileProvider == null) {
            _uiState.update { it.copy(error = "Not authenticated") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, hasTriedToLoadHeader = true) }

            try {
                val header = driveFileProvider.getFileHeader(driveId, fileId)

                _uiState.update { it.copy(isLoading = false, header = header) }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(isLoading = false, error = t.message ?: "Failed to load file header")
                }
            }
        }
    }

    private fun loadPayload(action: ChatMessageDetailUiAction.ViewPayloadClicked) {
        val provider = driveFileProvider ?: return

        viewModelScope.launch {
            val current = _uiState.value

            // toggle if already expanded
            if (current.expandedPayloadKey == action.payloadKey) {
                _uiState.update { it.copy(expandedPayloadKey = null) }
                return@launch
            }

            // expand immediately (panel shows loading)
            _uiState.update { it.copy(expandedPayloadKey = action.payloadKey) }

            // already loaded → done
            if (current.payloads.containsKey(action.payloadKey)) return@launch

            try {
                val bytes =
                        provider.getPayloadBytesDecrypted(
                                driveId = driveId,
                                fileId = fileId,
                                key = action.payloadKey
                        )

                if (bytes != null) {
                    _uiState.update {
                        it.copy(payloads = it.payloads + (action.payloadKey to bytes))
                    }
                }
            } catch (_: Throwable) {
                // surface later if needed
            }
        }
    }

    private fun sendEvent(event: ChatMessageDetailUiEvent) {
        viewModelScope.launch { _uiEvent.send(event) }
    }
}

data class ChatMessageDetailUiState(
        val isLoading: Boolean = false,
        val header: HomebaseFile? = null,
        val hasTriedToLoadHeader: Boolean = false,
        val error: String? = null,
        val payloads: Map<String, BytesResponse> = emptyMap(),
        val expandedPayloadKey: String? = null
)

sealed interface ChatMessageDetailUiEvent {
    data object NavigateBack : ChatMessageDetailUiEvent
}

sealed interface ChatMessageDetailUiAction {
    data object BackClicked : ChatMessageDetailUiAction
    data object GetFileHeaderClicked : ChatMessageDetailUiAction

    data class ViewPayloadClicked(val payloadKey: String) : ChatMessageDetailUiAction
}
