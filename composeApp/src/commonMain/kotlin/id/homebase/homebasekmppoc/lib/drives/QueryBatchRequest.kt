package id.homebase.homebasekmppoc.lib.drives
import id.homebase.homebasekmppoc.lib.encodeUrl
import kotlinx.serialization.Serializable

/**
 * Query batch request containing query parameters and result options
 *
 */
@Serializable
data class QueryBatchRequest(
    val queryParams: FileQueryParams,
    val resultOptionsRequest: QueryBatchResultOptionsRequest
) {
    fun toQueryString(): String {
        val params = mutableListOf<String>()

        // Required fields from queryParams.targetDrive
        params.add("alias=${encodeUrl(queryParams.targetDrive.alias.toString())}")
        params.add("type=${encodeUrl(queryParams.targetDrive.type.toString())}")

        // Optional list fields from queryParams
        queryParams.fileType?.forEach { params.add("fileType=${encodeUrl(it.toString())}") }
        queryParams.dataType?.forEach { params.add("dataType=${encodeUrl(it.toString())}") }
        queryParams.fileState?.forEach { params.add("fileState=${encodeUrl(it.value.toString())}") }
        queryParams.archivalStatus?.forEach { params.add("archivalStatus=${encodeUrl(it.toString())}") }
        queryParams.sender?.forEach { params.add("sender=${encodeUrl(it)}") }
        queryParams.groupId?.forEach { params.add("groupId=${encodeUrl(it.toString())}") }
        queryParams.clientUniqueIdAtLeastOne?.forEach { params.add("clientUniqueIdAtLeastOne=${encodeUrl(it.toString())}") }
        queryParams.tagsMatchAtLeastOne?.forEach { params.add("tagsMatchAtLeastOne=${encodeUrl(it.toString())}") }
        queryParams.tagsMatchAll?.forEach { params.add("tagsMatchAll=${encodeUrl(it.toString())}") }
        queryParams.localTagsMatchAll?.forEach { params.add("localTagsMatchAll=${encodeUrl(it.toString())}") }
        queryParams.localTagsMatchAtLeastOne?.forEach { params.add("localTagsMatchAtLeastOne=${encodeUrl(it.toString())}") }
        queryParams.globalTransitId?.forEach { params.add("globalTransitId=${encodeUrl(it.toString())}") }

        // Optional date range fields from queryParams
        queryParams.userDateStart?.let { params.add("userDateStart=${encodeUrl(it.toString())}") }
        queryParams.userDateEnd?.let { params.add("userDateEnd=${encodeUrl(it.toString())}") }

        // Result options fields from resultOptionsRequest
        resultOptionsRequest.cursorState?.let { if (it.isNotEmpty()) params.add("cursorState=${encodeUrl(it)}") }
        params.add("maxRecords=${encodeUrl(resultOptionsRequest.maxRecords.toString())}")
        params.add("includeMetadataHeader=${encodeUrl(resultOptionsRequest.includeMetadataHeader.toString())}")
        params.add("includeTransferHistory=${encodeUrl(resultOptionsRequest.includeTransferHistory.toString())}")
        resultOptionsRequest.ordering?.let { params.add("ordering=${encodeUrl(it.name)}") }
        resultOptionsRequest.sorting?.let { params.add("sorting=${encodeUrl(it.name)}") }

        return params.joinToString("&")
    }
}