package id.homebase.homebasekmppoc.prototype.lib.chat

import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import id.homebase.homebasekmppoc.prototype.lib.database.QueryBatch
import id.homebase.homebasekmppoc.prototype.lib.database.QueryBatchResult
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchSortField
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchSortOrder
import id.homebase.homebasekmppoc.prototype.lib.drives.SharedSecretEncryptedFileHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.files.RecipientTransferHistoryEntry
import id.homebase.homebasekmppoc.prototype.lib.drives.files.RecipientTransferSummary
import id.homebase.homebasekmppoc.prototype.lib.drives.files.TransferStatus
import id.homebase.homebasekmppoc.prototype.lib.drives.query.QueryBatchCursor
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/** Chat message file type constant */
const val CHAT_MESSAGE_FILE_TYPE = 7878

/** Archival status indicating a deleted chat */
const val ChatDeletedArchivalStatus = 2

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

/** Data class representing a chat message */
@Serializable
data class ChatMessageContent(
        /** Optional reply ID if this message is a reply to another message */
        val replyId: String? = null,

        /** Content of the message - can be a simple string or rich text */
        val message: String = "",

        /** Delivery status of the message (as int value) */
        val deliveryStatus: Int = ChatDeliveryStatus.Sent.value,

        /** Whether the message has been edited */
        val isEdited: Boolean = false
) {
        /** Get the delivery status as enum */
        fun getDeliveryStatusEnum(): ChatDeliveryStatus? =
                ChatDeliveryStatus.fromValue(deliveryStatus)
}

/**
 * Converts a recipient's transfer history entry to a ChatDeliveryStatus. Uses the existing
 * RecipientTransferHistoryEntry from the drives package.
 *
 * @param transferHistory The transfer history entry for a single recipient
 * @return The corresponding ChatDeliveryStatus
 */
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

/**
 * Builds a ChatDeliveryStatus from the transfer summary across all recipients. Uses the existing
 * RecipientTransferSummary from the drives package.
 *
 * @param recipientCount The total number of recipients
 * @param transferSummary The aggregated transfer summary
 * @return The corresponding ChatDeliveryStatus
 */
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
 * Helper class for fetching chat message files from the local database.
 *
 * This class wraps QueryBatch to provide convenient methods for querying chat message files
 * (fileType 7878) from the drive.
 *
 * @param identityId The identity UUID for the current user
 */
class ChatMessage(private val identityId: Uuid) {

        private val queryBatch = QueryBatch(identityId)

        /**
         * Fetches messages for a specific conversation from the local database.
         *
         * @param dbm The DatabaseManager instance
         * @param driveId The drive ID (alias) to query
         * @param conversationId The conversation ID (groupId) to filter messages
         * @param limit Maximum number of messages to return
         * @param cursor Optional cursor for pagination
         * @param sortOrder Order of results (default: newest first)
         * @param sortField Field to sort by (default: created date)
         * @return QueryBatchResult containing message file headers
         */
        suspend fun fetchMessages(
                dbm: DatabaseManager,
                driveId: Uuid,
                conversationId: Uuid,
                limit: Int = 1000,
                cursor: QueryBatchCursor? = null,
                sortOrder: QueryBatchSortOrder = QueryBatchSortOrder.NewestFirst,
                sortField: QueryBatchSortField = QueryBatchSortField.CreatedDate
        ): QueryBatchResult {
                return queryBatch.queryBatchAsync(
                        dbm = dbm,
                        driveId = driveId,
                        noOfItems = limit,
                        cursor = cursor,
                        sortOrder = sortOrder,
                        sortField = sortField,
                        fileSystemType = 0,
                        filetypesAnyOf = listOf(CHAT_MESSAGE_FILE_TYPE),
                        groupIdAnyOf = listOf(conversationId)
                )
        }

