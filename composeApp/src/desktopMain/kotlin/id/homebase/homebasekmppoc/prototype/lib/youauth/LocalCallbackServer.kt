package id.homebase.homebasekmppoc.prototype.lib.youauth

import co.touchlab.kermit.Logger
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.respondHtml
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.p
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe
import java.net.ServerSocket

/**
 * Local HTTP server for handling YouAuth callbacks on desktop.
 * Listens on http://localhost:{PORT}/callback where PORT is dynamically assigned
 */
object LocalCallbackServer {
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    private var currentPort: Int = 0
    private const val START_PORT = 49152  // Start of dynamic/private port range
    private const val END_PORT = 65535    // End of port range
    private const val MAX_PORT_ATTEMPTS = 100

    /**
     * Start the callback server on an available port
     * @param scope CoroutineScope for handling callbacks
     * @param preferredPort Optional preferred port to use, or 0 to auto-select
     * @return The port number the server is running on, or -1 if failed
     */
    fun start(scope: CoroutineScope, preferredPort: Int = 0): Int {
        if (server != null) {
            Logger.Companion.w("LocalCallbackServer") { "Server already running on port $currentPort" }
            return currentPort
        }

        // If a preferred port was specified, try it first
        val portsToTry = if (preferredPort > 0) {
            listOf(preferredPort) + (0 until MAX_PORT_ATTEMPTS).map {
                START_PORT + (Math.random() * (END_PORT - START_PORT)).toInt()
            }
        } else {
            (0 until MAX_PORT_ATTEMPTS).map {
                START_PORT + (Math.random() * (END_PORT - START_PORT)).toInt()
            }
        }

        for (port in portsToTry) {
            try {
                Logger.Companion.d("LocalCallbackServer") { "Attempting to start server on port $port" }

                server = embeddedServer(CIO, port = port) {
                    routing {
                        get("/authorization-code-callback") {
                            // Get the full URL with query parameters
                            val fullUrl = call.request.local.let { local ->
                                "http://localhost:$currentPort${local.uri}"
                            }

                            Logger.Companion.d("LocalCallbackServer") { "Received callback: $fullUrl" }

                            // Handle the callback asynchronously
                            scope.launch {
                                try {
                                    YouAuthCallbackRouter.handleCallback(fullUrl)
                                    Logger.Companion.i("LocalCallbackServer") { "Callback handled successfully" }
                                } catch (e: Exception) {
                                    Logger.Companion.e("LocalCallbackServer") { "Error handling callback: ${e.message}" }
                                }
                            }

                            // Return a success page
                            call.respondHtml {
                                head {
                                    title { +"Authentication Complete" }
                                    style {
                                        unsafe {
                                            +"""
                                        body {
                                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
                                            display: flex;
                                            justify-content: center;
                                            align-items: center;
                                            height: 100vh;
                                            margin: 0;
                                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                                        }
                                        .container {
                                            text-align: center;
                                            background: white;
                                            padding: 3rem;
                                            border-radius: 1rem;
                                            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                                        }
                                        h1 {
                                            color: #667eea;
                                            margin-bottom: 1rem;
                                        }
                                        p {
                                            color: #666;
                                            font-size: 1.1rem;
                                        }
                                        .checkmark {
                                            font-size: 4rem;
                                            color: #4CAF50;
                                            margin-bottom: 1rem;
                                        }
                                    """.trimIndent()
                                        }
                                    }
                                }
                                body {
                                    div(classes = "container") {
                                        div(classes = "checkmark") { +"✓" }
                                        h1 { +"Authentication Complete!" }
                                        p { +"You have been successfully authenticated." }
                                        p { +"You can close this window and return to the application." }
                                    }
                                }
                            }

                            // Stop the server after handling the callback
                            scope.launch {
                                delay(1000) // Give the response time to be sent
                                stop()
                            }
                        }

                        get("/") {
                            call.respondText("OAuth Callback Server is running. Waiting for callback...")
                        }
                    }
                }.start(wait = false)

                currentPort = port
                Logger.Companion.i("LocalCallbackServer") { "Callback server started successfully on http://localhost:$currentPort" }
                return currentPort

            } catch (e: Exception) {
                Logger.Companion.d("LocalCallbackServer") { "Port $port unavailable: ${e.message}" }
                server = null
            }
        }

        Logger.Companion.e("LocalCallbackServer") { "Failed to start server - no available ports found" }
        return -1
    }

    /**
     * Stop the callback server
     */
    fun stop() {
        server?.let {
            Logger.Companion.i("LocalCallbackServer") { "Stopping callback server on port $currentPort" }
            it.stop(1000, 2000)
            server = null
            currentPort = 0
        }
    }

    /**
     * Check if the server is running
     */
    fun isRunning(): Boolean = server != null

    /**
     * Get the current port the server is running on
     * @return The port number, or 0 if not running
     */
    fun getPort(): Int = currentPort

    /**
     * Find an available port without starting the server
     * @return An available port number, or -1 if none found
     */
    fun findAvailablePort(): Int {
        for (i in 0 until MAX_PORT_ATTEMPTS) {
            val port = START_PORT + (Math.random() * (END_PORT - START_PORT)).toInt()
            try {
                ServerSocket(port).use {
                    return port
                }
            } catch (e: Exception) {
                // Port not available, try next
            }
        }
        return -1
    }
}