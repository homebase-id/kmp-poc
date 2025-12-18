package id.homebase.homebasekmppoc.prototype.ui.driveUpload

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Drive Upload screen - dumb composable. Takes state and action callback, contains no logic.
 *
 * @param state Current UI state
 * @param onAction Callback for user actions
 * @param onNavigateBack Callback to navigate back
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveUploadScreen(
        state: DriveUploadUiState,
        onAction: (DriveUploadUiAction) -> Unit,
        onNavigateBack: () -> Unit
) {
    val isAnyOperationInProgress =
            state.isUploadingText || state.isUploadingImage || state.isPickingImage

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Drive Upload") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Text("←", style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                )
            }
    ) { paddingValues ->
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(paddingValues)
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Text Post Upload Section
            Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Text Post Upload", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    state.postContent?.let { post ->
                        Text(
                                text = "Caption: ${post.caption}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                            onClick = { onAction(DriveUploadUiAction.UploadTextPostClicked) },
                            enabled = !isAnyOperationInProgress
                    ) {
                        if (state.isUploadingText) {
                            CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (state.isUploadingText) "Uploading..." else "Upload Text Post")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Image Upload Section
            Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Image Upload", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Image picker button
                    Button(
                            onClick = { onAction(DriveUploadUiAction.PickImageClicked) },
                            enabled = !isAnyOperationInProgress
                    ) {
                        if (state.isPickingImage) {
                            CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (state.isPickingImage) "Picking..." else "Pick Image from Gallery")
                    }

                    // Show selected image info
                    state.selectedImageName?.let { name ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                                text =
                                        "Selected: $name (${state.selectedImageBytes?.size ?: 0} bytes)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Upload image button
                    Button(
                            onClick = { onAction(DriveUploadUiAction.UploadImageClicked) },
                            enabled = state.selectedImageBytes != null && !isAnyOperationInProgress
                    ) {
                        if (state.isUploadingImage) {
                            CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (state.isUploadingImage) "Uploading..." else "Upload Image")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Result display
            state.uploadResult?.let { result ->
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                ) {
                    Text(
                            text = result,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Error display
            state.errorMessage?.let { error ->
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                ) {
                    Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
