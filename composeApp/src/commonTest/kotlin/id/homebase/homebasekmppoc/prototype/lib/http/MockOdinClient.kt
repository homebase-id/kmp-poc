package id.homebase.homebasekmppoc.prototype.lib.http

import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel

/**
 * A testable subclass of OdinClient that allows injecting a mock HttpClient. This enables unit
 * testing of providers that depend on OdinClient without making real HTTP requests.
 *
 * Usage:
 * ```
 * val mockClient = MockOdinClientSetup.createMockClient(responseJson)
 * val odinClient = MockOdinClientSetup.createMockOdinClient(mockClient)
 * val provider = SomeProvider(odinClient)
 * ```
 */
class TestableOdinClient(providerOptions: ProviderOptions, private val mockHttpClient: HttpClient) :
        OdinClient(providerOptions) {

    /** Overrides the parent's createHttpClient to return the injected mock client instead. */
    override fun createHttpClient(createHttpClientOptions: CreateHttpClientOptions): HttpClient {
        return mockHttpClient
    }
}

/**
 * Helper object for setting up mocked OdinClient instances in tests. Provides factory methods for
 * creating mock HTTP clients and TestableOdinClient instances.
 */
object MockOdinClientSetup {

    /**
     * Creates a default test shared secret (32 bytes) for encryption/decryption. Uses a fixed byte
     * pattern for reproducible tests.
     */
    fun createTestSharedSecret(): ByteArray {
        return ByteArray(32) { it.toByte() }
    }

    /**
     * Creates a MockEngine HttpClient that returns the given response for any request.
     *
     * @param responseBody The JSON response body to return
     * @param status The HTTP status code to return (defaults to OK)
     * @return An HttpClient configured with a MockEngine
     */
    fun createMockClient(
            responseBody: String,
            status: HttpStatusCode = HttpStatusCode.OK
    ): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { _ ->
                    respond(
                            content = ByteReadChannel(responseBody),
                            status = status,
                            headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
            install(ContentNegotiation) { json(OdinSystemSerializer.json) }
        }
    }

    /**
     * Creates a MockEngine HttpClient with a custom request handler. This allows for more complex
     * test scenarios where you need to inspect or vary the response.
     *
     * @param handler A function that receives the request and returns a response pair (body,
     * status)
     * @return An HttpClient configured with a custom MockEngine handler
     */
    fun createMockClientWithHandler(
            handler: (io.ktor.client.request.HttpRequestData) -> Pair<String, HttpStatusCode>
    ): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    val (body, status) = handler(request)
                    respond(
                            content = ByteReadChannel(body),
                            status = status,
                            headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
            install(ContentNegotiation) { json(OdinSystemSerializer.json) }
        }
    }

    /**
     * Creates a TestableOdinClient with the provided mock HttpClient.
     *
     * @param mockClient The mock HttpClient to inject
     * @param sharedSecret The shared secret for encryption (defaults to test secret)
     * @param hostIdentity The host identity (defaults to test.domain.com)
     * @param api The API type (defaults to App)
     * @return A TestableOdinClient configured with the mock client
     */
    fun createMockOdinClient(
            mockClient: HttpClient,
            sharedSecret: ByteArray? = createTestSharedSecret(),
            hostIdentity: String = "test.domain.com",
            api: ApiType = ApiType.App
    ): OdinClient {
        val options =
                ProviderOptions(api = api, sharedSecret = sharedSecret, hostIdentity = hostIdentity)
        return TestableOdinClient(options, mockClient)
    }

    /**
     * Creates a complete mock setup with OdinClient ready to use. This is a convenience method that
     * combines createMockClient and createMockOdinClient.
     *
     * @param responseBody The JSON response body the mock should return
     * @param status The HTTP status code to return (defaults to OK)
     * @param sharedSecret The shared secret for encryption (defaults to test secret)
     * @param hostIdentity The host identity (defaults to test.domain.com)
     * @return A TestableOdinClient configured with the mock response
     */
    fun setupMockOdinClient(
            responseBody: String,
            status: HttpStatusCode = HttpStatusCode.OK,
            sharedSecret: ByteArray? = createTestSharedSecret(),
            hostIdentity: String = "test.domain.com"
    ): OdinClient {
        val mockClient = createMockClient(responseBody, status)
        return createMockOdinClient(mockClient, sharedSecret, hostIdentity)
    }
}
