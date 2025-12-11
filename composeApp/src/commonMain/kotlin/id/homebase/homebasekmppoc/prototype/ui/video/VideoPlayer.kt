package id.homebase.homebasekmppoc.prototype.ui.video

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import id.homebase.homebasekmppoc.prototype.lib.video.LocalVideoServer

/**
 * Platform-specific video player component.
 *
 */
@Composable
expect fun VideoPlayer(
    videoUrl: String?,
    modifier: Modifier = Modifier
)
