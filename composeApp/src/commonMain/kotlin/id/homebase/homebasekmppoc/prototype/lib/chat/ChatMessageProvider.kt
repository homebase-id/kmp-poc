package id.homebase.homebasekmppoc.prototype.lib.chat

import id.homebase.homebasekmppoc.prototype.lib.core.BatchResult
import id.homebase.homebasekmppoc.prototype.lib.core.time.UnixTimeUtc
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import id.homebase.homebasekmppoc.prototype.lib.database.QueryBatch
import id.homebase.homebasekmppoc.prototype.lib.drives.FileState
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchSortField
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchSortOrder
import id.homebase.homebasekmppoc.prototype.lib.drives.HomebaseFile
import id.homebase.homebasekmppoc.prototype.lib.drives.files.PayloadDescriptor
import id.homebase.homebasekmppoc.prototype.lib.drives.files.ReactionSummary
import id.homebase.homebasekmppoc.prototype.lib.drives.files.RecipientTransferHistoryEntry
import id.homebase.homebasekmppoc.prototype.lib.drives.files.RecipientTransferSummary
import id.homebase.homebasekmppoc.prototype.lib.drives.files.ThumbnailDescriptor
import id.homebase.homebasekmppoc.prototype.lib.drives.files.TransferStatus
import id.homebase.homebasekmppoc.prototype.lib.drives.query.QueryBatchCursor
import id.homebase.homebasekmppoc.prototype.lib.drives.upload.EmbeddedThumb
import id.homebase.homebasekmppoc.prototype.lib.http.OdinClient
import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/** Chat message file type constant */
const val CHAT_MESSAGE_FILE_TYPE = 7878

/** Archival status indicating a deleted chat */
const val ChatDeletedArchivalStatus = 2

const val CHAT_MESSAGE_PAYLOAD_KEY = "chat_mbl"
const val CHAT_LINKS_PAYLOAD_KEY = "chat_links"

/** Enum representing the delivery status of a chat message */
enum class ChatDeliveryStatus(val value: Int) {
    /** Message is currently being sent; Used for optimistic updates */
    Sending(15),

    /** Message has been sent and delivered to your identity */
    Sent(20),

    /** Message has been delivered to the recipient's inbox */
    Delivered(30),

    /** Message has been read by the recipient */
    Read(40),

    /** Message failed to send to the recipient */
    Failed(50);

    companion object {
        fun fromValue(value: Int): ChatDeliveryStatus? = entries.find { it.value == value }
    }
}

/** Represents a node in rich text content */
@Serializable
data class RichTextNode(
        val type: String? = null,
        val id: String? = null,
        val value: String? = null,
        val text: String? = null,
        val children: List<RichTextNode>? = null
)

/** Type alias for rich text content (array of RichTextNode) */
typealias RichText = List<RichTextNode>



@Serializable
data class ReplyPreview(
    val replyUniqueId: Uuid, // FileId of the message that was replied to
    val authorOdinId: String, // frodo.baggins.demo.rocks
    val message: String, // ~40 chars (IDK how many you use?)
    val previewThumbnail: EmbeddedThumb) // Tiny tiny thumb, can be even smaller than tinyThumb even a 1px color

@Serializable
data class LinkPreview(
    val title: String, // ~?? chars - the title
    val url: String, // ~?? chars - name or identity??
    val description: String, // ~?? chars
    val imageUrl: String?,
    val imageHeight: Int?,
    val imageWidth: Int?,

)
{
    fun getThumbUrl() : String { return "" }
}

@Serializable
data class ConversationLastMessageContent(
    val message: String?,
    val deliveryStatus: ChatDeliveryStatus,
    val sender: String,
    val uniqueId: Uuid,
    val time: UnixTimeUtc,
    val reactionSummary: ReactionSummary?,
)

/** Data class representing chat message content (parsed from JSON) */
@Serializable
data class ChatMessageContent(
        /** Optional reply ID if this message is a reply to another message */
        val replyId: Uuid? = null,
        val replyPreview: ReplyPreview? = null,

        /** Content of the message - can be a simple string or rich text */
        val message: String = "",
        // contentIsComplete is a boolean if true write "more..."
        // TODO: A helper function to load the text when the user presses "more..."

        /** Delivery status of the message (as int value) */
        val deliveryStatus: Int = ChatDeliveryStatus.Sent.value,

        /** Whether the message has been edited */
        val isEdited: Boolean = false
) {
    /** Get the delivery status as enum */
    fun getDeliveryStatusEnum(): ChatDeliveryStatus? = ChatDeliveryStatus.fromValue(deliveryStatus)
}

/**
 * Complete chat message data model with all fields. This is the domain model returned by
 * ChatMessageProvider with decrypted content.
 */
data class ChatMessageData(
        /** FileType of chatMessage (7878) */
        val fileType: Int = CHAT_MESSAGE_FILE_TYPE,

        /** Sender of the message */
        val sender: String?,

        /** FileId of the message - set on server, used as unique ID */
        val fileId: Uuid,

        /** ClientUniqueId - set by the device */
        val uniqueId: Uuid?,

        /** Timestamp when the message was created */
        val created: UnixTimeUtc,

        /** Timestamp when the message was updated */
        val updated: UnixTimeUtc,

        /** GlobalTransitId of the payload - same across all recipients */
        val globalTransitId: Uuid?,

        /** GroupId of the payload (conversationId) */
        val conversationId: Uuid?,

        /** FileState of the Message */
        val fileState: FileState,

        /** Decrypted content of the message */
        val content: ChatMessageContent,

        /** VersionTag indicating when message was last updated */
        val versionTag: Uuid?,

        /** Tiny blurry preview thumbnail of the file */
        val previewThumbnail: ThumbnailDescriptor?,

        /** Whether the content is complete or needs payload download */
        val contentIsComplete: Boolean,

        /** List of payload descriptors with metadata */
        val payloads: List<PayloadDescriptor>?,

        /** DriveId for reference */
        val driveId: String,

        /** Whether content is encrypted */
        val isEncrypted: Boolean,

        val reactionSummary: ReactionSummary?,


) {
    fun getLinkPreview() : LinkPreview? {
        val payload = payloads?.firstOrNull { it.keyEquals(CHAT_LINKS_PAYLOAD_KEY) }
        val appData = payload?.descriptorContent ?: return null

        return try {
            OdinSystemSerializer.deserialize<LinkPreview>(appData)
        } catch (e: Exception) {
            null
        }
    }
}

