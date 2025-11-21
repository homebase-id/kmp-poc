package id.homebase.homebasekmppoc.http

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.authentication.AuthState
import id.homebase.homebasekmppoc.crypto.AesCbc
import id.homebase.homebasekmppoc.crypto.ByteArrayUtil
import id.homebase.homebasekmppoc.crypto.CryptoHelper
import id.homebase.homebasekmppoc.encodeUrl
import id.homebase.homebasekmppoc.serialization.OdinSystemSerializer
import id.homebase.homebasekmppoc.toBase64
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
    private suspend fun buildUriWithEncryptedQueryString(uri: String): String {
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
    private suspend fun decryptContentAsString(cipherJson: String): String {
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
        return decryptContent<T>(cipherJson)
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

    suspend fun getPayloadBytes(): ByteArray
    {
        val uri = "https://$identity/api/owner/v1/drive/files/payload?alias=e8475dc46cb4b6651c2d0dbd0f3aad5f&type=8f448716e34cedf9014145e043ca6612&fileId=5201aa19-6010-2200-8aa8-fced8bf4cc24&key=pst_mdi0&xfst=128"

        val encryptedUri = buildUriWithEncryptedQueryString(uri)

        Logger.d("OdinHttpClient") { "Making GET request to: $encryptedUri" }

        val client = createHttpClient()

        var response: io.ktor.client.statement.HttpResponse
        if (uri.contains("/api/owner")) {
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

        Logger.d("OdinHttpClient") { "response length: ${response.contentLength()}" }

        // Get response as raw bytes
        val cipherBytes = response.body<ByteArray>()
        Logger.d("OdinHttpClient") { "getPayloadBytes encrypted response length: ${cipherBytes.size}" }
        Logger.d("OdinHttpClient") { "getPayloadBytes first 32 bytes: ${cipherBytes.take(32).joinToString(" ") { "%02x".format(it) }}" }
        Logger.d("OdinHttpClient") { "sharedSecret length: ${sharedSecret.size}" }
        Logger.d("OdinHttpClient") { "sharedSecret: ${sharedSecret.toBase64()}" }

        // Decrypt the response using raw byte decryption (no JSON)
        val decryptedBytes = CryptoHelper.decryptContent(cipherBytes, sharedSecret)
        Logger.d("OdinHttpClient") { "getPayloadBytes decrypted length: ${decryptedBytes.size}" }

        return decryptedBytes
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

/**
 * Create HTTP client with JSON serialization support
 */
fun createHttpClient() = HttpClient {
    install(ContentNegotiation) {
        json(OdinSystemSerializer.json)
    }
}
