package id.homebase.homebasekmppoc.prototype.lib.drives

import kotlinx.serialization.Serializable

/**
 * Query batch request containing query parameters and result options
 *
 * Ported from C# Odin.Services.Drives.QueryBatchRequest
 */
@Serializable
data class QueryBatchRequest(
    val queryParams: FileQueryParams,
    val resultOptionsRequest: QueryBatchResultOptionsRequest
)
