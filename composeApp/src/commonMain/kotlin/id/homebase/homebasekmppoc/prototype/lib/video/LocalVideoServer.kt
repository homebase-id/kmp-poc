package id.homebase.homebasekmppoc.prototype.lib.video

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.utils.io.core.toByteArray

/**
 * Simple HTTP server for serving video content locally
 * Supports both HLS manifests/segments and regular video files
 *
 * This is fully common code - Ktor server works across all platforms!
 */
class LocalVideoServer(
    private val authToken: String? = null
) {
    private var server: EmbeddedServer<*, *>? = null
    private var serverUrl: String = ""
    private val contentRegistry = mutableMapOf<String, ContentData>()

    private data class ContentData(
        val data: ByteArray,
        val contentType: String
    )

    /**
     * Start the server on a random available port
     * @return The URL where the server is accessible
     */
    suspend fun start(): String {
        if (server != null) {
            Logger.d("LocalVideoServer") { "Server already running at $serverUrl" }
            return serverUrl
        }

        server = embeddedServer(CIO, port = 0) {
            routing {
                // Serve registered content by ID
                get("/content/{id}") {
                    val id = call.parameters["id"]
                    if (id == null) {
                        call.response.status(HttpStatusCode.BadRequest)
                        return@get
                    }

                    val content = contentRegistry[id]
                    if (content == null) {
                        Logger.w("LocalVideoServer") { "Content not found: $id" }
                        call.response.status(HttpStatusCode.NotFound)
                        return@get
                    }

                    Logger.d("LocalVideoServer") { "Serving content: $id (${content.data.size} bytes, ${content.contentType})" }
                    call.respondBytes(
                        bytes = content.data,
                        contentType = ContentType.parse(content.contentType)
                    )
                }

                // Proxy endpoint for remote URLs with auth header
                get("/proxy") {
                    val url = call.request.queryParameters["url"]
                    if (url == null) {
                        Logger.w("LocalVideoServer") { "Proxy request missing url parameter" }
                        call.response.status(HttpStatusCode.BadRequest)
                        return@get
                    }

                    try {
                        Logger.i("LocalVideoServer") { "Proxying request to: $url" }
                        val client = HttpClient()
                        val response = client.get(url) {
                            // Forward all headers from the client request
                            call.request.headers.forEach { key, values ->
                                if (key.lowercase() != "host" && key.lowercase() != "accept-encoding") {
                                    values.forEach { value ->
                                        header(key, value)
                                    }
                                }
                            }
                            if (authToken != null) {
                                header("DY0810", authToken)
                                Logger.d("LocalVideoServer") { "Added DY0810 header to proxy request" }
                            }
                        }
                        val bytes = response.readBytes()
                        Logger.i("LocalVideoServer") { "Proxied content size: ${bytes.size}, status: ${response.status}" }

                        // Copy response headers
                        response.headers.forEach { key, values ->
                            values.forEach { value ->
                                call.response.headers.append(key, value)
                            }
                        }

                        call.respondBytes(
                            bytes = bytes,
                            contentType = ContentType.parse("application/octet-stream")
                        )
                        client.close()
                    } catch (e: Exception) {
                        Logger.e("LocalVideoServer") { "Proxy request failed: ${e.message}" }
                        call.response.status(HttpStatusCode.InternalServerError)
                    }
                }

                // Health check endpoint
                get("/health") {
                    call.respondBytes("OK".encodeToByteArray())
                }
            }
        }.start(wait = false)

        // Get the actual port that was assigned by the system
        val actualPort = server!!.engine.resolvedConnectors().first().port
        serverUrl = "http://127.0.0.1:$actualPort"
        Logger.i("LocalVideoServer") { "Video server started at $serverUrl" }
        return serverUrl
    }

    /**
     * Stop the server
     */
    fun stop() {
        server?.stop(1000, 2000)
        server = null
        contentRegistry.clear()
        Logger.i("LocalVideoServer") { "Video server stopped" }
    }

    /**
     * Register video data that can be served
     * @param id Unique identifier for this video
     * @param data Video data (can be HLS manifest, segment, or full video)
     * @param contentType MIME type (e.g., "application/vnd.apple.mpegurl", "video/mp4")
     */
    fun registerContent(id: String, data: ByteArray, contentType: String) {
        contentRegistry[id] = ContentData(data, contentType)
        Logger.d("LocalVideoServer") { "Registered content: $id (${data.size} bytes, $contentType)" }
    }

    /**
     * Get the URL for accessing registered content
     * @param id The content identifier
     * @return Full URL to access the content
     */
    fun getContentUrl(id: String): String {
        return "$serverUrl/content/$id"
    }
}
