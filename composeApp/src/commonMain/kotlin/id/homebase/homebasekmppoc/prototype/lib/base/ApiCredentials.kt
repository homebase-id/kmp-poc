package id.homebase.homebasekmppoc.prototype.lib.base

import id.homebase.homebasekmppoc.prototype.lib.core.SecureByteArray

@ConsistentCopyVisibility
data class ApiCredentials private constructor(
    val domain: String,
    val clientAccessToken: String,
    val sharedSecret: SecureByteArray
) {
    init {
        require(domain.isNotBlank())
        require(clientAccessToken.isNotBlank())
        require(sharedSecret.unsafeBytes.isNotEmpty())
    }

    companion object {
        fun create(domain: String, clientAccessToken: String, sharedSecret: SecureByteArray): ApiCredentials {
            return ApiCredentials(domain.lowercase().trim(), clientAccessToken, sharedSecret)
        }
    }
}
