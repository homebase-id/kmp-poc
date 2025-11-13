package id.homebase.homebasekmppoc.crypto

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Base64 URL encoding utilities
 * Provides URL-safe base64 encoding/decoding (RFC 4648 Section 5)
 */
object Base64UrlEncoder {

    @OptIn(ExperimentalEncodingApi::class)
    private val base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    /**
     * Encode byte array to base64url string
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun encode(input: ByteArray): String {
        return base64Url.encode(input)
    }

    /**
     * Encode string to base64url string
     */
    fun encode(input: String): String {
        return encode(input.toUtf8ByteArray())
    }

    /**
     * Decode base64url string to byte array
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun decode(input: String): ByteArray {
        return base64Url.decode(input)
    }

    /**
     * Decode base64url string to UTF-8 string
     */
    fun decodeString(input: String): String {
        return decode(input).toStringFromUtf8Bytes()
    }
}
