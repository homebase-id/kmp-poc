package id.homebase.homebasekmppoc.youauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YouAuthTokenRequest(
    @SerialName("secret_digest")
    val exchangeSecretDigest: String
)
