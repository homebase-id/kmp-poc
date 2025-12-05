package id.homebase.homebasekmppoc.prototype.ui.video

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific HLS video player component.
 *
 * @param manifestUrl The HLS manifest URL (m3u8)
 * @param clientAuthToken Optional client auth token for authenticated requests
 * @param modifier Optional modifier for the video player
 */
@Composable
expect fun HlsVideoPlayer(
    manifestUrl: String,
    clientAuthToken: String? = null,
    modifier: Modifier = Modifier
)
