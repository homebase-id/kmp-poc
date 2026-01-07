package id.homebase.homebasekmppoc.prototype.ui.driveFetch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment


enum class PendingDeleteType { Soft, Hard }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailPage(
    viewModel: FileDetailViewModel,
    onNavigateBack: () -> Unit
) {

    var pendingDelete by remember {
        mutableStateOf<PendingDeleteType?>(null)
    }

    val state by viewModel.uiState.collectAsState(
        initial = FileDetailUiState()
    )

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                FileDetailUiEvent.NavigateBack ->
                    onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Detail") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.onAction(FileDetailUiAction.BackClicked)
                        }
                    ) {
                        Text("←", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            Text("File ID", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text(viewModel.fileId.toString(), style = MaterialTheme.typography.bodyLarge)

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    enabled = !state.isLoading,
                    onClick = {
                        viewModel.onAction(FileDetailUiAction.GetFileHeaderClicked)
                    }
                ) {
                    Text(if (state.isLoading) "Loading…" else "Get file header")
                }

                DeleteDropdownButton(
                    enabled = !state.isLoading,
                    onRequestSoftDelete = {
                        pendingDelete = PendingDeleteType.Soft
                    },
                    onRequestHardDelete = {
                        pendingDelete = PendingDeleteType.Hard
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            when {
                state.isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Loading file header…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                state.header != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        FileHeaderPanel(
                            header = state.header!!,
                            error = state.error,
                            thumbnailBytes = state.thumbnails,
                            payloadBytes = state.payloads,
                            expandedPayloadKey = state.expandedPayloadKey,
                            onViewPayload = { key ->
                                viewModel.onAction(
                                    FileDetailUiAction.ViewPayloadClicked(key)
                                )
                            },
                            onGetThumbnail = { key, w, h ->
                                viewModel.onAction(
                                    FileDetailUiAction.GetThumbnailClicked(key, w, h)
                                )
                            },
                            onGetPayloadRange = { key, start, length ->
                                viewModel.onAction(
                                    FileDetailUiAction.GetPayloadRangeClicked(key, start, length)
                                )
                            },
                            onSaveLocalAppContent = { content ->
                                viewModel.onAction(
                                    FileDetailUiAction.LocalAppContentSaved(content)
                                )
                            },
                            onSaveLocalAppTags = { tags ->
                                viewModel.onAction(
                                    FileDetailUiAction.LocalAppTagsSaved(tags)
                                )
                            }
                        )
                    }
                }

                state.hasTriedToLoadHeader -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.error ?: "File not found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "The file could not be loaded or does not exist.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No file loaded",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Tap “Get file header” to load file details.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

        }
    }

    pendingDelete?.let { type ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },

            title = {
                Text(
                    if (type == PendingDeleteType.Hard)
                        "Hard delete file"
                    else
                        "Soft delete file"
                )
            },

            text = {
                Text(
                    if (type == PendingDeleteType.Hard)
                        "This will permanently delete the file. This action cannot be undone."
                    else
                        "This will remove the file from active use, but it can be restored later."
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        viewModel.onAction(
                            if (type == PendingDeleteType.Hard)
                                FileDetailUiAction.HardDeleteClicked
                            else
                                FileDetailUiAction.SoftDeleteClicked
                        )
                    },
                    colors =
                        if (type == PendingDeleteType.Hard)
                            ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        else
                            ButtonDefaults.textButtonColors()
                ) {
                    Text(
                        if (type == PendingDeleteType.Hard)
                            "Hard delete"
                        else
                            "Soft delete"
                    )
                }
            },

            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

}

@Composable
fun DeleteDropdownButton(
    enabled: Boolean = true,
    onRequestSoftDelete: () -> Unit,
    onRequestHardDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(
            enabled = enabled,
            onClick = { expanded = true }
        ) {
            Text("Delete")
            Spacer(Modifier.width(4.dp))
            Text("▾")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Soft delete") },
                onClick = {
                    expanded = false
                    onRequestSoftDelete()
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        "Hard delete",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    expanded = false
                    onRequestHardDelete()
                }
            )
        }
    }
}