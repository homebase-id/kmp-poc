package id.homebase.homebasekmppoc.prototype.lib.websockets

import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.lib.config.chatTargetDrive
import id.homebase.homebasekmppoc.prototype.lib.base.CredentialsManager
import id.homebase.homebasekmppoc.prototype.lib.core.SecureByteArray
import id.homebase.homebasekmppoc.prototype.lib.crypto.AesCbc
import id.homebase.homebasekmppoc.prototype.lib.crypto.ByteArrayUtil
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import id.homebase.homebasekmppoc.prototype.lib.database.MainIndexMetaHelpers
import id.homebase.homebasekmppoc.prototype.lib.drives.ServerFile
import id.homebase.homebasekmppoc.prototype.lib.drives.TargetDrive
import id.homebase.homebasekmppoc.prototype.lib.eventbus.BackendEvent
import id.homebase.homebasekmppoc.prototype.lib.eventbus.EventBus
import id.homebase.homebasekmppoc.prototype.lib.http.SharedSecretEncryptedPayload
import id.homebase.homebasekmppoc.prototype.lib.http.appCookieName
import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import id.homebase.homebasekmppoc.prototype.toBase64
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.uuid.Uuid


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

@Serializable
data class ClientNotificationPayload(
    val notificationType: ClientNotificationType,
    val data: String = ""
)

@Serializable
data class ClientDriveNotification(
    val targetDrive: TargetDrive? = null,
    val header: ServerFile? = null,
    val previousServerFileHeader: ServerFile? = null
)

/**
 * Represents a WebSocket message sent to the server
 * (ported from TypeScript WebsocketCommand interface)
 */
@Serializable
data class WebsocketCommand(
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
    private val credentialsManager: CredentialsManager,
    private val scope: CoroutineScope,
    private val eventBus: EventBus,
    private val databaseManager: DatabaseManager
) {
    private val client = HttpClient {
        install(WebSockets)
    }

    private var fileHeaderProcessor = MainIndexMetaHelpers.HomebaseFileProcessor(databaseManager)

    private lateinit var sharedSecret: ByteArray

    private val _connectionState = MutableStateFlow<WebSocketState>(WebSocketState.Disconnected)
    val connectionState: StateFlow<WebSocketState> = _connectionState.asStateFlow()

    private var connectionJob: Job? = null
    private var session: DefaultClientWebSocketSession? = null

    /**
     * Connect to the WebSocket endpoint
     */
    suspend fun connect() {
        if (connectionJob?.isActive == true) {
            Logger.w { "WebSocket already connected or connecting" }
            return
        }

        val creds = credentialsManager.getActiveCredentials()
            ?: run {
                Logger.w { "No active credentials, cannot connect WebSocket" }
                return
            }

        val identity = creds.domain
        sharedSecret = creds.sharedSecret.unsafeBytes

        try {
            _connectionState.value = WebSocketState.Connecting
            // Build WebSocket URL
            val wsUrl = "wss://${identity}/api/apps/v1/notify/ws"
            Logger.i { "Connecting to WebSocket at $wsUrl" }

            client.webSocket(
                urlString = wsUrl,
                request = {
                    headers.append(
                        "Cookie",
                        "$appCookieName=${creds.clientAccessToken}"
                    )
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

                            val decryptedJson = decryptData(text)
                            val notification =
                                OdinSystemSerializer.deserialize<ClientNotificationPayload>(
                                    decryptedJson
                                )

                            handleNotification(notification)
                        }

//                            is Frame.Binary -> {
//                                val bytes = frame.data
//                                Logger.d { "Received binary WebSocket message (${bytes.size} bytes)" }
//                                val newMessage = WebSocketMessage(
//                                    content = "[Binary data: ${bytes.size} bytes]",
//                                    timestamp = Clock.System.now().toEpochMilliseconds()
//                                )
//                                _messages.value += newMessage
//                            }

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

    private suspend fun handleNotification(notification: ClientNotificationPayload) {
        when (notification.notificationType) {
            ClientNotificationType.deviceHandshakeSuccess -> {
                onHandshakeSuccess()
            }

            ClientNotificationType.pong -> {
                //TODO: I'm alive
                // need to setup a ping every X seconds to ensure we are still alive
            }

            ClientNotificationType.authenticationError -> {
                handleAuthError()
            }

            ClientNotificationType.fileAdded -> {
                handleFileEvent(notification)
            }

            ClientNotificationType.fileDeleted -> {
            }

            ClientNotificationType.fileModified -> {
                handleFileEvent(notification)
            }

            ClientNotificationType.connectionRequestReceived -> {
            }

            ClientNotificationType.deviceConnected -> {
            }

            ClientNotificationType.deviceDisconnected -> {
            }

            ClientNotificationType.connectionRequestAccepted -> {
            }

            ClientNotificationType.inboxItemReceived -> {
            }

            ClientNotificationType.newFollower -> {
            }

            ClientNotificationType.statisticsChanged -> {
            }

            ClientNotificationType.reactionContentAdded -> {
            }

            ClientNotificationType.reactionContentDeleted -> {
            }

            ClientNotificationType.allReactionsByFileDeleted -> {
            }

            ClientNotificationType.appNotificationAdded -> {
            }

            ClientNotificationType.introductionsReceived -> {
            }

            ClientNotificationType.introductionAccepted -> {
            }

            ClientNotificationType.connectionFinalized -> {
            }

            ClientNotificationType.error -> {
            }

            else -> {
            }
        }
    }

    private suspend fun handleFileEvent(notification: ClientNotificationPayload) {

        var theFileNotification =
            OdinSystemSerializer.deserialize<ClientDriveNotification>(notification.data)
        val theFile = theFileNotification.header!!
        val lastModified = theFile.fileMetadata.updated

        val identityId =
            Uuid.parse("7b1be23b-48bb-4304-bc7b-db5910c09a92") // TODO: <- get the real identityId
        val files = listOf(theFile.asHomebaseFile(SecureByteArray(sharedSecret)))

        try {
            fileHeaderProcessor.baseUpsertEntryZapZap(
                identityId = identityId,
                driveId = chatTargetDrive.alias,
                fileHeaders = files,
                cursor = null
            )
        } catch (e: Exception) {
            Logger.e("DB upsert failed for batch: ${e.message}")
        }

        eventBus.emit(
            BackendEvent.DriveEvent.BatchReceived(
                theFile.driveId,
                1,
                1,
                lastModified,
                files
            )
        )
    }

    private suspend fun handleAuthError() {
        eventBus.emit(BackendEvent.GoingOffline)
    }

    private suspend fun onHandshakeSuccess() {
        Logger.i { "Device handshake successful" }
        eventBus.emit(BackendEvent.GoingOnline)
    }

    /**
     *
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
//        _messages.value = emptyList()
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

    private suspend fun decryptData(
        text: String
    ): String {

        val envelope = OdinSystemSerializer.deserialize<WebSocketClientNotificationPayload>(text)
        if (!envelope.isEncrypted) {
            return envelope.payload
        }

        val encryptedPayload =
            OdinSystemSerializer.deserialize<SharedSecretEncryptedPayload>(envelope.payload)

        val iv = Base64.decode(encryptedPayload.iv)
        val encryptedData = Base64.decode(encryptedPayload.data)
        val decryptedBytes = AesCbc.decrypt(encryptedData, sharedSecret, iv)

        return decryptedBytes.decodeToString()
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
                    drives = listOf(chatTargetDrive)
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
