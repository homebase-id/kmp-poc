@file:OptIn(ExperimentalUuidApi::class)

package id.homebase.homebasekmppoc.drives

import id.homebase.homebasekmppoc.core.time.UnixTimeUtc
import id.homebase.homebasekmppoc.core.time.UnixTimeUtcRange
import id.homebase.homebasekmppoc.serialization.UuidSerializer
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Flattened query batch request for HTTP API endpoints
 *
 * Ported from C# Odin.Services.Drives.GetQueryBatchRequest
 */
@Serializable
data class GetQueryBatchRequest(
    // FileQueryParams fields
    @Serializable(with = UuidSerializer::class)
    val alias: Uuid,
    @Serializable(with = UuidSerializer::class)
    val type: Uuid,
    val fileType: List<Int>? = null,
    val dataType: List<Int>? = null,
    val fileState: List<FileState>? = null,
    val archivalStatus: List<Int>? = null,
    val sender: List<String>? = null,
    val groupId: List<@Serializable(with = UuidSerializer::class) Uuid>? = null,
    val userDateStart: Long? = null,
    val userDateEnd: Long? = null,
    val clientUniqueIdAtLeastOne: List<@Serializable(with = UuidSerializer::class) Uuid>? = null,
    val tagsMatchAtLeastOne: List<@Serializable(with = UuidSerializer::class) Uuid>? = null,
    val tagsMatchAll: List<@Serializable(with = UuidSerializer::class) Uuid>? = null,
    val localTagsMatchAll: List<@Serializable(with = UuidSerializer::class) Uuid>? = null,
    val localTagsMatchAtLeastOne: List<@Serializable(with = UuidSerializer::class) Uuid>? = null,
    val globalTransitId: List<@Serializable(with = UuidSerializer::class) Uuid>? = null,

    // QueryBatchResultOptionsRequest fields
    val cursorState: String? = null,

    /**
     * Max number of records to return
     */
    val maxRecords: Int = 100,

    /**
     * Specifies if the result set includes the metadata header (assuming the file has one)
     */
    val includeMetadataHeader: Boolean = false,
    val includeTransferHistory: Boolean = false,
    val ordering: QueryBatchSortOrder? = null,
    val sorting: QueryBatchSortField? = null
) {
    fun toQueryBatchRequest(): QueryBatchRequest {
        return QueryBatchRequest(
            queryParams = FileQueryParams(
                targetDrive = TargetDrive(
                    alias = this.alias,
                    type = this.type
                ),
                fileType = this.fileType,
                dataType = this.dataType,
                fileState = this.fileState,
                archivalStatus = this.archivalStatus,
                sender = this.sender,
                groupId = this.groupId,
                userDate = if (this.userDateStart != null && this.userDateEnd != null) {
                    UnixTimeUtcRange(
                        UnixTimeUtc(this.userDateStart),
                        UnixTimeUtc(this.userDateEnd)
                    )
                } else null,
                clientUniqueIdAtLeastOne = this.clientUniqueIdAtLeastOne,
                tagsMatchAtLeastOne = this.tagsMatchAtLeastOne,
                tagsMatchAll = this.tagsMatchAll,
                localTagsMatchAtLeastOne = this.localTagsMatchAtLeastOne,
                localTagsMatchAll = this.localTagsMatchAll,
                globalTransitId = this.globalTransitId
            ),
            resultOptionsRequest = QueryBatchResultOptionsRequest(
                cursorState = this.cursorState,
                maxRecords = this.maxRecords,
                includeMetadataHeader = this.includeMetadataHeader,
                includeTransferHistory = this.includeTransferHistory,
                ordering = this.ordering,
                sorting = this.sorting
            )
        )
    }
}
