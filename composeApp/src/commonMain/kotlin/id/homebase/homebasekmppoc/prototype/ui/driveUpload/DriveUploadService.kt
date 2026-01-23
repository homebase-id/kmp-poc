package id.homebase.homebasekmppoc.prototype.ui.driveUpload

import id.homebase.homebasekmppoc.lib.image.createThumbnails
import id.homebase.homebasekmppoc.prototype.lib.base.CredentialsManager
import id.homebase.homebasekmppoc.prototype.lib.crypto.ByteArrayUtil
import id.homebase.homebasekmppoc.prototype.lib.crypto.KeyHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.HomebaseFile
import co.touchlab.kermit.Logger as KLogger
import id.homebase.homebasekmppoc.prototype.lib.drives.files.PayloadFile
import id.homebase.homebasekmppoc.prototype.lib.drives.files.ThumbnailFile
import id.homebase.homebasekmppoc.prototype.lib.drives.readFileBytes
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.DriveUploadProvider
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.EmbeddedThumb
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.PostContent
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.PostType
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.PriorityOptions
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.ScheduleOptions
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.SendContents
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.StorageOptions
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.TransitOptions
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UploadAppFileMetaData
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UploadFileMetadata
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UploadInstructionSet
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.CreateFileResult
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.FileUpdateInstructionSet
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UpdateFileByFileIdRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UpdateFileByUniqueIdRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UpdateFileResult
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UpdateLocale
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UpdateManifest
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UploadFileRequest
import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import id.homebase.homebasekmppoc.prototype.writeTextToTempFile
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Result of a text post upload operation.
 *
 * @param fileId The unique identifier of the uploaded file
 * @param versionTag The version tag of the uploaded file
 */
data class TextPostUploadResult(val fileId: String?, val versionTag: String?)

/**
 * Result of an image upload operation.
 *
 * @param fileId The unique identifier of the uploaded file
 * @param versionTag The version tag of the uploaded file
 */
data class ImageUploadResult(val fileId: String?, val versionTag: String?)

/**
 * Service layer for drive upload operations.
 *
 * This class handles the business logic and abstraction between the ViewModel and
 * DriveUploadProvider. It provides high-level methods for common upload operations like uploading
 * text posts and images.
 *
 * @param driveUploadProvider The underlying provider for drive upload operations
 */