/** Converts a recipient's transfer history entry to a ChatDeliveryStatus. */
fun transferHistoryToChatDeliveryStatus(
        transferHistory: RecipientTransferHistoryEntry?
): ChatDeliveryStatus {
    if (transferHistory == null) return ChatDeliveryStatus.Failed

    if (transferHistory.latestSuccessfullyDeliveredVersionTag != null) {
        return if (transferHistory.isReadByRecipient) {
            ChatDeliveryStatus.Read
        } else {
            ChatDeliveryStatus.Delivered
        }
    }

    val transferStatus = transferHistory.latestTransferStatus

    if (TransferStatus.isFailedStatus(transferStatus)) {
        return ChatDeliveryStatus.Failed
    }

    return ChatDeliveryStatus.Sent
}

/** Builds a ChatDeliveryStatus from the transfer summary across all recipients. */
fun buildDeliveryStatus(
        recipientCount: Int?,
        transferSummary: RecipientTransferSummary
): ChatDeliveryStatus {
    if (transferSummary.totalFailed > 0) return ChatDeliveryStatus.Failed

    val count = recipientCount ?: 0
    if (transferSummary.totalReadByRecipient >= count) return ChatDeliveryStatus.Read
    if (transferSummary.totalDelivered >= count) return ChatDeliveryStatus.Delivered

    return ChatDeliveryStatus.Sent
}


/**
 * Provider class for fetching and decrypting chat messages from the local database.
 *
 * This class wraps QueryBatch to provide convenient methods for querying chat message files
 * (fileType 7878) from the drive, with automatic content decryption.
 *
 * @param identityId The identity UUID for the current user
 * @param odinClient The OdinClient for obtaining shared secret for decryption
 */
@OptIn(ExperimentalEncodingApi::class)
class ChatMessageProvider(private val identityId: Uuid, private val odinClient: OdinClient) {
    private val queryBatch = QueryBatch(identityId)

    /**
     * Fetches messages for a specific conversation from the local database. Content is
     * automatically decrypted using the shared secret from OdinClient.
     */
    suspend fun fetchMessages(
            dbm: DatabaseManager,
            driveId: Uuid,
            conversationId: Uuid,
            limit: Int = 1000,
            cursor: QueryBatchCursor? = null
    ): BatchResult<ChatMessageData> {
        val result =
                queryBatch.queryBatchAsync(
                        dbm = dbm,
                        driveId = driveId,
                        noOfItems = limit,
                        cursor = cursor,
                        sortOrder = QueryBatchSortOrder.NewestFirst,
                        sortField = QueryBatchSortField.CreatedDate,
                        fileSystemType = 0,
                        filetypesAnyOf = listOf(CHAT_MESSAGE_FILE_TYPE),
                        groupIdAnyOf = listOf(conversationId)
                )
        return BatchResult(
                records = result.records.map { mapToMessageData(it) },
                hasMoreRows = result.hasMoreRows,
                cursor = result.cursor
        )
    }



    /** Maps a SharedSecretEncryptedFileHeader to ChatMessageData with decrypted content. */
    private suspend fun mapToMessageData(header: HomebaseFile): ChatMessageData {
        val metadata = header.fileMetadata
        val appData = metadata.appData

        // Decrypt content if encrypted
        val decryptedContent = header.fileMetadata.appData.content

        // Parse the content as ChatMessageContent
        val parsedContent =
                decryptedContent?.let { parseMessageContent(it) } ?: ChatMessageContent()

        // Get preview thumbnail from appData or first payload
        val previewThumbnail =
                appData.previewThumbnail ?: metadata.payloads?.firstOrNull()?.previewThumbnail

        return ChatMessageData(
                fileType = appData.fileType ?: CHAT_MESSAGE_FILE_TYPE,
                sender = metadata.senderOdinId,
                fileId = header.fileId,
                uniqueId = appData.uniqueId,
                created = metadata.created,
                updated = metadata.updated,
                globalTransitId = metadata.globalTransitId,
                conversationId = appData.groupId,
                fileState = header.fileState,
                content = parsedContent,
                versionTag = metadata.versionTag,
                previewThumbnail = previewThumbnail,
                contentIsComplete = metadata.payloads?.find { it.keyEquals(CHAT_MESSAGE_PAYLOAD_KEY) } == null,
                payloads = metadata.payloads,
                driveId = header.driveId.toString(),
                isEncrypted = metadata.isEncrypted,
                reactionSummary = metadata.reactionPreview

        )
    }

    /** Parses a JSON string as ChatMessageContent. */
    private fun parseMessageContent(content: String): ChatMessageContent? {
        return try {
            OdinSystemSerializer.deserialize<ChatMessageContent>(content)
        } catch (e: Exception) {
            println(
                "ChatProvider: Failed to parse ChatMetadata: ${e.message}\nContent: [${content}]"
            )

            // If parsing fails, create a simple message with the raw content
            try {
                ChatMessageContent(message = content)
            } catch (e2: Exception) {
                null
            }
        }
    }
}
