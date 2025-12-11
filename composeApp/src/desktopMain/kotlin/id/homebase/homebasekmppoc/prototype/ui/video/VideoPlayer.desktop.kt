package id.homebase.homebasekmppoc.prototype.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.prototype.lib.video.LocalVideoServer
import java.util.UUID

@Composable
actual fun VideoPlayer(
    videoData: ByteArray,
    localVideoServer: LocalVideoServer,
    modifier: Modifier
) {
    var videoUrl by remember { mutableStateOf<String?>(null) }

    // Register video content with LocalVideoServer
    LaunchedEffect(videoData) {
        try {
            val contentId = "video-${UUID.randomUUID()}"
            localVideoServer.registerContent(
                id = contentId,
                data = videoData,
                contentType = "video/mp4"
            )
            videoUrl = localVideoServer.getContentUrl(contentId)
            Logger.d("VideoPlayer.Desktop") { "Registered video content: $contentId at $videoUrl" }
        } catch (e: Exception) {
            Logger.e("VideoPlayer.Desktop", e) { "Failed to register video content" }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (videoUrl != null) {
            // For now, show a placeholder text
            // To enable video playback on desktop, you would need to add VLC or JavaFX libraries
            Text(
                text = "Video player (Desktop)\nURL: $videoUrl\nSize: ${videoData.size / 1024} KB\n\nTo enable video playback, add VLC or JavaFX dependencies",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Loading video...",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
