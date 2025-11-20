package id.homebase.homebasekmppoc.authentication

import kotlinx.serialization.Serializable

@Serializable
data class NonceData(
    val crc: Int,
    val id: String,
    val nonce64: String,
    val publicJwk: String,
    val saltKek64: String,
    val saltPassword64: String
)
