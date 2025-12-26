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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import id.homebase.homebasekmppoc.lib.image.toImageBitmap
import id.homebase.homebasekmppoc.prototype.lib.drives.files.BytesResponse
import id.homebase.homebasekmppoc.prototype.lib.drives.files.FileOperationOptions
import id.homebase.homebasekmppoc.prototype.lib.drives.files.PayloadOperationOptions


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

            is FileDetailUiAction.GetThumbnailClicked ->
                loadThumbnail(action)

            is FileDetailUiAction.ViewPayloadClicked ->
                loadPayload(action)
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

    private fun loadPayload(action: FileDetailUiAction.ViewPayloadClicked) {
        val provider = driveFileProvider ?: return

        // already loaded → do nothing
        if (_uiState.value.payload != null) return;

        viewModelScope.launch {
            try {
                val bytes =
                    provider.getPayloadBytes(
                        driveId = driveId,
                        fileId = fileId,
                        key = action.payloadKey,
                        options = PayloadOperationOptions()
                    )

                if (bytes != null) {
                    _uiState.update {
                        it.copy(
                            payload = it.payload
                        )
                    }
                }
            } catch (t: Throwable) {
                // swallow for now or surface later
            }
        }
    }

    private fun loadThumbnail(action: FileDetailUiAction.GetThumbnailClicked) {
        val provider = driveFileProvider ?: return

        val key = thumbKey(action.payloadKey, action.width, action.height)

        // already loaded → do nothing
        if (_uiState.value.thumbnails.containsKey(key)) return

        viewModelScope.launch {
            try {
                val bytes =
                    provider.getThumbBytes(
                        driveId = driveId,
                        fileId = fileId,
                        payloadKey = action.payloadKey,
                        width = action.width,
                        height = action.height,
                        options = FileOperationOptions(decrypt = true)
                    )

                if (bytes != null) {
                    _uiState.update {
                        it.copy(
                            thumbnails =
                                it.thumbnails + (key to bytes)
                        )
                    }
                }
            } catch (t: Throwable) {
                // swallow for now or surface later
            }
        }
    }

    private fun thumbKey(
        payloadKey: String,
        width: Int,
        height: Int
    ): String {
        return "$payloadKey:${width}x$height"
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
    thumbnailBytes: Map<String, BytesResponse>,
    onViewPayload: (key: String) -> Unit = {},
    onGetThumbnail: (payloadKey: String, width: Int, height: Int) -> Unit
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
                    thumbnailBytes = thumbnailBytes,
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
    thumbnailBytes: Map<String, BytesResponse>,
    onViewPayload: (key: String) -> Unit,
    onGetThumbnail: (payloadKey: String, width: Int, height: Int) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Key: ${payload.key}",
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
                val width = thumb.pixelWidth ?: 0
                val height = thumb.pixelHeight ?: 0
                val key = "${payload.key}:${width}x$height"

                val bytes = thumbnailBytes[key]

                ThumbnailRow(
                    payloadKey = payload.key,
                    thumbnail = thumb,
                    thumbnailBytes = bytes,
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
    thumbnailBytes: BytesResponse?,
    onGetThumbnail: (payloadKey: String, width: Int, height: Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
    ) {
        val width = thumbnail.pixelWidth ?: 0
        val height = thumbnail.pixelHeight ?: 0

        Text(
            text = "${width} × ${height}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )

        Button(
            onClick = {
                onGetThumbnail(payloadKey, width, height)
            }
        ) {
            Text("Get thumbnail")
        }
    }

    // 👇 Render image if loaded
    if (thumbnailBytes != null) {
        Spacer(Modifier.height(8.dp))
        ThumbnailImage(
            bytes = thumbnailBytes.bytes,
            pixelWidth = thumbnail.pixelWidth,
            pixelHeight = thumbnail.pixelHeight
        )
    }

    Spacer(Modifier.height(8.dp))
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


@Composable
fun ThumbnailImage(
    bytes: ByteArray,
    pixelWidth: Int?,
    pixelHeight: Int?,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(bytes) {
        bytes.toImageBitmap()
    }

    val density = LocalDensity.current

    // Convert only if both dimensions are known
    val sizeModifier =
        if (pixelWidth != null && pixelHeight != null) {
            val widthDp = with(density) { pixelWidth.toDp() }
            val heightDp = with(density) { pixelHeight.toDp() }
            Modifier.size(widthDp, heightDp)
        } else {
            // Fallback size to avoid zero / layout issues
            Modifier
                .fillMaxWidth()
                .height(120.dp)
        }

    Column(modifier = modifier.padding(8.dp)) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Thumbnail",
                modifier = sizeModifier
            )
        } else {
            Text(
                text = "Unable to render image",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}


data class FileDetailUiState(
    val isLoading: Boolean = false,
    val header: HomebaseFile? = null,
    val error: String? = null,

    val thumbnails: Map<String, BytesResponse> = emptyMap(),
    val payload: BytesResponse? = null
)

sealed interface FileDetailUiEvent {
    object NavigateBack : FileDetailUiEvent
}

sealed interface FileDetailUiAction {
    object BackClicked : FileDetailUiAction
    object GetFileHeaderClicked : FileDetailUiAction

    data class GetThumbnailClicked(
        val payloadKey: String,
        val width: Int,
        val height: Int
    ) : FileDetailUiAction

    data class ViewPayloadClicked(
        val payloadKey: String
    ) : FileDetailUiAction
}
