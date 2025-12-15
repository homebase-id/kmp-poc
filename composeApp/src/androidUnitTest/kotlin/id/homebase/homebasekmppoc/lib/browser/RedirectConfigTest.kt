package id.homebase.homebasekmppoc.lib.browser

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Android-specific unit tests for RedirectConfig using Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class RedirectConfigTest {

    @Test
    fun testScheme_isYouauth() {
        assertEquals("youauth", RedirectConfig.scheme, "Android should use 'youauth' scheme")
    }

    @Test
    fun testBuildRedirectUri_startsWithScheme() {
        val clientId = "test-app-id"
        val redirectUri = RedirectConfig.buildRedirectUri(clientId)

        assertTrue(
                redirectUri.startsWith("youauth://"),
                "Redirect URI should start with 'youauth://'"
        )
    }

    @Test
    fun testBuildRedirectUri_containsClientId() {
        val clientId = "my-unique-app-id"
        val redirectUri = RedirectConfig.buildRedirectUri(clientId)

        assertTrue(redirectUri.contains(clientId), "Redirect URI should contain the client ID")
    }

    @Test
    fun testBuildRedirectUri_containsCallbackPath() {
        val clientId = "test-client"
        val redirectUri = RedirectConfig.buildRedirectUri(clientId)

        assertTrue(
                redirectUri.contains("/authorization-code-callback"),
                "Redirect URI should contain callback path"
        )
    }

    @Test
    fun testBuildRedirectUri_format() {
        val clientId = "com.example.app"
        val redirectUri = RedirectConfig.buildRedirectUri(clientId)

        assertEquals(
                "youauth://com.example.app/authorization-code-callback",
                redirectUri,
                "Redirect URI should follow the expected format"
        )
    }
}
