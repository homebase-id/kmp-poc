package id.homebase.homebasekmppoc.prototype.ui.driveFetch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface DriveFetchUiEvent {
    data class NavigateToFileDetail(val driveId: String, val fileId: String) : DriveFetchUiEvent
    object NavigateBack : DriveFetchUiEvent
}

sealed interface DriveFetchUiAction {
    data class FileClicked(val driveId: String, val fileId: String) : DriveFetchUiAction
    object BackClicked : DriveFetchUiAction
}

class DriveFetchViewModel : ViewModel() {

    private val _uiEvent = Channel<DriveFetchUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onAction(action: DriveFetchUiAction) {
        when (action) {
            is DriveFetchUiAction.FileClicked -> {
                sendEvent(
                    DriveFetchUiEvent.NavigateToFileDetail(action.driveId, action.fileId)
                )
            }

            DriveFetchUiAction.BackClicked -> {
                sendEvent(DriveFetchUiEvent.NavigateBack)
            }
        }
    }

    private fun sendEvent(event: DriveFetchUiEvent) {
        viewModelScope.launch { _uiEvent.send(event) }
    }
}
