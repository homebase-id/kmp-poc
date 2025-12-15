package id.homebase.homebasekmppoc.lib.browser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Desktop-specific tests for RedirectConfig. These test the http://localhost scheme used for
 * desktop OAuth flows.
 */
class RedirectConfigTest {

    @Test
    fun testScheme_isHttp() {
        assertEquals(
                "http",
                RedirectConfig.scheme,
                "Desktop should use 'http' scheme for localhost"
        )
    }

    @Test
    fun testBuildRedirectUri_containsLocalhost() {
        val clientId = "test-app-id"
        val redirectUri = RedirectConfig.buildRedirectUri(clientId)

        assertTrue(
                redirectUri.contains("localhost"),
                "Desktop redirect URI should contain 'localhost'"
        )
    }

    @Test
    fun testBuildRedirectUri_containsCallbackPath() {
        val clientId = "my-app"
        val redirectUri = RedirectConfig.buildRedirectUri(clientId)

        assertTrue(
                redirectUri.contains("/authorization-code-callback"),
                "Redirect URI should contain callback path"
        )
    }

    @Test
    fun testBuildRedirectUri_containsPort() {
        val clientId = "test-client"
        val redirectUri = RedirectConfig.buildRedirectUri(clientId)

        // Desktop uses dynamic ports in the format http://localhost:PORT/...
        val portPattern = Regex(":([0-9]+)/")
        assertTrue(
                portPattern.containsMatchIn(redirectUri),
                "Desktop redirect URI should contain a port number"
        )
    }
}
