package id.homebase.homebasekmppoc.prototype.ui.chat

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.homebase.homebasekmppoc.prototype.lib.drives.files.BytesResponse
import id.homebase.homebasekmppoc.prototype.lib.drives.files.HomebaseFile
import id.homebase.homebasekmppoc.prototype.lib.drives.files.PayloadDescriptor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMessageDetailPage(viewModel: ChatMessageDetailViewModel, onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                ChatMessageDetailUiEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Message Detail") },
                        navigationIcon = {
                            IconButton(
                                    onClick = {
                                        viewModel.onAction(ChatMessageDetailUiAction.BackClicked)
                                    }
                            ) { Text("←", style = MaterialTheme.typography.headlineMedium) }
                        }
                )
            }
    ) { paddingValues ->
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(paddingValues)
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
        ) {
            // Show drive and file IDs
            LabeledValue(label = "Drive ID", value = viewModel.driveId.toString())
            LabeledValue(label = "File ID", value = viewModel.fileId.toString())

            Spacer(modifier = Modifier.height(16.dp))

            // Get file header button
            Button(
                    onClick = {
                        viewModel.onAction(ChatMessageDetailUiAction.GetFileHeaderClicked)
                    },
                    enabled = !uiState.isLoading
            ) { Text(if (uiState.isLoading) "Loading..." else "Get File Header") }

            Spacer(modifier = Modifier.height(16.dp))

            // Loading indicator
            if (uiState.isLoading) {
                CircularProgressIndicator()
            }

            // Error message
            uiState.error?.let { error ->
                Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // File header panel
            uiState.header?.let { header ->
                ChatFileHeaderPanel(
                        header = header,
                        payloadBytes = uiState.payloads,
                        expandedPayloadKey = uiState.expandedPayloadKey,
                        onViewPayload = { key ->
                            viewModel.onAction(ChatMessageDetailUiAction.ViewPayloadClicked(key))
                        }
                )
            }
        }
    }
}

@Composable
fun ChatFileHeaderPanel(
        header: HomebaseFile,
        payloadBytes: Map<String, BytesResponse>,
        expandedPayloadKey: String?,
        onViewPayload: (key: String) -> Unit = {}
) {
    Column {
        Text(text = "File Header", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(12.dp))

        // Core metadata
        LabeledValue("State", header.fileState.toString())
        LabeledValue("Created", header.fileMetadata.created.toString())
        LabeledValue("Updated", header.fileMetadata.updated.toString())
        LabeledValue("Encrypted", header.fileMetadata.isEncrypted.toString())
        LabeledValue("Sender", header.fileMetadata.senderOdinId ?: "—")

        // App Data content
        header.fileMetadata.appData.content?.let { content ->
            Spacer(Modifier.height(12.dp))
            Text(text = "App Data Content", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(text = content, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(16.dp))

        // Payloads
        Text(text = "Payloads", style = MaterialTheme.typography.titleSmall)

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
                PayloadSection(
                        payload = payload,
                        payloadBytes = payloadBytes,
                        expandedPayloadKey = expandedPayloadKey,
                        onViewPayload = onViewPayload
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PayloadSection(
        payload: PayloadDescriptor,
        payloadBytes: Map<String, BytesResponse>,
        expandedPayloadKey: String?,
        onViewPayload: (key: String) -> Unit
) {
    val isExpanded = expandedPayloadKey == payload.key
    val bytes = payloadBytes[payload.key]

    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                    text = "Key: ${payload.key}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
            )

            Button(onClick = { onViewPayload(payload.key) }) {
                Text(if (isExpanded) "Hide payload" else "View payload")
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).padding(12.dp)) {
                when {
                    bytes == null -> {
                        Text(
                                text = "Loading payload…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        // Try to display as text
                        val textContent =
                                try {
                                    bytes.bytes.decodeToString()
                                } catch (e: Exception) {
                                    "[Binary data: ${bytes.bytes.size} bytes]"
                                }
                        Text(text = textContent, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column {
        Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
    }
}
