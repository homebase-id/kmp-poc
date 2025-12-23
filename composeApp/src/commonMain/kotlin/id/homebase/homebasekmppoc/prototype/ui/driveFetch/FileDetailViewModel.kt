package id.homebase.homebasekmppoc.prototype.ui.driveFetch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.homebase.homebasekmppoc.prototype.lib.drives.files.DriveFileProvider
import id.homebase.homebasekmppoc.prototype.lib.drives.files.HomebaseFile
import id.homebase.homebasekmppoc.prototype.lib.drives.files.PayloadDescriptor
import id.homebase.homebasekmppoc.prototype.lib.drives.files.ThumbnailDescriptor
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

class FileDetailViewModel(
    val driveId: Uuid,
    val fileId: Uuid,
    private val driveFileProvider: DriveFileProvider?
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileDetailUiState())
    val uiState: StateFlow<FileDetailUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<FileDetailUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onAction(action: FileDetailUiAction) {
        when (action) {
            FileDetailUiAction.BackClicked ->
                sendEvent(FileDetailUiEvent.NavigateBack)

            FileDetailUiAction.GetFileHeaderClicked ->
                loadHeader()
        }
    }

    private fun loadHeader() {
        if (driveFileProvider == null) {
            _uiState.update {
                it.copy(error = "Not authenticated")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            try {
                val header = driveFileProvider.getFileHeader(driveId, fileId)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        header = header
                    )
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = t.message ?: "Failed to load file header"
                    )
                }
            }
        }
    }

    private fun sendEvent(event: FileDetailUiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }
}

@Composable
fun FileHeaderPanel(
    header: HomebaseFile,
    onViewPayload: (key: String) -> Unit = {},
    onGetThumbnail: (payloadKey: String, thumbnail: ThumbnailDescriptor) -> Unit = { _, _ -> }
) {
    Column {
        Text(
            text = "File Header",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(12.dp))

        // ── Core metadata ────────────────────────

        LabeledValue("Created", header.fileMetadata.created.toString())
        LabeledValue("Updated", header.fileMetadata.updated.toString())
        LabeledValue("Encrypted", header.fileMetadata.isEncrypted.toString())
        LabeledValue(
            "Sender",
            header.fileMetadata.senderOdinId ?: "—"
        )

        Spacer(Modifier.height(16.dp))

        // ── Payloads ─────────────────────────────

        Text(
            text = "Payloads",
            style = MaterialTheme.typography.titleSmall
        )

        Spacer(Modifier.height(8.dp))

        val payloads = header.fileMetadata.payloads

        if (payloads.isNullOrEmpty()) {
            Text(
                text = "No payloads",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            payloads.forEach { payload ->
                PayloadWithThumbnails(
                    payload = payload,
                    onViewPayload = onViewPayload,
                    onGetThumbnail = onGetThumbnail
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
@Composable
private fun PayloadWithThumbnails(
    payload: PayloadDescriptor,
    onViewPayload: (key: String) -> Unit,
    onGetThumbnail: (payloadKey: String, thumbnail: ThumbnailDescriptor) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = payload.key,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = { onViewPayload(payload.key) }
            ) {
                Text("View payload")
            }
        }

        val thumbnails = payload.thumbnails
            ?: payload.previewThumbnail?.let { listOf(it) }
            ?: emptyList()

        if (thumbnails.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))

            thumbnails.forEach { thumb ->
                ThumbnailRow(
                    payloadKey = payload.key,
                    thumbnail = thumb,
                    onGetThumbnail = onGetThumbnail
                )
            }
        } else {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "No thumbnails",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ThumbnailRow(
    payloadKey: String,
    thumbnail: ThumbnailDescriptor,
    onGetThumbnail: (payloadKey: String, thumbnail: ThumbnailDescriptor) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
    ) {
        val sizeText =
            "${thumbnail.pixelWidth ?: "?"} × ${thumbnail.pixelHeight ?: "?"}"

        Text(
            text = sizeText,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )

        Button(
            onClick = { onGetThumbnail(payloadKey, thumbnail) }
        ) {
            Text("Get thumbnail")
        }
    }

    Spacer(Modifier.height(4.dp))
}


@Composable
private fun LabeledValue(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
    }
}


data class FileDetailUiState(
    val isLoading: Boolean = false,
    val header: HomebaseFile? = null,
    val error: String? = null
)

sealed interface FileDetailUiEvent {
    object NavigateBack : FileDetailUiEvent
}

sealed interface FileDetailUiAction {
    object BackClicked : FileDetailUiAction
    object GetFileHeaderClicked : FileDetailUiAction
}
