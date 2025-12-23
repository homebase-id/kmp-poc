package id.homebase.homebasekmppoc.prototype.ui.driveFetch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailPage(
    viewModel: FileDetailViewModel,
    onNavigateBack: () -> Unit
) {
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

            Button(
                enabled = !state.isLoading,
                onClick = {
                    viewModel.onAction(FileDetailUiAction.GetFileHeaderClicked)
                }
            ) {
                Text(if (state.isLoading) "Loading file header" else "Get file header")
            }

            Spacer(Modifier.height(24.dp))

            val error = state.error
            val header = state.header

            when {
                error != null -> {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                header != null -> {
                    FileHeaderPanel(
                        header = header,
                        thumbnailBytes = state.thumbnails,
                        onViewPayload = { payloadKey ->
                            // placeholder for now
                        },
                        onGetThumbnail = { payloadKey, width, height ->
                            viewModel.onAction(
                                FileDetailUiAction.GetThumbnailClicked(
                                    payloadKey = payloadKey,
                                    width = width,
                                    height = height
                                )
                            )
                        }
                    )
                }

            }

        }
    }
}

