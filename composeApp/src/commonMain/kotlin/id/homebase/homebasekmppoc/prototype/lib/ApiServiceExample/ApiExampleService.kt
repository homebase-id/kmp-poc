package id.homebase.homebasekmppoc.prototype.lib.ApiServiceExample

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.prototype.lib.core.SecureByteArray
import id.homebase.homebasekmppoc.prototype.lib.crypto.CryptoHelper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

class ApiExampleService(
    private val httpClient: HttpClient,
    private val credentialsManager: CredentialsManager
) {

    suspend fun Ping(homebaseId: String): Boolean {
        val response: HttpResponse = httpClient.get("http://${homebaseId}/api/v2/health/ping")
        return response.status.isSuccess();
    }

    suspend fun echoSharedSecretEncryptedParam(): String {
        val (domain, cat, secret) = requireCredentials()
        Logger.d { "echoSharedSecretEncryptedParam: $domain, $cat, $secret" }

        //
        // Specific to this endpoint
        //
        val test = "Hello, World!"
        val uri =
            "https://$domain/api/v2/auth/echo-shared-secret-encrypted-param?checkValue64=${test}"

        //
        // Common for most endpoints
        //
        val encryptedUri = buildUriWithEncryptedQueryString(uri, secret)
        val response = httpClient.get(encryptedUri) {
            bearerAuth(cat)
        }
        val bodyCipher = response.body<String>()
        val bodyJson = decryptContentAsString(bodyCipher, secret)

        //
        // Specific to this endpoint
        //
        if (response.status.value != 200) {
            Logger.e { "echoSharedSecretEncryptedParam: Unexpected status code ${response.status.value}" }
        }

        val bodyText = Json.decodeFromString<String>(bodyJson)
        if (test == bodyText) {
            return "OK, $test == $bodyText"
        } else {
            return "NOT OK, $test != $bodyText"
        }
    }

    //

    private suspend fun requireCredentials(): ApiCredentials {
        return checkNotNull(credentialsManager.getActiveCredentials()) {
            "No active credentials found"
        }
    }

    //

    private suspend fun buildUriWithEncryptedQueryString(
        uri: String,
        secret: SecureByteArray
    ): String {
        return CryptoHelper.uriWithEncryptedQueryString(uri, secret.unsafeBytes)
    }

    //

    private suspend fun decryptContentAsString(
        cipherJson: String,
        secret: SecureByteArray
    ): String {
        return CryptoHelper.decryptContentAsString(cipherJson, secret.unsafeBytes)
    }

    //

}