@OptIn(ExperimentalUuidApi::class)
class DriveUploadService(
    private val driveUploadProvider: DriveUploadProvider,
    private val credentialsManager: CredentialsManager
) {

    companion object {
        private const val TAG = "DriveUploadService"

        // Common file types
        const val FILE_TYPE_POST = 101
        const val FILE_TYPE_MEDIA = 101

        // Common data types
        const val DATA_TYPE_POST = 100
        const val DATA_TYPE_IMAGE = 200
    }

    /**
     * Uploads a text post to the drive.
     *
     * @param driveId The target drive to upload to
     * @param postContent The post content to upload
     * @param encrypt Whether to encrypt the file (default true)
     * @return The upload result containing file ID and version tag
     */
    suspend fun uploadTextPost(
        driveId: Uuid,
        postContent: PostContent,
        encrypt: Boolean = false
    ): TextPostUploadResult {
        KLogger.d(TAG) { "Uploading text post with id: ${postContent.id}" }

        val contentJson = OdinSystemSerializer.serialize(postContent)

        val metadata =
            UploadFileMetadata(
                allowDistribution = true,
                isEncrypted = encrypt,
                appData =
                    UploadAppFileMetaData(
                        uniqueId = postContent.id,
                        fileType = FILE_TYPE_POST,
                        dataType = DATA_TYPE_POST,
                        content = contentJson
                    )
            )

        val request = UploadFileRequest(
            driveId = driveId,
            keyHeader = KeyHeader.empty(),
            metadata = metadata
        )

        val result = driveUploadProvider.uploadFile(request)

        KLogger.i(TAG) { "Text post uploaded successfully: ${result?.fileId}" }

        return TextPostUploadResult(
            fileId = result?.fileId.toString(),
            versionTag = result?.newVersionTag.toString()
        )
    }

    /**
     * Uploads an image to the drive.
     *
     * @param driveId The target drive to upload to
     * @param filePath The raw image bytes
     * @param payloadKey The key for the payload (default "pst_mdi")
     * @param uniqueId Optional unique ID for the file (auto-generated if not provided)
     * @param fileType The file type (default: MEDIA file type)
     * @param dataType The data type (default: IMAGE data type)
     * @param encrypt Whether to encrypt the file (default true)
     * @return The upload result containing file ID and version tag
     */
    suspend fun uploadImage(
        driveId: Uuid,
        filePath: String,
        payloadKey: String = "pst_mdia",
        uniqueId: String? = null,
        fileType: Int = FILE_TYPE_MEDIA,
        dataType: Int = DATA_TYPE_IMAGE,
        encrypt: Boolean = false
    ): ImageUploadResult {
        val actualUniqueId = uniqueId ?: Uuid.random().toString()
        KLogger.d(TAG) {
            "Uploading image with uniqueId: $actualUniqueId, file: ${filePath}"
        }

        val post = createSamplePostContent();
        val contentJson = OdinSystemSerializer.serialize(post)
        val imageBytes = readFileBytes(filePath)
        val (imageSize, previewThumb, thumbnails) = createThumbnails(
            imageBytes,
            payloadKey = payloadKey
        );

        val metadata =
            UploadFileMetadata(
                allowDistribution = true,
                isEncrypted = encrypt,
                appData =
                    UploadAppFileMetaData(
                        uniqueId = actualUniqueId,
                        fileType = fileType,
                        dataType = dataType,
                        content = contentJson,
                        previewThumbnail = EmbeddedThumb(
                            content = previewThumb.content,
                            pixelWidth = imageSize.pixelWidth,
                            pixelHeight = imageSize.pixelHeight,
                            contentType = previewThumb.contentType
                        ),

                        )
            )
        val payloads = listOf(
            PayloadFile(
                key = payloadKey,
                filePath = filePath,
                previewThumbnail = previewThumb,
//                contentType = "image/jpeg"
            )
        )

        val request = UploadFileRequest(
            driveId = driveId,
            keyHeader = KeyHeader.empty(),
            metadata = metadata,
            payloads = payloads,
            transitOptions = TransitOptions.withoutNotifications(
                recipients = emptyList(),
                isTransient = false,
                schedule = ScheduleOptions.SendNowAwaitResponse,
                priority = PriorityOptions.Medium,
                sendContents = SendContents.All,
            ),
            thumbnails = thumbnails
        )

//        if (encrypt) {
//            throw NotImplementedError("todo: handle encryption")
//        }

        val result = driveUploadProvider.uploadFile(request)

        KLogger.i(TAG) { "Image uploaded successfully: ${result?.fileId.toString()}" }

        return ImageUploadResult(
            fileId = result?.fileId.toString(),
            versionTag = result?.newVersionTag.toString()
        )
    }

    /**
     * Uploads a file with payloads to the drive.
     *
     * This is a more flexible method that allows specifying payloads and thumbnails.
     *
     * @param targetDrive The target drive to upload to
     * @param metadata The file metadata
     * @param payloads Optional list of payload files
     * @param thumbnails Optional list of thumbnail files
     * @param encrypt Whether to encrypt the file (default true)
     * @param onVersionConflict Optional callback for version conflict handling
     * @return The upload result
     */
    suspend fun uploadFileWithPayloads(
        driveId: Uuid,
        metadata: UploadFileMetadata,
        payloads: List<PayloadFile> = emptyList(),
        thumbnails: List<ThumbnailFile> = emptyList(),
        encrypt: Boolean = true,
        onVersionConflict: (suspend () -> CreateFileResult?)? = null
    ): CreateFileResult? {
        if (encrypt) {
            throw NotImplementedError("need to handle encryption")
        }

        val request = UploadFileRequest(
            driveId = driveId,
            keyHeader = KeyHeader.empty(),
            metadata = metadata,
            payloads = payloads,
            thumbnails = thumbnails,
        )

        return driveUploadProvider.uploadFile(
            request,
            onVersionConflict = onVersionConflict
        )
    }

    sealed interface UpdateTarget {
        data class ByFileId(val fileId: Uuid) : UpdateTarget
        data class ByUniqueId(val uniqueId: Uuid) : UpdateTarget
    }


    /**
     * Updates an existing text post file by replacing / appending payloads.
     *
     * @param driveId The drive containing the file
     * @param fileId The file to update
     * @param newPayloadText The new text content
     */
    suspend fun updateTextPost(
        driveId: Uuid,
        target: UpdateTarget,
        header: HomebaseFile,
        newContentText: String,
        newPayloadText: String,
    ): UpdateFileResult? {
        KLogger.d(TAG) { "Updating text post target=$target" }

        val isEncrypted = header.fileMetadata.isEncrypted
        val versionTag = header.fileMetadata.versionTag

        val credentials = credentialsManager.getActiveCredentials();

        val keyHeader: KeyHeader =
            if (isEncrypted) {
                header.keyHeader
            } else {
                KeyHeader.empty()
            }

        if (isEncrypted) {
            //
            // TODO: need to encrypt the newContentText and newPayloadText
            //
        }

        val payloads =
            listOf(
                PayloadFile(
                    key = "txt_data1",
                    contentType = "text/plain",
                    filePath = writeTextToTempFile(
                        prefix = "payload_",
                        suffix = ".txt",
                        content = newPayloadText
                    )
                )
            )

        val manifest =
            UpdateManifest.build(
                payloads = payloads,
                toDeletePayloads = null,
                thumbnails = null,
                generatePayloadIv = isEncrypted
            )

        val instructions =
            FileUpdateInstructionSet(
                transferIv = ByteArrayUtil.getRndByteArray(16),
                locale = UpdateLocale.Local,
                recipients = emptyList(),
                manifest = manifest,
                useAppNotification = false
            )

        val metadata =
            UploadFileMetadata(
                allowDistribution = true,
                isEncrypted = isEncrypted,
                versionTag = versionTag,
                appData =
                    UploadAppFileMetaData(
                        uniqueId = null,
                        fileType = FILE_TYPE_POST,
                        dataType = DATA_TYPE_POST,
                        content = OdinSystemSerializer.serialize(newContentText)
                    )
            )

        when (target) {
            is UpdateTarget.ByFileId -> {
                val request =
                    UpdateFileByFileIdRequest(
                        driveId = driveId,
                        fileId = target.fileId,
                        keyHeader = keyHeader,
                        instructions = instructions,
                        metadata = metadata,
                        payloads = payloads,
                        thumbnails = null
                    )

                return driveUploadProvider.updateFileByFileId(request)
            }

            is UpdateTarget.ByUniqueId -> {
                val request =
                    UpdateFileByUniqueIdRequest(
                        driveId = driveId,
                        uniqueId = target.uniqueId,
                        keyHeader = keyHeader,
                        instructions = instructions,
                        metadata = metadata,
                        payloads = payloads,
                        thumbnails = null
                    )

                return driveUploadProvider.updateFileByUniqueId(request)
            }
        }

        KLogger.i(TAG) { "Text post updated successfully: target=$target" }
    }


    /**
     * Creates a new PostContent with default values.
     *
     * @param channelId The channel ID for the post
     * @param caption The post caption
     * @param slug The post slug (URL-friendly identifier)
     * @param type The type of post (default: Tweet)
     * @return A new PostContent instance
     */
    fun createPostContent(
        channelId: String = Uuid.random().toString(),
        caption: String,
        slug: String,
        type: PostType = PostType.Tweet
    ): PostContent {
        return PostContent(
            id = Uuid.random().toString(),
            channelId = channelId,
            caption = caption,
            slug = slug,
            type = type,
            reactAccess = null,
            isCollaborative = false,
            captionAsRichText = null,
            primaryMediaFile = null,
            embeddedPost = null,
            sourceUrl = null
        )
    }

    /**
     * Creates a sample PostContent for testing purposes.
     *
     * @return A sample PostContent instance
     */
    fun createSamplePostContent(): PostContent {
        return createPostContent(
            caption = "Test post from KMP POC",
            type = PostType.Tweet,
            slug = "1a1a22557a572da799bfabc7fa1c88c7",
            channelId = "e8475dc4-6cb4-b665-1c2d-0dbd0f3aad5f",
        )
    }
}
