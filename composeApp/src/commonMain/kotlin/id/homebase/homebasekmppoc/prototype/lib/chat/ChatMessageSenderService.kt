package id.homebase.homebasekmppoc.prototype.lib.chat

import id.homebase.homebasekmppoc.lib.config.chatTargetDrive
import id.homebase.homebasekmppoc.prototype.lib.base.CredentialsManager
import id.homebase.homebasekmppoc.prototype.lib.crypto.ByteArrayUtil
import id.homebase.homebasekmppoc.prototype.lib.crypto.KeyHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.HomebaseFile
import co.touchlab.kermit.Logger as KLogger
import id.homebase.homebasekmppoc.prototype.lib.drives.files.PayloadFile
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.DriveUploadProvider
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UploadAppFileMetaData
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UploadFileMetadata
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UploadInstructionSet
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.FileUpdateInstructionSet
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.TransitOptions
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UpdateFileByFileIdRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UpdateFileByUniqueIdRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UpdateFileResult
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UpdateLocale
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UpdateManifest
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.UploadFileRequest
import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import id.homebase.homebasekmppoc.prototype.toBase64
import id.homebase.homebasekmppoc.prototype.writeTextToTempFile
import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class SendMessageResult(val fileId: Uuid, val uniqueId: Uuid, val versionTag: Uuid)

data class ImageUploadResult(val fileId: String?, val versionTag: String?)

@Serializable
data class SendChatMessageRequest(
    val messageText: String,
    val recipients: List<String>
)

@OptIn(ExperimentalUuidApi::class)
class ChatMessageSenderService(
    private val driveUploadProvider: DriveUploadProvider,
    private val credentialsManager: CredentialsManager
) {

    companion object {
        private const val TAG = "ChatMessageSenderService"

        // Common file types
        const val CHAT_MESSAGE_FILE_TYPE = 7878
        const val CHAT_MESSAGE_PAYLOAD_KEY = "chat_mbl"
        const val CHAT_LINKS_PAYLOAD_KEY = "chat_links"
    }

    suspend fun sendMessage(message: SendChatMessageRequest): SendMessageResult {

        val driveId = chatTargetDrive.alias

        // do the encryption
        val keyHeader = KeyHeader.newRandom16()

        val contentJson = OdinSystemSerializer.serialize(message)
        val encryptedBytes = keyHeader.encryptDataAes(contentJson.toByteArray())

        val uniqueId = Uuid.random()
        val metadata =
            UploadFileMetadata(
                allowDistribution = true,
                isEncrypted = true,
                appData =
                    UploadAppFileMetaData(
                        uniqueId = uniqueId.toString(),
                        fileType = CHAT_MESSAGE_FILE_TYPE,
                        content = encryptedBytes.toBase64()
                    )
            )


        val request = UploadFileRequest(
            driveId = driveId,
            keyHeader = keyHeader,
            metadata = metadata.encryptContent(keyHeader),
            transitOptions = TransitOptions(
                recipients = message.recipients,
                isTransient = null,
                schedule = null,
                priority = null,
                sendContents = null,
                remoteTargetDrive = null,
                useAppNotification = null,
                appNotificationOptions = null
            ),
        )

        val result = driveUploadProvider.uploadFile(request)
            ?: throw Exception("How did we get here?")

        return SendMessageResult(
            uniqueId = uniqueId,
            fileId = result.fileId,
            versionTag = result.newVersionTag
        )
    }


    sealed interface UpdateTarget {
        data class ByFileId(val fileId: Uuid) : UpdateTarget
        data class ByUniqueId(val uniqueId: Uuid) : UpdateTarget
    }

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
                        fileType = CHAT_MESSAGE_FILE_TYPE,
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
}
