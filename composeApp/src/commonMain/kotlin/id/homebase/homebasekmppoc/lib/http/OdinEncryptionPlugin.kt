package id.homebase.homebasekmppoc.lib.http
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.*
import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.lib.crypto.CryptoHelper
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.content.TextContent

object OdinEncryptionKeys {
    val Secret = AttributeKey<ByteArray>("OdinSecret")
    val Override = AttributeKey<Boolean>("OdinOverride")
}

val OdinEncryptionPlugin = createClientPlugin("OdinEncryption") {

    // REQUEST ENCRYPTION
    on(Send) { request ->
        val secret = request.attributes.getOrNull(OdinEncryptionKeys.Secret) ?: return@on proceed(request)
        val override = request.attributes.getOrNull(OdinEncryptionKeys.Override) ?: false
        if (override) return@on proceed(request)

        when (request.method.value.uppercase()) {
            "POST", "PUT", "PATCH", "DELETE" -> {
                // Ktor sends JSON as OutgoingContent → we intercept before it's written
                val currentBody = request.body
                if (currentBody is TextContent && currentBody.text.isNotBlank()) {
                    val payload = CryptoHelper.encryptData(currentBody.text, secret)
                    request.setBody(payload)
                    request.contentType(ContentType.Application.Json)
                }
            }
            else -> {
                val encryptedUrl = CryptoHelper.uriWithEncryptedQueryString(request.url.toString(), secret)
                request.url.takeFrom(encryptedUrl)
            }
        }
        proceed(request)
    }

    // RESPONSE DECRYPTION
    transformResponseBody { response, content, _ ->
        val secret = response.call.attributes.getOrNull(OdinEncryptionKeys.Secret) ?: return@transformResponseBody content
        val override = response.call.attributes.getOrNull(OdinEncryptionKeys.Override) ?: false
        if (override || response.status.value == 204) return@transformResponseBody content

        val text = content.readRemaining().readText()

        if (text.contains("\"data\"") && text.contains("\"iv\"")) {
            try {
                val decrypted = CryptoHelper.decryptContentAsString(text, secret)
                Logger.d("Odin") { "Decrypted → ${response.request.url}" }
                return@transformResponseBody ByteReadChannel(decrypted)
            } catch (e: Exception) {
                Logger.e(e) { "Decrypt failed" }
            }
        }
        ByteReadChannel(text)
    }
}