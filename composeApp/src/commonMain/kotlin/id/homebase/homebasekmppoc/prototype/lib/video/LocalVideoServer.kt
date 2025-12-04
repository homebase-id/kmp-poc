package id.homebase.homebasekmppoc.prototype.lib.video

import co.touchlab.kermit.Logger
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/**
 * Simple HTTP server for serving video content locally
 * Supports both HLS manifests/segments and regular video files
 *
 * This is fully common code - Ktor server works across all platforms!
 */
class LocalVideoServer(private val defaultPort: Int = 3001) {
    private var server: EmbeddedServer<*, *>? = null
    private var serverUrl: String = ""
    private val contentRegistry = mutableMapOf<String, ContentData>()

    private data class ContentData(
        val data: ByteArray,
        val contentType: String
    )

    /**
     * Start the server on the specified port
     * @param port Port to use (0 = use defaultPort)
     * @return The URL where the server is accessible
     */
    suspend fun start(port: Int = 0): String {
        if (server != null) {
            Logger.d("LocalVideoServer") { "Server already running at $serverUrl" }
            return serverUrl
        }

        val actualPort = if (port == 0) defaultPort else port

        server = embeddedServer(CIO, port = actualPort) {
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

                // Health check endpoint
                get("/health") {
                    call.respondBytes("OK".encodeToByteArray())
                }
            }
        }.start(wait = false)

        serverUrl = "http://localhost:$actualPort"
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
