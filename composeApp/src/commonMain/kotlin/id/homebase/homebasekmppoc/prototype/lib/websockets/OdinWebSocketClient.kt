package id.homebase.homebasekmppoc.prototype.lib.websockets

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.prototype.lib.authentication.AuthState
import id.homebase.homebasekmppoc.lib.crypto.AesCbc
import id.homebase.homebasekmppoc.lib.crypto.ByteArrayUtil
import id.homebase.homebasekmppoc.lib.drives.TargetDrive
import id.homebase.homebasekmppoc.lib.http.SharedSecretEncryptedPayload
import id.homebase.homebasekmppoc.lib.serialization.OdinSystemSerializer
import id.homebase.homebasekmppoc.prototype.lib.toBase64
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.time.Clock



/**
 * Represents a WebSocket message received from the server
 */
@Serializable
data class WebSocketMessage(
    val content: String,
    val timestamp: Long = 0L
)

/**
 * Represents a WebSocket (encrypted) payload received from the server
 */
@Serializable
data class WebSocketClientNotificationPayload(
    val isEncrypted: Boolean,
    val payload: String
)

/**
 * Represents a WebSocket message sent to the server
 * (ported from TypeScript WebsocketCommand interface)
 */
@Serializable
data class WebsocketCommand (
    val command: String, // TS: WebSocketCommands
    val data: String
)

/**
 * Request to establish a WebSocket connection with drive subscriptions
 * (ported from TypeScript EstablishConnectionRequest interface)
 */
@Serializable
data class EstablishConnectionRequest(
    val drives: List<TargetDrive>,
)

/**
 * Represents the connection state of the WebSocket
 */
sealed class WebSocketState {
    data object Disconnected : WebSocketState()
    data object Connecting : WebSocketState()
    data object Connected : WebSocketState()
    data class Error(val message: String) : WebSocketState()
}

/**
 * WebSocket client for connecting to Odin notify/ws endpoint
 */
