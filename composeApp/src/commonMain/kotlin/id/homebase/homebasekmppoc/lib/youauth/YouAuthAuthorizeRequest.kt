package id.homebase.homebasekmppoc.lib.youauth

import id.homebase.homebasekmppoc.lib.encodeUrl

data class YouAuthAuthorizeRequest(
    val redirectUri: String = "",
    val clientId: String = "",
    val clientType: ClientType = ClientType.unknown,
    val clientInfo: String = "",
    val permissionRequest: String = "",
    val publicKey: String = "",
    val state: String = ""
) {
    fun toQueryString(): String {
        val params = mutableListOf<String>()
        if (clientId.isNotEmpty()) params.add("client_id=${encodeUrl(clientId)}")
        params.add("client_type=${encodeUrl(clientType.toString())}")
        if (clientInfo.isNotEmpty()) params.add("client_info=${encodeUrl(clientInfo)}")
        if (redirectUri.isNotEmpty()) params.add("redirect_uri=${encodeUrl(redirectUri)}")
        if (permissionRequest.isNotEmpty()) params.add("permission_request=${encodeUrl(permissionRequest)}")
        if (publicKey.isNotEmpty()) params.add("public_key=${encodeUrl(publicKey)}")
        if (state.isNotEmpty()) params.add("state=${encodeUrl(state)}")
        return params.joinToString("&")
    }
}

