package id.homebase.homebasekmppoc.prototype.lib.drives

import id.homebase.homebasekmppoc.prototype.encodeUrl
import kotlinx.serialization.Serializable

@Serializable
data class GetDrivesByTypeRequest (
    var driveType: String,
    var pageNumber: Int,
    var pageSize: Int
) {
    fun toQueryString(): String {
        val params = mutableListOf<String>()
        params.add("driveType=${encodeUrl(driveType)}")
        params.add("pageNumber=${pageNumber}")
        params.add("pageSize=${pageSize}")
        return params.joinToString("&")
    }
}

