package id.homebase.homebasekmppoc.prototype.ui.driveUpload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.ui.screens.login.feedTargetDrive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Drive Upload screen following strict MVI pattern.
 * - Single state via StateFlow
 * - Single entry point via onAction()
 * - One-off events via Channel
 *
 * Uses DriveUploadService for business logic abstraction.
 */
class DriveUploadViewModel(private val driveUploadService: DriveUploadService?) : ViewModel() {

    private val _uiState = MutableStateFlow(DriveUploadUiState())
    val uiState: StateFlow<DriveUploadUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<DriveUploadUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        // Initialize with a sample post content
        _uiState.update { it.copy(postContent = driveUploadService?.createSamplePostContent()) }
    }

    /** Single entry point for all UI actions. */
    fun onAction(action: DriveUploadUiAction) {
        when (action) {
            is DriveUploadUiAction.PickImageClicked -> handlePickImage()
            is DriveUploadUiAction.UploadTextPostClicked -> handleUploadTextPost()
            is DriveUploadUiAction.UploadImageClicked -> handleUploadImage()
            is DriveUploadUiAction.ImagePicked -> handleImagePicked(action.bytes, action.name)
            is DriveUploadUiAction.ImagePickFailed -> handleImagePickError(action.error)
            is DriveUploadUiAction.ImagePickCancelled -> handleImagePickCancelled()
        }
    }

    private fun handlePickImage() {
        _uiState.update { it.copy(isPickingImage = true, errorMessage = null) }
        sendEvent(DriveUploadUiEvent.OpenImagePicker)
    }

    private fun handleImagePicked(bytes: ByteArray, name: String) {
        Logger.d("DriveUploadViewModel") { "Image picked: $name, size: ${bytes.size}" }
        _uiState.update {
            it.copy(
                    isPickingImage = false,
                    selectedImageBytes = bytes,
                    selectedImageName = name,
                    errorMessage = null
            )
        }
    }

    private fun handleImagePickError(error: String) {
        Logger.e("DriveUploadViewModel") { "Image pick failed: $error" }
        _uiState.update {
            it.copy(isPickingImage = false, errorMessage = "Failed to pick image: $error")
        }
    }

    private fun handleImagePickCancelled() {
        _uiState.update { it.copy(isPickingImage = false) }
    }

    private fun handleUploadTextPost() {
        if (driveUploadService == null) {
            _uiState.update {
                it.copy(errorMessage = "Not authenticated - DriveUploadService unavailable")
            }
            return
        }

        val postContent = _uiState.value.postContent
        if (postContent == null) {
            _uiState.update { it.copy(errorMessage = "No post content configured") }
            return
        }

        _uiState.update {
            it.copy(isUploadingText = true, errorMessage = null, uploadResult = null)
        }

        viewModelScope.launch {
            try {
                val result =
                        driveUploadService.uploadTextPost(
                                targetDrive = feedTargetDrive,
                                postContent = postContent,
                                encrypt = false
                        )

                val successMessage =
                        "Text post uploaded!\nFile ID: ${result.fileId}\nVersion: ${result.versionTag}"
                Logger.i("DriveUploadViewModel") { "Upload successful: ${result.fileId}" }

                _uiState.update { it.copy(isUploadingText = false, uploadResult = successMessage) }
            } catch (e: Exception) {
                Logger.e("DriveUploadViewModel") { "Upload failed: ${e.message}" }
                _uiState.update {
                    it.copy(isUploadingText = false, errorMessage = "Upload failed: ${e.message}")
                }
            }
        }
    }

    private fun handleUploadImage() {
        if (driveUploadService == null) {
            _uiState.update {
                it.copy(errorMessage = "Not authenticated - DriveUploadService unavailable")
            }
            return
        }

        val imageBytes = _uiState.value.selectedImageBytes
        if (imageBytes == null) {
            _uiState.update { it.copy(errorMessage = "No image selected") }
            return
        }

        _uiState.update {
            it.copy(isUploadingImage = true, errorMessage = null, uploadResult = null)
        }

        viewModelScope.launch {
            try {
                val result =
                        driveUploadService.uploadImage(
                                targetDrive = feedTargetDrive,
                                imageBytes = imageBytes,
                                encrypt = true
                        )

                val successMessage =
                        "Image uploaded!\nFile ID: ${result.fileId}\nVersion: ${result.versionTag}"
                Logger.i("DriveUploadViewModel") { "Image upload successful: ${result.fileId}" }

                _uiState.update {
                    it.copy(
                            isUploadingImage = false,
                            uploadResult = successMessage,
                            selectedImageBytes = null,
                            selectedImageName = null
                    )
                }
            } catch (e: Exception) {
                Logger.e("DriveUploadViewModel") { "Image upload failed: ${e.message}" }
                _uiState.update {
                    it.copy(
                            isUploadingImage = false,
                            errorMessage = "Image upload failed: ${e.message}"
                    )
                }
            }
        }
    }

    private fun sendEvent(event: DriveUploadUiEvent) {
        viewModelScope.launch { _uiEvent.send(event) }
    }
}
