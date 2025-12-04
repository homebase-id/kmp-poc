package id.homebase.homebasekmppoc.prototype.lib.youauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ClientType {
    @SerialName("unknown")
    unknown,

    @SerialName("app")
    app,

    @SerialName("domain")
    domain
}
