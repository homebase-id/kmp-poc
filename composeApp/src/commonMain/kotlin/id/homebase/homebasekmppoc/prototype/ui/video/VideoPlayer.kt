package id.homebase.homebasekmppoc.prototype.ui.video

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import id.homebase.homebasekmppoc.prototype.lib.video.LocalVideoServer

/**
 * Platform-specific video player component.
 *
 * @param videoData The video data as a byte array
 * @param localVideoServer The local video server for serving content
 * @param modifier Optional modifier for the video player
 */
@Composable
expect fun VideoPlayer(
    videoData: ByteArray,
    localVideoServer: LocalVideoServer,
    modifier: Modifier = Modifier
)
