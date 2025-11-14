package id.homebase.homebasekmppoc.youauth

import kotlinx.serialization.Serializable

@Serializable
data class YouAuthTokenResponse(
    val base64SharedSecretCipher: String,
    val base64SharedSecretIv: String,
    val base64ClientAuthTokenCipher: String,
    val base64ClientAuthTokenIv: String
)
