package id.homebase.homebasekmppoc.lib.authentication

import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationPayload(
    val hpwd64: String,
    val kek64: String,
    val secret: String  // Base64-encoded random bytes
)
