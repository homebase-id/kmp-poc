package id.homebase.homebasekmppoc.prototype.ui.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.prototype.lib.authentication.AuthState
import id.homebase.homebasekmppoc.prototype.lib.drives.SharedSecretEncryptedFileHeader
import id.homebase.homebasekmppoc.prototype.lib.http.PayloadPlayground
import id.homebase.homebasekmppoc.prototype.ui.video.VideoPlayer

/**
 * A video player page that displays video content.
 *
 * @param videoHeader The video header containing metadata
 * @param onBack Callback to navigate back to the previous page
 * @param modifier Optional modifier for the page
 */
@Composable
fun OwnerVideoPlayer(
    authenticatedState: AuthState.Authenticated?,
    videoHeader: SharedSecretEncryptedFileHeader,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var videoBytes by remember { mutableStateOf<ByteArray?>(null) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(authenticatedState != null) }

    LaunchedEffect(authenticatedState, videoHeader) {
        if (authenticatedState != null) {
            isLoading = true
            try {
                val payloadPlayground = PayloadPlayground(authenticatedState)
                videoBytes = payloadPlayground.getVideo(videoHeader)
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error"
                Logger.e(e) { "$errorMessage" }
                isLoading = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Video Player",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Video ID: ${videoHeader.fileId}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Video player section
        when {
            isLoading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Loading video...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            errorMessage != null -> {
                Text(
                    text = "Error: $errorMessage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
            videoBytes != null -> {
                VideoPlayer(
                    videoData = videoBytes!!,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> {
                Text(
                    text = "No video data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onBack) {
            Text("Back")
        }
    }
}
