package id.homebase.homebasekmppoc.lib.http

enum class ApiType {
    Owner,
    App,
    Guest,
}

data  class ProviderOptions(
    val api: ApiType = ApiType.Guest,
    val sharedSecret: ByteArray? = null,
    val hostIdentity: String,
    val loggedInIdentity: String? = null,
    val headers: Map<String, String>? = null,
    val v2Experimental: Boolean = false,

    ) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ProviderOptions

        if (api != other.api) return false
        if (!sharedSecret.contentEquals(other.sharedSecret)) return false
        if (hostIdentity != other.hostIdentity) return false
        if (loggedInIdentity != other.loggedInIdentity) return false
        if (headers != other.headers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = api.hashCode()
        result = 31 * result + (sharedSecret?.contentHashCode() ?: 0)
        result = 31 * result + hostIdentity.hashCode()
        result = 31 * result + (loggedInIdentity?.hashCode() ?: 0)
        result = 31 * result + (headers?.hashCode() ?: 0)
        return result
    }
}
