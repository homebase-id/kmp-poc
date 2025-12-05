package id.homebase.homebasekmppoc.prototype.lib.video

import kotlinx.serialization.Serializable

@Serializable
data class VideoMetaData (
    val mimeType: String,
    val isSegmented: Boolean = false,
    val isDescriptorContentComplete: Boolean = true,
    val fileSize: Long? = null,
    val key: String? = null,
    val duration: Double,
    val codec: String? = null,
    val hlsPlaylist: String? = null,
)
