package id.homebase.homebasekmppoc.http

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.crypto.AesCbc
import id.homebase.homebasekmppoc.crypto.ByteArrayUtil
import id.homebase.homebasekmppoc.encodeUrl
import id.homebase.homebasekmppoc.serialization.OdinSystemSerializer
import id.homebase.homebasekmppoc.toBase64
import id.homebase.homebasekmppoc.youauth.YouAuthState
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlin.io.encoding.Base64

/**
 * HTTP client for making authenticated requests to Odin backend
 * Handles query string encryption and authentication headers
 */
class OdinHttpClient(
    private val authenticatedState: YouAuthState.Authenticated
) {
    private val identity: String = authenticatedState.identity
    private val clientAuthToken: String = authenticatedState.clientAuthToken
    private val sharedSecret: ByteArray = Base64.decode(authenticatedState.sharedSecret)

    /**
     * Builds a URI with encrypted query string
     * @param uri The full URI including path and query string
     * @return URI with query string encrypted and replaced with ss parameter
     */
    private suspend fun buildUriWithEncryptedQueryString(uri: String): String {
        val queryIndex = uri.indexOf('?')
        if (queryIndex == -1 || queryIndex == uri.length - 1) {
            return uri
        }

        val path = uri.substring(0, queryIndex)
        val query = uri.substring(queryIndex + 1)

        // Generate random IV
        val iv = ByteArrayUtil.getRndByteArray(16)

        // Encrypt query string
        val encryptedBytes = AesCbc.encrypt(query.encodeToByteArray(), sharedSecret, iv)

        // Build payload
        val payload = SharedSecretEncryptedPayload(
            iv = iv.toBase64(),
            data = encryptedBytes.toBase64()
        )

        // Serialize and URL encode
        val serializedPayload = OdinSystemSerializer.serialize(payload)
        val encodedPayload = encodeUrl(serializedPayload)

        return "$path?ss=$encodedPayload"
    }

    /**
     * Performs an authenticated GET request and returns decrypted String response
     * @param path The path including query string (e.g., "/api/guest/v1/builtin/home/auth/ping?text=helloworld")
     * @return Decrypted response as String
     */
    suspend fun getString(path: String): String {
        val cipherJson = performRequest(path)
        return decryptContentAsString(cipherJson)
    }

    /**
     * Performs the HTTP GET request with encrypted query string and auth cookie
     */
    suspend fun performRequest(path: String): String {
        val fullUri = "https://$identity$path"
        val encryptedUri = buildUriWithEncryptedQueryString(fullUri)

        Logger.d("OdinHttpClient") { "Making GET request to: $encryptedUri" }

        val client = createHttpClient()

        val response = client.get(encryptedUri) {
            headers {
                append("Cookie", "XT32=$clientAuthToken")
            }
        }

        val cipherJson = response.body<String>()
        Logger.d("OdinHttpClient") { "Encrypted response: $cipherJson" }

        return cipherJson
    }

    /**
     * Decrypts response content encrypted with shared secret
     * @param cipherJson The encrypted JSON response
     * @return Decrypted string
     */
    private suspend fun decryptContentAsString(cipherJson: String): String {
        // Deserialize the encrypted payload
        val payload = OdinSystemSerializer.deserialize<SharedSecretEncryptedPayload>(cipherJson)

        // Decrypt the data
        val iv = Base64.decode(payload.iv)
        val encryptedData = Base64.decode(payload.data)
        val plainBytes = AesCbc.decrypt(encryptedData, sharedSecret, iv)

        // Convert to string
        val plainString = plainBytes.decodeToString()
        Logger.d("OdinHttpClient") { "Decrypted response: $plainString" }

        return plainString
    }

    /**
     * Decrypts response content encrypted with shared secret and deserializes to type T
     * @param cipherJson The encrypted JSON response
     * @return Decrypted and deserialized object of type T
     */
    suspend fun <T> decryptContent(cipherJson: String, typeDeserializer: (String) -> T): T {
        // Deserialize the encrypted payload
        val payload = OdinSystemSerializer.deserialize<SharedSecretEncryptedPayload>(cipherJson)

        // Decrypt the data
        val iv = Base64.decode(payload.iv)
        val encryptedData = Base64.decode(payload.data)
        val plainBytes = AesCbc.decrypt(encryptedData, sharedSecret, iv)

        // Convert to string and deserialize
        val plainJson = plainBytes.decodeToString()
        Logger.d("OdinHttpClient") { "Decrypted response: $plainJson" }

        return typeDeserializer(plainJson)
    }

    /**
     * Performs an authenticated GET request and returns decrypted response
     * @param path The path including query string
     * @return Decrypted response as type T
     */
    suspend inline fun <reified T> get(path: String): T {
        val cipherJson = performRequest(path)
        return decryptContent(cipherJson) { plainJson ->
            OdinSystemSerializer.deserialize<T>(plainJson)
        }
    }

    //

    suspend fun isAuthenticated(): String {
        val uri = "https://$identity/api/guest/v1/builtin/home/auth/is-authenticated"
        val encryptedUri = buildUriWithEncryptedQueryString(uri)

        val client = createHttpClient()
        val response = client.get(encryptedUri) {
            headers {
                append("Cookie", "XT32=$clientAuthToken")
            }
        }

        return response.body<String>()
    }

    //

    suspend fun verifyToken(): String {
        val uri = "https://$identity/api/apps/v1/auth/verifytoken"
        val encryptedUri = buildUriWithEncryptedQueryString(uri)

        Logger.d("OdinHttpClient") { "Making GET request to: $encryptedUri" }

        val client = createHttpClient()
        val response = client.get(encryptedUri) {
            headers {
                append("Cookie", "XT32=$clientAuthToken")
            }
        }

        return response.status.toString()
    }

    //

    /**
     * Create HTTP client with JSON serialization support
     */
    private fun createHttpClient() = HttpClient {
        install(ContentNegotiation) {
            json(OdinSystemSerializer.json)
        }
    }
}
