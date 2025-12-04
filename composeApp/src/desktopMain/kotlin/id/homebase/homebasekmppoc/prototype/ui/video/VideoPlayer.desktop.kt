package id.homebase.homebasekmppoc.prototype.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun VideoPlayer(
    videoData: ByteArray,
    modifier: Modifier
) {
    val videoFile = remember(videoData) {
        try {
            val file = File.createTempFile("video", ".mp4")
            file.deleteOnExit()
            FileOutputStream(file).use { it.write(videoData) }
            file
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(videoFile) {
        onDispose {
            videoFile?.delete()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (videoFile != null) {
            // For now, show a placeholder text
            // To enable video playback on desktop, you would need to add VLC or JavaFX libraries
            Text(
                text = "Video player (Desktop)\nFile: ${videoFile.name}\nSize: ${videoData.size / 1024} KB\n\nTo enable video playback, add VLC or JavaFX dependencies",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Failed to load video",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