class OdinWebSocketClient(
    private val authenticatedState: AuthState.Authenticated
) {
    private val client = HttpClient {
        install(WebSockets)
    }

    private val sharedSecret: ByteArray = Base64.decode(authenticatedState.sharedSecret)

    private val _messages = MutableStateFlow<List<WebSocketMessage>>(emptyList())
    val messages: StateFlow<List<WebSocketMessage>> = _messages.asStateFlow()

    private val _connectionState = MutableStateFlow<WebSocketState>(WebSocketState.Disconnected)
    val connectionState: StateFlow<WebSocketState> = _connectionState.asStateFlow()

    private var connectionJob: Job? = null
    private var session: DefaultClientWebSocketSession? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Connect to the WebSocket endpoint
     */
    fun connect() {
        if (connectionJob?.isActive == true) {
            Logger.w { "WebSocket already connected or connecting" }
            return
        }

        connectionJob = scope.launch {
            try {
                _connectionState.value = WebSocketState.Connecting
                Logger.i { "Connecting to WebSocket at wss://${authenticatedState.identity}/api/owner/v1/notify/ws" }

                // Build WebSocket URL
                val wsUrl = "wss://${authenticatedState.identity}/api/owner/v1/notify/ws"

                client.webSocket(
                    urlString = wsUrl,
                    request = {
                        headers.append("Cookie", "DY0810=${authenticatedState.clientAuthToken}")
                    }
                ) {
                    session = this // Store session reference for sending messages
                    _connectionState.value = WebSocketState.Connected
                    Logger.i { "WebSocket connected successfully" }

                    establishConnectionRequest()

                    // Listen for incoming messages
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val text = frame.readText()
                                Logger.d { "Received WebSocket message: $text" }

                                val envelope = OdinSystemSerializer.deserialize<WebSocketClientNotificationPayload>(text)
                                val newMessage = decryptData(envelope)
                                _messages.value += newMessage
                            }
                            is Frame.Binary -> {
                                val bytes = frame.data
                                Logger.d { "Received binary WebSocket message (${bytes.size} bytes)" }
                                val newMessage = WebSocketMessage(
                                    content = "[Binary data: ${bytes.size} bytes]",
                                    timestamp = Clock.System.now().toEpochMilliseconds()
                                )
                                _messages.value += newMessage
                            }
                            is Frame.Close -> {
                                Logger.i { "WebSocket closed by server" }
                                _connectionState.value = WebSocketState.Disconnected
                            }
                            else -> {
                                Logger.d { "Received other frame type: ${frame.frameType}" }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.e(e) { "WebSocket connection error: ${e.message}" }
                _connectionState.value = WebSocketState.Error(e.message ?: "Unknown error")
            } finally {
                session = null // Clear session reference
                if (_connectionState.value != WebSocketState.Error("Unknown error")) {
                    _connectionState.value = WebSocketState.Disconnected
                }
                Logger.i { "WebSocket connection ended" }
            }
        }
    }

    /**
     * Disconnect from the WebSocket
     */
    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        _connectionState.value = WebSocketState.Disconnected
        Logger.i { "WebSocket disconnected" }
    }

    /**
     * Clear all received messages
     */
    fun clearMessages() {
        _messages.value = emptyList()
    }

    /**
     * Encrypts data with the shared secret
     */
    private suspend fun encryptData(message: WebsocketCommand): SharedSecretEncryptedPayload {
        val iv = ByteArrayUtil.getRndByteArray(16)
        val json = OdinSystemSerializer.serialize(message);
        val encryptedBytes = AesCbc.encrypt(json.encodeToByteArray(), sharedSecret, iv)

        // Build and return the payload
        return SharedSecretEncryptedPayload(
            iv = iv.toBase64(),
            data = encryptedBytes.toBase64()
        )
    }

    /**
     * Decrypts data with the shared secret
     */
    private suspend fun decryptData(message: WebSocketClientNotificationPayload): WebSocketMessage {
        if (!message.isEncrypted) {
            return WebSocketMessage(
                content = message.payload,
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
        }

        val encryptedPayload = OdinSystemSerializer.deserialize<SharedSecretEncryptedPayload>(message.payload)
        val iv = Base64.decode(encryptedPayload.iv)
        val encryptedData = Base64.decode(encryptedPayload.data)
        val decryptedBytes = AesCbc.decrypt(encryptedData, sharedSecret, iv)
        val json = decryptedBytes.decodeToString()

        return WebSocketMessage(
            content = json,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
    }


    /**
     * Send EstablishConnectionRequest to server
     */
    fun establishConnectionRequest() {
        scope.launch {
            try {
                val currentSession = session
                if (currentSession == null) {
                    Logger.w { "Cannot send establishConnectionRequest: WebSocket not connected" }
                    return@launch
                }

                val data = EstablishConnectionRequest(
                    drives = emptyList()
                )

                val message = WebsocketCommand(
                    command = "establishConnectionRequest",
                    data = OdinSystemSerializer.serialize(data)
                )

                val encryptedMessage = encryptData(message)

                // Serialize and send the message as JSON
                val jsonMessage = OdinSystemSerializer.serialize(encryptedMessage)
                currentSession.send(Frame.Text(jsonMessage))
                Logger.d { "Sent WebSocket establishConnectionRequest: $jsonMessage" }
            } catch (e: Exception) {
                Logger.e(e) { "Failed to send establishConnectionRequest: ${e.message}" }
            }
        }
    }

    /**
     * Send a ping message to the server
     */
    fun ping() {
        scope.launch {
            try {
                val currentSession = session
                if (currentSession == null) {
                    Logger.w { "Cannot send ping: WebSocket not connected" }
                    return@launch
                }

                // Build the command with encrypted data
                val message = WebsocketCommand(
                    command = "ping",
                    data = "ping"
                )

                val encryptedMessage = encryptData(message)

                // Serialize and send the message as JSON
                val jsonMessage = OdinSystemSerializer.serialize(encryptedMessage)
                currentSession.send(Frame.Text(jsonMessage))
                Logger.d { "Sent WebSocket ping: $jsonMessage" }
            } catch (e: Exception) {
                Logger.e(e) { "Failed to send ping: ${e.message}" }
            }
        }
    }

    /**
     * Close the client and release resources
     */
    fun close() {
        disconnect()
        client.close()
    }
}
