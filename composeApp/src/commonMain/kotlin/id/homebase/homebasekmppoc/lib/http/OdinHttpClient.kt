package id.homebase.homebasekmppoc.lib.http

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.lib.authentication.AuthState
import id.homebase.homebasekmppoc.lib.crypto.AesCbc
import id.homebase.homebasekmppoc.lib.crypto.ByteArrayUtil
import id.homebase.homebasekmppoc.lib.crypto.CryptoHelper
import id.homebase.homebasekmppoc.lib.encodeUrl
import id.homebase.homebasekmppoc.lib.serialization.OdinSystemSerializer
import id.homebase.homebasekmppoc.lib.toBase64
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.serialization.kotlinx.json.json
import kotlin.io.encoding.Base64

/**
 * HTTP client for making authenticated requests to Odin backend
 * Handles query string encryption and authentication headers
 */
class OdinHttpClient(
    private val authenticatedState: AuthState.Authenticated
) {
    private val identity: String = authenticatedState.identity
    private val clientAuthToken: String = authenticatedState.clientAuthToken
    @PublishedApi
    internal val sharedSecret: ByteArray = Base64.decode(authenticatedState.sharedSecret)

    /**
     * Builds a URI with encrypted query string
     * @param uri The full URI including path and query string
     * @return URI with query string encrypted and replaced with ss parameter
     */
    suspend fun buildUriWithEncryptedQueryString(uri: String): String {
        return CryptoHelper.uriWithEncryptedQueryString(uri, sharedSecret)
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

        var response: io.ktor.client.statement.HttpResponse
        if (path.startsWith("/api/owner")) {
            response = client.get(encryptedUri) {
                headers {
                    append("Cookie", "DY0810=$clientAuthToken")
                }
            }
        } else  {
            response = client.get(encryptedUri) {
                headers {
                    append("Cookie", "XT32=$clientAuthToken")
                }
            }
        }

        val cipherJson = response.body<String>()
        // Logger.d("OdinHttpClient") { "Encrypted response: $cipherJson" }

        return cipherJson
    }

    /**
     * Decrypts response content encrypted with shared secret
     * @param cipherJson The encrypted JSON response
     * @return Decrypted string
     */
    suspend fun decryptContentAsString(cipherJson: String): String {
        return CryptoHelper.decryptContentAsString(cipherJson, sharedSecret)
    }

    /**
     * Decrypts response content encrypted with shared secret and deserializes to type T
     * @param cipherJson The encrypted JSON response
     * @return Decrypted and deserialized object of type T
     */
    suspend inline fun <reified T> decryptContent(cipherJson: String): T {
        return CryptoHelper.decryptContent<T>(cipherJson, sharedSecret)
    }

    /**
     * Performs an authenticated GET request and returns decrypted response
     * @param path The path including query string
     * @return Decrypted response as type T
     */
    suspend inline fun <reified T> get(path: String): T {
        val cipherJson = performRequest(path)
        val plainJson = decryptContentAsString(cipherJson)
        Logger.d("OdinHttpClient") { "Decrypted response JSON: $plainJson" }
        return OdinSystemSerializer.deserialize<T>(plainJson)
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

    suspend fun verifyOwnerToken(): String {
        val uri = "https://$identity/api/owner/v1/authentication/verifytoken"
        val encryptedUri = buildUriWithEncryptedQueryString(uri)

        Logger.d("OdinHttpClient") { "Making GET request to: $encryptedUri" }

        val client = createHttpClient()
        val response = client.get(encryptedUri) {
            headers {
                append("Cookie", "DY0810=$clientAuthToken")
            }
        }

        if (response.status != HttpStatusCode.OK) {
            return response.status.toString()
        }

        return response.body<Boolean>().toString()
    }

    //

    suspend fun verifyAppToken(): String {
        val uri = "https://$identity/api/apps/v1/auth/verifytoken"
        val encryptedUri = buildUriWithEncryptedQueryString(uri)

        Logger.d("OdinHttpClient") { "Making GET request to: $encryptedUri" }

        val client = createHttpClient()
        val response = client.get(encryptedUri) {
            headers {
                append("Cookie", "XT32=$clientAuthToken")
            }
        }

        if (response.status != HttpStatusCode.OK) {
            return response.status.toString()
        }

        return response.body<Boolean>().toString()
    }

    //


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

/**
 * Create HTTP client with JSON serialization support
 */
fun createHttpClient() = HttpClient {
    install(ContentNegotiation) {
        json(OdinSystemSerializer.json)
    }
}
