package id.homebase.homebasekmppoc.lib.youauth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests for YouAuthorizationParams data class. */
class YouAuthorizationParamsTest {

    @Test
    fun toQueryString_withAllFields_containsAllParameters() {
        val params =
                YouAuthorizationParams(
                        clientId = "test-app-id",
                        clientType = ClientType.app,
                        clientInfo = "Test Client",
                        publicKey = "base64PublicKey",
                        permissionRequest = """{"appId":"test"}""",
                        state = "random-state-123",
                        redirectUri = "homebase://callback"
                )

        val queryString = params.toQueryString()

        assertTrue(queryString.contains("client_id=test-app-id"))
        assertTrue(queryString.contains("client_type=app"))
        assertTrue(queryString.contains("client_info=Test%20Client"))
        assertTrue(queryString.contains("public_key=base64PublicKey"))
        assertTrue(queryString.contains("state=random-state-123"))
        assertTrue(queryString.contains("redirect_uri=homebase"))
    }

    @Test
    fun toQueryString_withEmptyFields_excludesEmptyFields() {
        val params =
                YouAuthorizationParams(
                        clientId = "test-app-id",
                        clientType = ClientType.domain,
                        clientInfo = "",
                        publicKey = "key",
                        permissionRequest = "",
                        state = "",
                        redirectUri = "http://callback"
                )

        val queryString = params.toQueryString()

        assertTrue(queryString.contains("client_id=test-app-id"))
        assertTrue(queryString.contains("client_type=domain"))
        assertTrue(!queryString.contains("client_info=&"))
    }

    @Test
    fun toMap_returnsCorrectMap() {
        val params =
                YouAuthorizationParams(
                        clientId = "app-123",
                        clientType = ClientType.app,
                        clientInfo = "My App",
                        publicKey = "pubkey",
                        permissionRequest = "{}",
                        state = "state123",
                        redirectUri = "https://callback"
                )

        val map = params.toMap()

        assertEquals("app-123", map["client_id"])
        assertEquals("app", map["client_type"])
        assertEquals("My App", map["client_info"])
        assertEquals("pubkey", map["public_key"])
        assertEquals("{}", map["permission_request"])
        assertEquals("state123", map["state"])
        assertEquals("https://callback", map["redirect_uri"])
    }

    @Test
    fun toQueryString_encodesSpecialCharacters() {
        val params =
                YouAuthorizationParams(
                        clientId = "test",
                        clientType = ClientType.app,
                        clientInfo = "App With Spaces & Special=Chars",
                        publicKey = "key",
                        permissionRequest = "",
                        state = "",
                        redirectUri = ""
                )

        val queryString = params.toQueryString()

        // URL encoding should escape spaces and special chars
        assertTrue(queryString.contains("%20") || queryString.contains("+"))
    }
}
