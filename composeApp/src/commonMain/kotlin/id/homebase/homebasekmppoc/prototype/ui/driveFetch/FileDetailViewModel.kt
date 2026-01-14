package id.homebase.homebasekmppoc.prototype.ui.driveFetch

import androidx.compose.animation.AnimatedVisibility
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
import id.homebase.homebasekmppoc.prototype.ui.driveUpload.DriveUploadService


class FileDetailViewModel(
    val driveId: Uuid,
    val fileId: Uuid,
    private val driveFileProvider: DriveFileProvider,
    private val driveUploadService: DriveUploadService
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

            FileDetailUiAction.SoftDeleteClicked ->
                softDeleteFile()

            FileDetailUiAction.HardDeleteClicked ->
                hardDeleteFile()

            is FileDetailUiAction.GetThumbnailClicked ->
                loadThumbnail(action)

            is FileDetailUiAction.ViewPayloadClicked ->
                loadPayload(action)

            is FileDetailUiAction.GetPayloadRangeClicked ->
                loadPayloadRange(action)

            is FileDetailUiAction.UpdateFileClicked ->
                updateFile(action)
        }
    }

    private fun updateFile(action: FileDetailUiAction.UpdateFileClicked) {
        val provider = driveUploadService ?: return

        val header = _uiState.value.header

        val versionTag = header?.fileMetadata?.versionTag
        if (versionTag == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    hasTriedToLoadHeader = false,
                    error = "Click get header so we know the version tag"
                )
            }
            return
        }

        val target =
            when (action) {
                FileDetailUiAction.UpdateFileClicked.ByFileId ->
                    DriveUploadService.UpdateTarget.ByFileId(fileId)

                FileDetailUiAction.UpdateFileClicked.ByUniqueId -> {
                    val uniqueId = header
                        .fileMetadata
                        .appData
                        ?.uniqueId

                    if (uniqueId == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                hasTriedToLoadHeader = false,
                                error = "File has no uniqueId; cannot update by uniqueId"
                            )
                        }
                        return
                    }

                    DriveUploadService.UpdateTarget.ByUniqueId(uniqueId)
                }
            }

        viewModelScope.launch {
            try {
                provider.updateTextPost(
                    driveId = driveId,
                    target = target,
                    versionTag = versionTag,
                    contentText = "content post ${Uuid.random()}",
                    payloadText = "payload text ${Uuid.random()}"
                )
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasTriedToLoadHeader = false,
                        error = t.message ?: "Failed to update file"
                    )
                }
            }
        }
    }


    private fun loadPayloadRange(action: FileDetailUiAction.GetPayloadRangeClicked) {
        val provider = driveFileProvider ?: return

        viewModelScope.launch {
            try {
                val rangeBytes =
                    provider.getPayloadBytesDecrypted(
                        driveId = driveId,
                        fileId = fileId,
                        key = action.payloadKey,
                        chunkStart = action.start,
                        chunkLength = action.length
                    )

                if (rangeBytes != null) {
                    _uiState.update {
                        it.copy(
                            payloads = it.payloads + (action.payloadKey to rangeBytes)
                        )
                    }
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = t.message ?: "Failed to get payload range"
                    )
                }
            }
        }
    }

    private fun softDeleteFile() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            try {
                val result = driveFileProvider.deleteFile(driveId, fileId)

                // load the deleted header
                if (result) {
                    val deletedHeader = driveFileProvider.getFileHeader(driveId, fileId)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            header = deletedHeader
                        )
                    }
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

    private fun hardDeleteFile() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            try {
                val result = driveFileProvider.deleteFile(driveId, fileId, hardDelete = true)

                // load the deleted header
                if (result) {
                    val deletedHeader = driveFileProvider.getFileHeader(driveId, fileId)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            header = deletedHeader
                        )
                    }
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

    private fun loadHeader() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    hasTriedToLoadHeader = true
                )
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

        viewModelScope.launch {
            val current = _uiState.value

            // toggle if already expanded
            if (current.expandedPayloadKey == action.payloadKey) {
                _uiState.update {
                    it.copy(expandedPayloadKey = null)
                }
                return@launch
            }

            // expand immediately (panel shows loading)
            _uiState.update {
                it.copy(expandedPayloadKey = action.payloadKey)
            }

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
                        it.copy(
                            payloads = it.payloads + (action.payloadKey to bytes)
                        )
                    }
                }
            } catch (_: Throwable) {
                // surface later if needed
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
                    provider.getThumbBytesDecrypted(
                        driveId = driveId,
                        fileId = fileId,
                        payloadKey = action.payloadKey,
                        width = action.width,
                        height = action.height
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
    payloadBytes: Map<String, BytesResponse>,
    expandedPayloadKey: String?,
    onViewPayload: (key: String) -> Unit = {},
    onGetThumbnail: (payloadKey: String, width: Int, height: Int) -> Unit,
    onGetPayloadRange: (key: String, start: Long, length: Long) -> Unit

) {
    Column {
        Text(
            text = "File Header",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(12.dp))

        // ── Core metadata ────────────────────────

        LabeledValue("State", header.fileState.toString())
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
                    payloadBytes = payloadBytes,
                    expandedPayloadKey = expandedPayloadKey,
                    onViewPayload = onViewPayload,
                    onGetThumbnail = onGetThumbnail,
                    onGetPayloadRange = onGetPayloadRange
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

    payloadBytes: Map<String, BytesResponse>,
    expandedPayloadKey: String?,

    onViewPayload: (key: String) -> Unit,
    onGetThumbnail: (payloadKey: String, width: Int, height: Int) -> Unit,
    onGetPayloadRange: (key: String, start: Long, length: Long) -> Unit

) {
    val isExpanded = expandedPayloadKey == payload.key
    val bytes = payloadBytes[payload.key]

    Column {
        // ── Header row ───────────────────────────────
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
                Text(if (isExpanded) "Hide payload" else "View payload")
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .padding(12.dp)
            ) {
                when {
                    bytes == null -> {
                        Text(
                            text = "Loading payload…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    else -> {
                        PayloadPreview(payload, bytes, onGetPayloadRange)
                    }
                }
            }
        }

        // ── Thumbnails (unchanged) ──────────────────
        val thumbnails = payload.thumbnails
            ?: payload.previewThumbnail?.let { listOf(it) }
            ?: emptyList()

        if (thumbnails.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))

            thumbnails.forEach { thumb ->
                val width = thumb.pixelWidth ?: 0
                val height = thumb.pixelHeight ?: 0
                val key = "${payload.key}:${width}x$height"

                val thumbBytes = thumbnailBytes[key]

                ThumbnailRow(
                    payloadKey = payload.key,
                    thumbnail = thumb,
                    thumbnailBytes = thumbBytes,
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
    val hasTriedToLoadHeader: Boolean = false,
    val error: String? = null,

    val thumbnails: Map<String, BytesResponse> = emptyMap(),

    val payloads: Map<String, BytesResponse> = emptyMap(),

    val expandedPayloadKey: String? = null

)

sealed interface FileDetailUiEvent {
    object NavigateBack : FileDetailUiEvent
}

sealed interface FileDetailUiAction {
    object BackClicked : FileDetailUiAction
    object GetFileHeaderClicked : FileDetailUiAction

    sealed interface UpdateFileClicked : FileDetailUiAction {
        object ByFileId : UpdateFileClicked
        object ByUniqueId : UpdateFileClicked
    }

    object HardDeleteClicked : FileDetailUiAction
    object SoftDeleteClicked : FileDetailUiAction

    data class GetThumbnailClicked(
        val payloadKey: String,
        val width: Int,
        val height: Int
    ) : FileDetailUiAction

    data class ViewPayloadClicked(
        val payloadKey: String
    ) : FileDetailUiAction

    data class GetPayloadRangeClicked(
        val payloadKey: String,
        val start: Long,
        val length: Long
    ) : FileDetailUiAction

}

