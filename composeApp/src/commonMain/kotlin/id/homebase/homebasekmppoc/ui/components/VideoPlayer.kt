package id.homebase.homebasekmppoc.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific video player component.
 *
 * @param videoData The video data as a byte array
 * @param modifier Optional modifier for the video player
 */
@Composable
expect fun VideoPlayer(
    videoData: ByteArray,
    modifier: Modifier = Modifier
)
