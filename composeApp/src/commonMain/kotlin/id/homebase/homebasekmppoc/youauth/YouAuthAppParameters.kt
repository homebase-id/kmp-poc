package id.homebase.homebasekmppoc.youauth

import id.homebase.homebasekmppoc.encodeUrl

data class YouAuthAppParameters(
    val appId: String = "",
    val appName: String = "",
    val appOrigin: String = "",
    val clientFriendly: String = "",
    val drivesParam: String = "",
    val circleDrivesParam: String = "",
    val circleParam: String = "",
    val permissionParam: String = "",
    val returnParam: String = "",
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