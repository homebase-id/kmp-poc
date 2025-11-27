package id.homebase.homebasekmppoc.lib.http

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.lib.crypto.CryptoHelper
import id.homebase.homebasekmppoc.lib.drives.FileSystemType
import id.homebase.homebasekmppoc.lib.serialization.OdinSystemSerializer

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*


data class CreateHttpClientOptions (
    val  overrideEncryption: Boolean = false,
    val  headers: Map<String, String> = emptyMap(),
    val  fileSystemType: FileSystemType = FileSystemType.Standard
)

/**
 * HTTP client for making authenticated requests to Odin backend
 * Handles query string encryption and authentication headers
 */
class OdinClient(
    private val providerOptions: ProviderOptions
) {

    fun getSharedSecret(): ByteArray? {
        return providerOptions.sharedSecret
    }

    fun getType(): ApiType {
        return providerOptions.api
    }

    fun getHostIdentity(): String {
        return providerOptions.hostIdentity
    }

    fun getLoggedInIdentity(): String? {
        return providerOptions.loggedInIdentity
    }

    fun isAuthenticated(): Boolean {
        return (this.getSharedSecret() != null)
    }

    fun getRoot(): String {
        return "https://${this.getHostIdentity()}"
    }

    fun getEndpointUrl(): String {
        if (providerOptions.v2Experimental) {
            return this.getRoot() + "/api/v2"
        }
        //TODO(biswa): RIP IT OUT NOWW!!
        val endpoint = when (this.getType()) {
            ApiType.Guest -> "/api/guest/v1"
            ApiType.App -> "/api/app/v1"
            ApiType.Owner -> "/api/owner/v1"
        }

        return this.getRoot() + endpoint
    }

    fun getHeaders(): Map<String, String> {
        return providerOptions.headers ?: emptyMap()
    }

    fun createHttpClient(createHttpClientOptions: CreateHttpClientOptions = CreateHttpClientOptions()): HttpClient {

        return HttpClient {
            expectSuccess = false
            defaultRequest {
                url(getEndpointUrl())
                header("X-ODIN-FILE-SYSTEM-TYPE", createHttpClientOptions.fileSystemType.name)
                providerOptions.headers?.forEach { (key, value) ->
                    header(key, value)
                }
                createHttpClientOptions.headers.forEach { (key, value) ->
                    header(key, value)
                }
            }
            install(ContentNegotiation) {
                register(ContentType.Application.OctetStream, KotlinxSerializationConverter(OdinSystemSerializer.json))
                json(OdinSystemSerializer.json)
            }
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }
            install(OdinEncryptionPlugin)

        }.apply {
            attributes.put(OdinEncryptionKeys.Secret, getSharedSecret() ?: ByteArray(0))
            if (createHttpClientOptions.overrideEncryption) {
                attributes.put(OdinEncryptionKeys.Override, false)
            }
        }

    }

    /**
     * Builds a URI with encrypted query string
     * @param uri The full URI including path and query string
     * @return URI with query string encrypted and replaced with ss parameter
     */
    private suspend fun buildUriWithEncryptedQueryString(uri: String, sharedSecret: ByteArray): String {
        return CryptoHelper.uriWithEncryptedQueryString(uri, sharedSecret)
    }


    suspend fun verifyToken(): Boolean {
        val ss = getSharedSecret() ?: return false

        val identity = getHostIdentity()
        val uri = "https://$identity/api/apps/v1/auth/verifytoken"
        val encryptedUri = buildUriWithEncryptedQueryString(uri, sharedSecret = ss )

        Logger.d("OdinHttpClient") { "Making GET request to: $encryptedUri" }

        val client = createHttpClient()
        val response = client.get(encryptedUri)

        return response.status.value == 200
    }


}