        /**
         * Fetches all messages (without conversation filter) from the local database.
         *
         * @param dbm The DatabaseManager instance
         * @param driveId The drive ID (alias) to query
         * @param limit Maximum number of messages to return
         * @param cursor Optional cursor for pagination
         * @param sortOrder Order of results (default: newest first)
         * @param sortField Field to sort by (default: created date)
         * @return QueryBatchResult containing message file headers
         */
        suspend fun fetchAllMessages(
                dbm: DatabaseManager,
                driveId: Uuid,
                limit: Int = 1000,
                cursor: QueryBatchCursor? = null,
                sortOrder: QueryBatchSortOrder = QueryBatchSortOrder.NewestFirst,
                sortField: QueryBatchSortField = QueryBatchSortField.CreatedDate
        ): QueryBatchResult {
                return queryBatch.queryBatchAsync(
                        dbm = dbm,
                        driveId = driveId,
                        noOfItems = limit,
                        cursor = cursor,
                        sortOrder = sortOrder,
                        sortField = sortField,
                        fileSystemType = 0,
                        filetypesAnyOf = listOf(CHAT_MESSAGE_FILE_TYPE)
                )
        }

        /**
         * Fetches a single message by its unique ID.
         *
         * @param dbm The DatabaseManager instance
         * @param driveId The drive ID (alias) to query
         * @param uniqueId The unique ID of the message
         * @return The message file header, or null if not found
         */
        suspend fun fetchMessageByUniqueId(
                dbm: DatabaseManager,
                driveId: Uuid,
                uniqueId: Uuid
        ): SharedSecretEncryptedFileHeader? {
                val result =
                        queryBatch.queryBatchAsync(
                                dbm = dbm,
                                driveId = driveId,
                                noOfItems = 1,
                                cursor = null,
                                sortOrder = QueryBatchSortOrder.NewestFirst,
                                sortField = QueryBatchSortField.CreatedDate,
                                fileSystemType = 0,
                                filetypesAnyOf = listOf(CHAT_MESSAGE_FILE_TYPE),
                                uniqueIdAnyOf = listOf(uniqueId)
                        )
                return result.records.firstOrNull()
        }

        /**
         * Fetches messages filtered by archival status. Useful for filtering out deleted messages
         * (archivalStatus = 2).
         *
         * @param dbm The DatabaseManager instance
         * @param driveId The drive ID (alias) to query
         * @param conversationId The conversation ID (groupId) to filter messages
         * @param archivalStatusAnyOf List of archival statuses to include
         * @param limit Maximum number of messages to return
         * @param cursor Optional cursor for pagination
         * @return QueryBatchResult containing message file headers
         */
        suspend fun fetchMessagesByArchivalStatus(
                dbm: DatabaseManager,
                driveId: Uuid,
                conversationId: Uuid,
                archivalStatusAnyOf: List<Int>,
                limit: Int = 1000,
                cursor: QueryBatchCursor? = null
        ): QueryBatchResult {
                return queryBatch.queryBatchAsync(
                        dbm = dbm,
                        driveId = driveId,
                        noOfItems = limit,
                        cursor = cursor,
                        sortOrder = QueryBatchSortOrder.NewestFirst,
                        sortField = QueryBatchSortField.CreatedDate,
                        fileSystemType = 0,
                        filetypesAnyOf = listOf(CHAT_MESSAGE_FILE_TYPE),
                        groupIdAnyOf = listOf(conversationId),
                        archivalStatusAnyOf = archivalStatusAnyOf
                )
        }

        /**
         * Fetches non-deleted messages for a conversation. This excludes messages with
         * ChatDeletedArchivalStatus (2).
         *
         * @param dbm The DatabaseManager instance
         * @param driveId The drive ID (alias) to query
         * @param conversationId The conversation ID (groupId) to filter messages
         * @param limit Maximum number of messages to return
         * @param cursor Optional cursor for pagination
         * @return QueryBatchResult containing message file headers
         */
        suspend fun fetchActiveMessages(
                dbm: DatabaseManager,
                driveId: Uuid,
                conversationId: Uuid,
                limit: Int = 1000,
                cursor: QueryBatchCursor? = null
        ): QueryBatchResult {
                // Fetch messages with archival status 0 (active) or 1 (archived but not deleted)
                return queryBatch.queryBatchAsync(
                        dbm = dbm,
                        driveId = driveId,
                        noOfItems = limit,
                        cursor = cursor,
                        sortOrder = QueryBatchSortOrder.NewestFirst,
                        sortField = QueryBatchSortField.CreatedDate,
                        fileSystemType = 0,
                        filetypesAnyOf = listOf(CHAT_MESSAGE_FILE_TYPE),
                        groupIdAnyOf = listOf(conversationId),
                        archivalStatusAnyOf = listOf(0, 1)
                )
        }
}
