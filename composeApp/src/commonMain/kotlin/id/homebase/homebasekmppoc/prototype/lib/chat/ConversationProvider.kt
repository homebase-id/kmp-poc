package id.homebase.homebasekmppoc.prototype.lib.chat

import id.homebase.homebasekmppoc.prototype.lib.core.BatchResult
import id.homebase.homebasekmppoc.prototype.lib.core.time.UnixTimeUtc
import id.homebase.homebasekmppoc.prototype.lib.crypto.HashUtil
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import id.homebase.homebasekmppoc.prototype.lib.database.QueryBatch
import id.homebase.homebasekmppoc.prototype.lib.drives.FileState
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchSortField
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchSortOrder
import id.homebase.homebasekmppoc.prototype.lib.drives.HomebaseFile
import id.homebase.homebasekmppoc.prototype.lib.drives.files.PayloadDescriptor
import id.homebase.homebasekmppoc.prototype.lib.drives.files.ThumbnailDescriptor
import id.homebase.homebasekmppoc.prototype.lib.drives.query.QueryBatchCursor
import id.homebase.homebasekmppoc.prototype.lib.http.OdinClient
import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/** Chat conversation file type constant */
const val CHAT_CONVERSATION_FILE_TYPE = 8888

/** The conversation ID for conversations with yourself */
const val ConversationWithYourselfId = "e4ef2382-ab3c-405d-a8b5-ad3e09e980dd"

/** Base conversation interface containing common properties */
interface BaseConversation {
    val title: String
}

/** Unified conversation content data class (parsed from JSON) */
@Serializable
data class UnifiedConversation(
    override val title: String = "",
    val recipients: List<String> = emptyList()
) : BaseConversation

/**
 * Metadata stored locally for a conversation (from localAppData). Contains local-only data like
 * last read time.
 */
@Serializable
data class ConversationMetadata(
    /** The conversation ID this metadata belongs to */
    val conversationId: String? = null,

    /** Timestamp when the conversation was last read (UnixTimeUtc in milliseconds) */
    val lastReadTime: Long? = null,

    val lastMessage: ConversationLastMessageContent? = null,
    //TODO: Discuss how to generate unread count
)

/**
 * Complete conversation data model with all fields. This is the domain model returned by
 * ConversationProvider with decrypted content.
 */
@Serializable
data class ConversationData(
    /** FileType of conversation (8888) */
    val fileType: Int = CHAT_CONVERSATION_FILE_TYPE,

    /** FileId of the conversation */
    val fileId: Uuid?,

    /** Unique ID of the conversation */
    val uniqueId: Uuid?,

    /** When the conversation was created */
    val created: UnixTimeUtc,

    /** When the conversation was updated */
    val updated: UnixTimeUtc,

    /** Decrypted conversation content */
    val content: UnifiedConversation,

    /** Decrypted local metadata (from localAppData) */
    val conversationMeta: ConversationMetadata?,

    /** Preview thumbnail 20x20 */
    val previewThumbnail: ThumbnailDescriptor?,

    /** FileState */
    val fileState: FileState,

    /** Whether content is encrypted */
    val isEncrypted: Boolean,

    /** DriveId for reference */
    val driveId: Uuid,

    /** VersionTag */
    val versionTag: Uuid?,

    /** List of payload descriptors with metadata */
    val payloads: List<PayloadDescriptor>?
) {
    /** Returns true if this conversation is with yourself */
    fun isConversationWithYourself(): Boolean {
        val conversationId = conversationMeta?.conversationId ?: return false
        return conversationId == ConversationWithYourselfId
    }

    fun getLastMessageContent(): ConversationLastMessageContent? {
        return conversationMeta?.lastMessage
    }

        /** Get lastReadTime as UnixTimeUtc */

        fun getLastReadTimeUtc(): UnixTimeUtc? = conversationMeta?.lastReadTime?.let { UnixTimeUtc(it) }

    fun getProfilePictureUrl(): String? {
        // For simplicity, use the first recipient's profile picture
        val firstRecipient = content.recipients.firstOrNull() ?: return null
        return "https://${firstRecipient}/pub/image"
    }

}

/**
 * Provider class for fetching and decrypting conversations from the local database.
 *
 * This class wraps QueryBatch to provide convenient methods for querying conversation files
 * (fileType 8888) from the drive, with automatic content decryption.
 *
 * @param identityId The identity UUID for the current user
 * @param odinClient The OdinClient for obtaining shared secret for decryption
 */
