package id.homebase.homebasekmppoc.lib.youauth

import id.homebase.homebasekmppoc.lib.encodeUrl
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YouAuthAppParameters(
    @SerialName(AppIdName)
    val appId: String = "",

    @SerialName(AppNameName)
    val appName: String = "",

    @SerialName(AppOriginName)
    val appOrigin: String = "",

    @SerialName(ClientFriendlyName)
    val clientFriendly: String = "",

    @SerialName(DrivesParamName)
    val drivesParam: String = "",

    @SerialName(CircleDrivesParamName)
    val circleDrivesParam: String = "",

    @SerialName(CircleParamName)
    val circleParam: String = "",

    @SerialName(PermissionParamName)
    val permissionParam: String = "",

    @SerialName(ReturnName)
    val returnParam: String = "",

    @SerialName(CancelName)
    val cancel: String = ""
) {
    fun toQueryString(): String {
        val params = mutableListOf<String>()
        params.add("appId=${encodeUrl(appId)}")
        params.add("n=${encodeUrl(appName)}")
        params.add("o=${encodeUrl(appOrigin)}")
        params.add("fn=${encodeUrl(clientFriendly)}")
        params.add("d=${encodeUrl(drivesParam)}")
        params.add("cd=${encodeUrl(circleDrivesParam)}")
        params.add("c=${encodeUrl(circleParam)}")
        params.add("p=${encodeUrl(permissionParam)}")
        params.add("return=${encodeUrl(returnParam)}")
        params.add("cancel=${encodeUrl(cancel)}")
        return params.joinToString("&")
    }

    companion object {
        const val AppIdName = "appId"
        const val AppNameName = "n"
        const val AppOriginName = "o"
        const val ClientFriendlyName = "fn"
        const val DrivesParamName = "d"
        const val CircleDrivesParamName = "cd"
        const val CircleParamName = "c"
        const val PermissionParamName = "p"
        const val ReturnName = "return"
        const val CancelName = "cancel"
    }
}