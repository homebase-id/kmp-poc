package id.homebase.homebasekmppoc.pages.ws

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.homebase.homebasekmppoc.authentication.AuthState
import id.homebase.homebasekmppoc.websockets.OdinWebSocketClient
import id.homebase.homebasekmppoc.websockets.WebSocketState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * A card component that displays WebSocket traffic for authenticated users.
 *
 * @param authenticatedState The authenticated state containing identity and tokens (optional)
 * @param modifier Optional modifier for the card
 */
@Composable
fun AuthenticatedWebsocketCard(
    authenticatedState: AuthState.Authenticated?,
    modifier: Modifier = Modifier
) {
    // WebSocket client
    val webSocketClient = remember(authenticatedState) {
        authenticatedState?.let { OdinWebSocketClient(it) }
    }
    val webSocketMessages by webSocketClient?.messages?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val webSocketState by webSocketClient?.connectionState?.collectAsState() ?: remember { mutableStateOf(WebSocketState.Disconnected) }

    // Cleanup WebSocket on disposal
    DisposableEffect(webSocketClient) {
        onDispose {
            webSocketClient?.close()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // WebSocket Traffic Section
            if (authenticatedState == null) {
                Text(
                    text = "Not authenticated",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "WebSocket Traffic",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Connection state indicator
                    Text(
                        text = when (webSocketState) {
                            is WebSocketState.Disconnected -> "Status: Disconnected"
                            is WebSocketState.Connecting -> "Status: Connecting..."
                            is WebSocketState.Connected -> "Status: Connected"
                            is WebSocketState.Error -> "Status: Error - ${(webSocketState as WebSocketState.Error).message}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (webSocketState) {
                            is WebSocketState.Connected -> MaterialTheme.colorScheme.tertiary
                            is WebSocketState.Error -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Connect/Disconnect buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { webSocketClient?.connect() },
                            enabled = webSocketState is WebSocketState.Disconnected || webSocketState is WebSocketState.Error
                        ) {
                            Text("Connect")
                        }

                        Button(
                            onClick = { webSocketClient?.disconnect() },
                            enabled = webSocketState is WebSocketState.Connected || webSocketState is WebSocketState.Connecting
                        ) {
                            Text("Disconnect")
                        }

                        Button(
                            onClick = { webSocketClient?.ping() },
                            enabled = webSocketState is WebSocketState.Connected
                        ) {
                            Text("Ping")
                        }

                        Button(
                            onClick = { webSocketClient?.clearMessages() },
                            enabled = webSocketMessages.isNotEmpty()
                        ) {
                            Text("Clear")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Messages display
                    if (webSocketMessages.isEmpty()) {
                        Text(
                            text = "No messages received yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .verticalScroll(rememberScrollState())
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            webSocketMessages.forEach { message ->
                                val timestamp = kotlin.time.Instant.fromEpochMilliseconds(message.timestamp)
                                    .toLocalDateTime(TimeZone.currentSystemDefault())
                                val timeStr = "${timestamp.hour.toString().padStart(2, '0')}:" +
                                        "${timestamp.minute.toString().padStart(2, '0')}:" +
                                        timestamp.second.toString().padStart(2, '0')

                                Column {
                                    Text(
                                        text = "[$timeStr]",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = message.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