@OptIn(ExperimentalEncodingApi::class)
class ConversationProvider(private val identityId: Uuid, private val odinClient: OdinClient) {
    private val queryBatch = QueryBatch(identityId)

    /**
     * Creates a deterministic conversation ID for a list of recipients.
     *
     * - If the list has exactly 1 recipient, generates a deterministic ID using XOR of the
     * logged-in identity and the recipient identity (for 1:1 chats).
     * - Otherwise, generates a random UUID (for group chats or self-conversations).
     *
     * @param recipients List of recipient identity strings
     * @return A UUID representing the conversation
     * @throws IllegalStateException if logged-in identity is not available for 1:1 chats
     */
    suspend fun createConversationId(recipients: List<String>): Uuid {
        return if (recipients.size == 1) {
            val loggedInIdentity =
                odinClient.getLoggedInIdentity()
                    ?: throw IllegalStateException(
                        "Logged-in identity not available"
                    )
            HashUtil.getNewXorId(loggedInIdentity, recipients.first())
        } else {
            Uuid.random()
        }
    }

    /**
     * Fetches conversations from the local database. Content is automatically decrypted using
     * the shared secret from OdinClient.
     */
    suspend fun fetchConversations(
        dbm: DatabaseManager,
        driveId: Uuid,
        limit: Int = 1000,
        cursor: QueryBatchCursor? = null
    ): BatchResult<ConversationData> {
        val result =
            queryBatch.queryBatchAsync(
                dbm = dbm,
                driveId = driveId,
                noOfItems = limit,
                cursor = cursor,
                sortOrder = QueryBatchSortOrder.NewestFirst,
                sortField = QueryBatchSortField.AnyChangeDate, // Sort by modified
                fileSystemType = 0,
                filetypesAnyOf = listOf(CHAT_CONVERSATION_FILE_TYPE)
            )
        return BatchResult(
            records = result.records.map { mapToConversationData(it) },
            hasMoreRows = result.hasMoreRows,
            cursor = result.cursor
        )
    }

    /** Maps a SharedSecretEncryptedFileHeader to ConversationData with decrypted content. */
    private suspend fun mapToConversationData(
        header: HomebaseFile
    ): ConversationData {
        val metadata = header.fileMetadata
        val appData = metadata.appData

        // Decrypt appData content if encrypted
        val decryptedContent = header.fileMetadata.appData.content

        // Parse the content as UnifiedConversation
        val parsedContent =
            decryptedContent?.let { parseConversationContent(it) }
                ?: UnifiedConversation()

        // Parse localAppData content for ConversationMetadata
        var conversationMeta: ConversationMetadata? = null
        if (!header.fileMetadata.localAppData?.content.isNullOrEmpty()) {
            conversationMeta = parseConversationMetadata(header.fileMetadata.localAppData.content)
        }

        return ConversationData(
            fileType = appData.fileType ?: CHAT_CONVERSATION_FILE_TYPE,
            fileId = header.fileId,
            uniqueId = appData.uniqueId,
            created = metadata.created,
            updated = metadata.updated,
            content = parsedContent,
            conversationMeta = conversationMeta,
            previewThumbnail = appData.previewThumbnail,
            fileState = header.fileState,
            isEncrypted = metadata.isEncrypted,
            driveId = header.driveId,
            versionTag = metadata.versionTag,
            payloads = metadata.payloads
        )
    }

    /** Parses a JSON string as UnifiedConversation. */
    private fun parseConversationContent(content: String): UnifiedConversation? {
        return try {
            OdinSystemSerializer.deserialize<UnifiedConversation>(content)
        } catch (e: Exception) {
            // If parsing fails, create a simple conversation with raw content as title
            try {
                UnifiedConversation(title = content.take(100))
            } catch (e2: Exception) {
                null
            }
        }
    }

    /** Parses a JSON string as ConversationMetadata. */
    private fun parseConversationMetadata(content: String): ConversationMetadata? {
        return try {
            OdinSystemSerializer.deserialize<ConversationMetadata>(content)
        } catch (e: Exception) {
            println(
                "ConversationProvider: Wrong IV? Failed to parse ConversationMetadata: ${e.message}\nContent: [${content}]"
            )
            null
        }
    }
}
