package id.homebase.homebasekmppoc.prototype.lib.client

import id.homebase.homebasekmppoc.prototype.lib.ApiServiceExample.CredentialsManager
import id.homebase.homebasekmppoc.prototype.lib.core.SecureByteArray
import id.homebase.homebasekmppoc.prototype.lib.crypto.CryptoHelper
import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.contentType


abstract class OdinApiProviderBase(
    protected val httpClient: HttpClient,
    protected val credentialsManager: CredentialsManager
) {

    protected data class ActiveCreds(
        val domain: String,
        val accessToken: String,
        val secret: SecureByteArray
    )

    protected suspend fun requireCreds(): ActiveCreds {
        val (domain, token, secret) =
            checkNotNull(credentialsManager.getActiveCredentials())
        return ActiveCreds(domain, token, secret)
    }

    protected fun apiUrl(domain: String, path: String): String =
        "https://$domain/api/v2$path"

    // ------------------------------------------------------------
    // Core request primitive
    // ------------------------------------------------------------

    protected suspend fun request(
        block: suspend () -> io.ktor.client.statement.HttpResponse,
        secret: SecureByteArray?
    ): ApiResponse {
        val response = block()
        val rawBody = response.body<String>()
        val isEncrypted = response.headers["X-SSE"] == "1"

        val body =
            if (isEncrypted && secret != null) {
                CryptoHelper.decryptContentAsString(rawBody, secret.unsafeBytes)
            } else {
                rawBody
            }

        return ApiResponse(
            status = response.status.value,
            headers = response.headers,
            body = body,
            isEncrypted = isEncrypted
        )
    }

    // ------------------------------------------------------------
    // Plain requests (JSON already serialized)
    // ------------------------------------------------------------

    protected suspend fun plainGet(
        url: String,
        token: String
    ): ApiResponse =
        request(
            {
                httpClient.get(url) {
                    bearerAuth(token)
                    accept(ContentType.Application.Json)
                }
            },
            secret = null
        )

    protected suspend fun plainPutJson(
        url: String,
        token: String,
        jsonBody: String
    ): ApiResponse =
        request(
            {
                httpClient.put(url) {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(TextContent(jsonBody, ContentType.Application.Json))
                }
            },
            secret = null
        )

    protected suspend fun plainPatchJson(
        url: String,
        token: String,
        jsonBody: String
    ): ApiResponse =
        request(
            {
                httpClient.patch(url) {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(TextContent(jsonBody, ContentType.Application.Json))
                }
            },
            secret = null
        )

    protected suspend fun plainPostMultipart(
        url: String,
        token: String,
        formData: MultiPartFormDataContent
    ): ApiResponse =
        request(
            {
                httpClient.post(url) {
                    bearerAuth(token)
                    setBody(formData)
                }
            },
            secret = null
        )

    protected suspend fun plainPatchMultipart(
        url: String,
        token: String,
        formData: MultiPartFormDataContent
    ): ApiResponse =
        request(
            {
                httpClient.patch(url) {
                    bearerAuth(token)
                    setBody(formData)
                }
            },
            secret = null
        )


    // ------------------------------------------------------------
    // Encrypted requests (JSON already serialized)
    // ------------------------------------------------------------

    protected suspend fun encryptedGet(
        url: String,
        token: String,
        secret: SecureByteArray
    ): ApiResponse =
        request(
            {
                httpClient.get(url) {
                    bearerAuth(token)
                    accept(ContentType.Application.Json)
                }
            },
            secret = secret
        )

    protected suspend fun encryptedPostJson(
        url: String,
        token: String,
        jsonBody: String,
        secret: SecureByteArray
    ): ApiResponse =
        request(
            {
                httpClient.post(url) {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(
                        TextContent(
                            OdinSystemSerializer.serialize(
                                CryptoHelper.encryptData(
                                    jsonBody,
                                    secret.unsafeBytes
                                )
                            ),
                            ContentType.Application.Json
                        )
                    )
                }
            },
            secret = secret
        )

    protected suspend fun encryptedPutJson(
        url: String,
        token: String,
        jsonBody: String,
        secret: SecureByteArray
    ): ApiResponse =
        request(
            {
                httpClient.put(url) {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(
                        TextContent(
                            OdinSystemSerializer.json.encodeToString(
                                CryptoHelper.encryptData(jsonBody, secret.unsafeBytes)
                            ),
                            ContentType.Application.Json
                        )
                    )
                }
            },
            secret = secret
        )

    protected suspend fun encryptedPatchJson(
        url: String,
        token: String,
        jsonBody: String,
        secret: SecureByteArray
    ): ApiResponse =
        request(
            {
                httpClient.patch(url) {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(
                        TextContent(
                            OdinSystemSerializer.json.encodeToString(
                                CryptoHelper.encryptData(jsonBody, secret.unsafeBytes)
                            ),
                            ContentType.Application.Json
                        )
                    )
                }
            },
            secret = secret
        )

    protected inline fun <reified T> deserialize(json: String): T =
        OdinSystemSerializer.deserialize(json)


}
