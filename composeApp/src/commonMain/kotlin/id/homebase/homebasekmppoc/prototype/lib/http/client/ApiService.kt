package id.homebase.homebasekmppoc.prototype.lib.http.client

import io.ktor.client.HttpClient

class ApiService(private val httpClient: HttpClient, private val credentialsManager: CredentialsManager) {
    suspend fun foo(data: String) {
        val (domain, cat, secret) = requireCredentials()

    }

    suspend fun bar(data: String) {
        // val domain = credentialsManager.activeCredentials?.domain
        //     ?: throw IllegalStateException("No active credentials found")
        // val cat = credentialsManager.activeCredentials?.clientAccessToken
        //     ?: throw IllegalStateException("No active credentials found")
        // val secret = credentialsManager.activeCredentials?.sharedSecret
        //     ?: throw IllegalStateException("No active credentials found")
    }

    private suspend fun requireCredentials(): ApiCredentials {
        return checkNotNull(credentialsManager.getActiveCredentials()) {
            "No active credentials found"
        }
    }
}