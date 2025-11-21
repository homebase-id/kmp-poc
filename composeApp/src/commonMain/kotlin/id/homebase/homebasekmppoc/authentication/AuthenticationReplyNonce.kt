package id.homebase.homebasekmppoc.authentication

import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationReplyNonce(
    val nonce64: String,
    val nonceHashedPassword64: String,
    val crc: UInt,
    val gcmEncrypted64: String,
    val publicKeyJwk: String
)
