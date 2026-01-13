package id.homebase.homebasekmppoc.prototype.ui.driveUpload

import id.homebase.homebasekmppoc.lib.image.createThumbnails
import co.touchlab.kermit.Logger as KLogger
import id.homebase.homebasekmppoc.prototype.lib.drives.TargetDrive
import id.homebase.homebasekmppoc.prototype.lib.drives.files.PayloadFile
import id.homebase.homebasekmppoc.prototype.lib.drives.files.ThumbnailFile
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.DriveUploadProvider
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.EmbeddedThumb
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.FileIdFileIdentifier
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.PayloadDeleteKey
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.PostContent
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.PostType
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.PriorityOptions
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.ScheduleOptions
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.SendContents
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.StorageOptions
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.TransitOptions
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UpdateLocalInstructionSet
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UpdateFileResult
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UploadAppFileMetaData
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UploadFileMetadata
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UploadInstructionSet
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.CreateFileResult
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UploadFileRequest
import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
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
class DriveUploadService(private val driveUploadProvider: DriveUploadProvider) {

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

        val instructions =
            UploadInstructionSet(storageOptions = StorageOptions(driveId = driveId))

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
            instructions = instructions,
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

        val instructions =
            UploadInstructionSet(
                storageOptions = StorageOptions(driveId = driveId),
                transitOptions = TransitOptions.withoutNotifications(
                    recipients = emptyList(),
                    isTransient = false,
                    schedule = ScheduleOptions.SendNowAwaitResponse,
                    priority = PriorityOptions.Medium,
                    sendContents = SendContents.All,
                )
            )


        val post = createSamplePostContent();
        val contentJson = OdinSystemSerializer.serialize(post)
        val (imageSize, previewThumb, thumbnails) = createThumbnails(
            filePath,
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
            instructions = instructions,
            metadata = metadata,
            payloads = payloads,
            thumbnails,
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
        payloads: List<PayloadFile>? = null,
        thumbnails: List<ThumbnailFile>? = null,
        encrypt: Boolean = true,
        onVersionConflict: (suspend () -> CreateFileResult?)? = null
    ): CreateFileResult? {
        val instructions = UploadInstructionSet(storageOptions = StorageOptions(driveId = driveId))

        if (encrypt) {
            throw NotImplementedError("need to handle encryption")
        }

        val request = UploadFileRequest(
            instructions = instructions,
            metadata = metadata,
            payloads = payloads,
            thumbnails = thumbnails,
        )
        return driveUploadProvider.uploadFile(
            request,
            onVersionConflict = onVersionConflict
        )
    }

    /**
     * Updates an existing file.
     *
     * @param targetDrive The target drive
     * @param fileId The file ID to update
     * @param versionTag The current version tag of the file
     * @param keyHeader The key header (encrypted or decrypted)
     * @param metadata The new file metadata
     * @param payloads Optional list of payload files to add/update
     * @param thumbnails Optional list of thumbnail files
     * @param toDeletePayloads Optional list of payloads to delete
     * @param onVersionConflict Optional callback for version conflict handling
     * @return The update result
     */
    suspend fun updateFile(
        targetDrive: TargetDrive,
        fileId: String,
        versionTag: String,
        keyHeader: Any?,
        metadata: UploadFileMetadata,
        payloads: List<PayloadFile>? = null,
        thumbnails: List<ThumbnailFile>? = null,
        toDeletePayloads: List<PayloadDeleteKey>? = null,
        onVersionConflict: (suspend () -> UpdateFileResult?)? = null
    ): UpdateFileResult? {
        val fileIdentifier = FileIdFileIdentifier(fileId = fileId, targetDrive = targetDrive)

        val instructions = UpdateLocalInstructionSet(versionTag = versionTag, file = fileIdentifier)

        return driveUploadProvider.patchFile(
            keyHeader = keyHeader,
            instructions = instructions,
            metadata = metadata,
            payloads = payloads,
            thumbnails = thumbnails,
            toDeletePayloads = toDeletePayloads,
            onVersionConflict = onVersionConflict
        )
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
