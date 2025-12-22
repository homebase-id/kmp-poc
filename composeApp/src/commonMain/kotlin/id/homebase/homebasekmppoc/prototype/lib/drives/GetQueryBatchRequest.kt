@file:OptIn(ExperimentalUuidApi::class)

package id.homebase.homebasekmppoc.prototype.lib.drives

import id.homebase.homebasekmppoc.prototype.lib.core.time.UnixTimeUtc
import id.homebase.homebasekmppoc.prototype.lib.core.time.UnixTimeUtcRange
import id.homebase.homebasekmppoc.prototype.encodeUrl
import id.homebase.homebasekmppoc.prototype.lib.drives.query.FileQueryParams
import id.homebase.homebasekmppoc.prototype.lib.serialization.UuidSerializer
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

    }
