package id.homebase.homebasekmppoc.prototype.lib.youauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YouAuthDriveParameters(
    @SerialName("a")
    val driveAlias: String,

    @SerialName("t")
    val driveType: String,

    @SerialName("n")
    val name: String,

    @SerialName("d")
    val description: String,

    @SerialName("p")
    val permission: Int
)
