package id.homebase.homebasekmppoc.lib.youauth

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.lib.youAuth.YouAuthFlowManager
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.respondHtml
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.net.ServerSocket
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

/**
 * Local HTTP server for handling YouAuth callbacks on desktop. Listens on
 * http://localhost:{PORT}/callback where PORT is dynamically assigned
 */
object LocalCallbackServer {
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? =
            null
    private var currentPort: Int = 0
    private const val START_PORT = 49152 // Start of dynamic/private port range
    private const val END_PORT = 65535 // End of port range
    private const val MAX_PORT_ATTEMPTS = 100

    /**
     * Start the callback server on an available port
     * @param scope CoroutineScope for handling callbacks
     * @param preferredPort Optional preferred port to use, or 0 to auto-select
     * @return The port number the server is running on, or -1 if failed
     */
    fun start(scope: CoroutineScope, preferredPort: Int = 0): Int {
        if (server != null) {
            Logger.w("LocalCallbackServer") {
                "Server already running on port $currentPort"
            }
            return currentPort
        }

        // If a preferred port was specified, try it first
        val portsToTry =
                if (preferredPort > 0) {
                    listOf(preferredPort) +
                            (0 until MAX_PORT_ATTEMPTS).map {
                                START_PORT + (Math.random() * (END_PORT - START_PORT)).toInt()
                            }
                } else {
                    (0 until MAX_PORT_ATTEMPTS).map {
                        START_PORT + (Math.random() * (END_PORT - START_PORT)).toInt()
                    }
                }

        for (port in portsToTry) {
            try {
                Logger.d("LocalCallbackServer") {
                    "Attempting to start server on port $port"
                }

                server =
                        embeddedServer(CIO, port = port) {
                                    routing {
                                        get("/authorization-code-callback") {
                                            // Get the full URL with query parameters
                                            val fullUrl =
                                                    call.request.local.let { local ->
                                                        "http://localhost:$currentPort${local.uri}"
                                                    }

                                            Logger.d("LocalCallbackServer") {
                                                "Received callback: $fullUrl"
                                            }

                                            // Handle the callback asynchronously
                                            scope.launch {
                                                try {
                                                    YouAuthFlowManager.handleCallback(fullUrl)
                                                    Logger.i("LocalCallbackServer") {
                                                        "Callback handled successfully"
                                                    }
                                                } catch (e: Exception) {
                                                    Logger.e("LocalCallbackServer") {
                                                        "Error handling callback: ${e.message}"
                                                    }
                                                }
                                            }

                                            // Return a success page
                                            call.respondHtml {
                                                head {
                                                    title { +"Authentication Complete" }
                                                    style {
                                                        unsafe {
                                                            +"""
                    :root {
                        --bg-color: #ffffff;
                        --text-primary: #171717;
                        --text-secondary: #666666;
                        --border-color: #eaeaea;
                        --success-color: #22c55e;
                    }

                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        -webkit-font-smoothing: antialiased;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        margin: 0;
                        background-color: var(--bg-color);
                        color: var(--text-primary);
                    }

                    .container {
                        width: 100%;
                        max-width: 400px;
                        padding: 2.5rem;
                        text-align: center;
                        /* Minimalist border instead of heavy shadow */
                        border: 1px solid var(--border-color); 
                        border-radius: 12px;
                    }

                    .icon-wrapper {
                        display: inline-flex;
                        align-items: center;
                        justify-content: center;
                        width: 48px;
                        height: 48px;
                        margin-bottom: 1.5rem;
                        border-radius: 50%;
                        background-color: #f0fdf4; /* Very subtle green bg */
                        color: var(--success-color);
                    }

                    .checkmark {
                        font-size: 24px;
                        line-height: 1;
                        font-weight: bold;
                    }

                    h1 {
                        font-size: 1.25rem;
                        font-weight: 600;
                        margin: 0 0 0.75rem 0;
                        letter-spacing: -0.025em;
                        color: var(--text-primary);
                    }

                    p {
                        margin: 0;
                        font-size: 0.95rem;
                        line-height: 1.5;
                        color: var(--text-secondary);
                    }

                    .secondary-text {
                        margin-top: 0.5rem;
                        font-size: 0.875rem;
                    }
                """.trimIndent()
                                                        }
                                                    }
                                                }
                                                body {
                                                    div(classes = "container") {
                                                        div(classes = "icon-wrapper") {
                                                            div(classes = "checkmark") { +"✓" }
                                                        }
                                                        h1 { +"Authentication Complete" }
                                                        p {
                                                            +"You have been successfully authenticated."
                                                        }
                                                        p(classes = "secondary-text") {
                                                            +"You can close this window now."
                                                        }
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
                                            call.respondText(
                                                    "OAuth Callback Server is running. Waiting for callback..."
                                            )
                                        }
                                    }
                                }
                                .start(wait = false)

                currentPort = port
                Logger.i("LocalCallbackServer") {
                    "Callback server started successfully on http://localhost:$currentPort"
                }
                return currentPort
            } catch (e: Exception) {
                Logger.d("LocalCallbackServer") { "Port $port unavailable: ${e.message}" }
                server = null
            }
        }

        Logger.e("LocalCallbackServer") {
            "Failed to start server - no available ports found"
        }
        return -1
    }

    /** Stop the callback server */
    fun stop() {
        server?.let {
            Logger.i("LocalCallbackServer") {
                "Stopping callback server on port $currentPort"
            }
            it.stop(1000, 2000)
            server = null
            currentPort = 0
        }
    }

    /** Check if the server is running */
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
