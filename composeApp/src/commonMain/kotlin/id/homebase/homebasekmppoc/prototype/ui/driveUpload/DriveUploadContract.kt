package id.homebase.homebasekmppoc.prototype.ui.driveUpload

import id.homebase.homebasekmppoc.prototype.lib.drives.upload.PostContent

/** Single immutable state for Drive Upload screen. */
data class DriveUploadUiState(
        val isUploadingText: Boolean = false,
        val isUploadingImage: Boolean = false,
        val isPickingImage: Boolean = false,
        val uploadResult: String? = null,
        val errorMessage: String? = null,
        val selectedImageBytes: ByteArray? = null,
        val selectedImageName: String? = null,
        val postContent: PostContent? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as DriveUploadUiState
        if (isUploadingText != other.isUploadingText) return false
        if (isUploadingImage != other.isUploadingImage) return false
        if (isPickingImage != other.isPickingImage) return false
        if (uploadResult != other.uploadResult) return false
        if (errorMessage != other.errorMessage) return false
        if (selectedImageBytes != null) {
            if (other.selectedImageBytes == null) return false
            if (!selectedImageBytes.contentEquals(other.selectedImageBytes)) return false
        } else if (other.selectedImageBytes != null) return false
        if (selectedImageName != other.selectedImageName) return false
        if (postContent != other.postContent) return false
        return true
    }

    override fun hashCode(): Int {
        var result = isUploadingText.hashCode()
        result = 31 * result + isUploadingImage.hashCode()
        result = 31 * result + isPickingImage.hashCode()
        result = 31 * result + (uploadResult?.hashCode() ?: 0)
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + (selectedImageBytes?.contentHashCode() ?: 0)
        result = 31 * result + (selectedImageName?.hashCode() ?: 0)
        result = 31 * result + (postContent?.hashCode() ?: 0)
        return result
    }
}

/** All possible user actions on Drive Upload screen. */
sealed interface DriveUploadUiAction {
    /** User wants to pick an image from gallery */
    data object PickImageClicked : DriveUploadUiAction

    /** User wants to upload text post */
    data object UploadTextPostClicked : DriveUploadUiAction

    /** User wants to upload selected image */
    data object UploadImageClicked : DriveUploadUiAction

    /** Image was successfully picked from gallery */
    data class ImagePicked(val bytes: ByteArray, val name: String) : DriveUploadUiAction {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as ImagePicked
            if (!bytes.contentEquals(other.bytes)) return false
            if (name != other.name) return false
            return true
        }
        override fun hashCode(): Int {
            var result = bytes.contentHashCode()
            result = 31 * result + name.hashCode()
            return result
        }
    }

    /** Image picking failed */
    data class ImagePickFailed(val error: String) : DriveUploadUiAction

    /** Image picking was cancelled */
    data object ImagePickCancelled : DriveUploadUiAction
}

/** One-off events for side effects. */
sealed interface DriveUploadUiEvent {
    /** Request to open file picker for images */
    data object OpenImagePicker : DriveUploadUiEvent

    /** Show success toast/snackbar */
    data class ShowSuccess(val message: String) : DriveUploadUiEvent

    /** Show error toast/snackbar */
    data class ShowError(val message: String) : DriveUploadUiEvent
}
