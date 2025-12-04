package id.homebase.homebasekmppoc.prototype.ui.video

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun VideoPlayer(
    videoData: ByteArray,
    modifier: Modifier
) {
    val context = LocalContext.current

    // Create a temporary file from the byte array
    val videoFile = remember(videoData) {
        try {
            val file = File.createTempFile("video", ".mp4", context.cacheDir)
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (videoFile != null) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.fromFile(videoFile))
                        setOnPreparedListener { mediaPlayer ->
                            mediaPlayer.isLooping = true
                            mediaPlayer.start()
                        }
                        setOnErrorListener { _, what, extra ->
                            // Handle error
                            true
                        }
                    }
                },
                modifier = Modifier.matchParentSize()
            )
        }
    }
}
