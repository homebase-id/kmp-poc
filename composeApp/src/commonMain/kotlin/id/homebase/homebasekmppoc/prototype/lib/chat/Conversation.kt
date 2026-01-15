package id.homebase.homebasekmppoc.prototype.lib.chat

import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import id.homebase.homebasekmppoc.prototype.lib.database.QueryBatch
import id.homebase.homebasekmppoc.prototype.lib.database.QueryBatchResult
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchSortField
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchSortOrder
import id.homebase.homebasekmppoc.prototype.lib.drives.SharedSecretEncryptedFileHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.query.QueryBatchCursor
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

/** Unified conversation data class representing a chat conversation */
@Serializable
data class UnifiedConversation(
        override val title: String = "",
        val recipients: List<String> = emptyList()
) : BaseConversation

/**
 * Helper class for fetching conversation files from the local database.
 *
 * This class wraps QueryBatch to provide convenient methods for querying conversation files
 * (fileType 8888) from the drive.
 *
 * @param identityId The identity UUID for the current user
 */
class Conversation(private val identityId: Uuid) {

        private val queryBatch = QueryBatch(identityId)

        /**
         * Fetches conversations from the local database.
         *
         * @param dbm The DatabaseManager instance
         * @param driveId The drive ID (alias) to query
         * @param limit Maximum number of conversations to return
         * @param cursor Optional cursor for pagination
         * @param sortOrder Order of results (default: newest first)
         * @param sortField Field to sort by (default: created date)
         * @return QueryBatchResult containing conversation file headers
         */
        suspend fun fetchConversations(
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
                        filetypesAnyOf = listOf(CHAT_CONVERSATION_FILE_TYPE)
                )
        }

        /**
         * Fetches a single conversation by its unique ID.
         *
         * @param dbm The DatabaseManager instance
         * @param driveId The drive ID (alias) to query
         * @param uniqueId The unique ID of the conversation
         * @return The conversation file header, or null if not found
         */
        suspend fun fetchConversationByUniqueId(
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
                                filetypesAnyOf = listOf(CHAT_CONVERSATION_FILE_TYPE),
                                uniqueIdAnyOf = listOf(uniqueId)
                        )
                return result.records.firstOrNull()
        }

        /**
         * Fetches conversations filtered by archival status.
         *
         * @param dbm The DatabaseManager instance
         * @param driveId The drive ID (alias) to query
         * @param archivalStatusAnyOf List of archival statuses to include
         * @param limit Maximum number of conversations to return
         * @param cursor Optional cursor for pagination
         * @return QueryBatchResult containing conversation file headers
         */
        suspend fun fetchConversationsByArchivalStatus(
                dbm: DatabaseManager,
                driveId: Uuid,
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
                        filetypesAnyOf = listOf(CHAT_CONVERSATION_FILE_TYPE),
                        archivalStatusAnyOf = archivalStatusAnyOf
                )
        }
}
