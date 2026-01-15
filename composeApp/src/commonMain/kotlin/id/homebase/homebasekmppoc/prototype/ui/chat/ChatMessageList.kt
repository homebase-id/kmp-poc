package id.homebase.homebasekmppoc.prototype.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import id.homebase.homebasekmppoc.prototype.lib.chat.ChatMessageContent
import id.homebase.homebasekmppoc.prototype.lib.chat.UnifiedConversation
import id.homebase.homebasekmppoc.prototype.lib.crypto.ContentDecryptor
import id.homebase.homebasekmppoc.prototype.lib.drives.SharedSecretEncryptedFileHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.files.ThumbnailDescriptor
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

/** List of conversations (fileType 8888). Clicking a conversation navigates to its messages. */
@Composable
fun ConversationList(
        items: List<SharedSecretEncryptedFileHeader>,
        modifier: Modifier = Modifier,
        sharedSecret: String? = null,
        onConversationClicked: (conversationId: String) -> Unit
) {
        LazyColumn(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                items(items, key = { it.fileId.toString() }) { item ->
                        ConversationCard(
                                item = item,
                                sharedSecret = sharedSecret,
                                onClick = {
                                        val uniqueId =
                                                item.fileMetadata.appData.uniqueId?.toString()
                                                        ?: item.fileId.toString()
                                        onConversationClicked(uniqueId)
                                }
                        )
                }
        }
}

@Composable
fun ConversationCard(
        item: SharedSecretEncryptedFileHeader,
        sharedSecret: String? = null,
        onClick: () -> Unit
) {
        var parsedConversation by remember { mutableStateOf<UnifiedConversation?>(null) }
        var rawContent by remember { mutableStateOf<String?>(null) }
        var isDecrypting by remember { mutableStateOf(false) }

        val previewThumbnail = item.fileMetadata.appData.previewThumbnail

        LaunchedEffect(item.fileId, sharedSecret) {
                val content = item.fileMetadata.appData.content
                if (content != null && item.fileMetadata.isEncrypted && sharedSecret != null) {
                        isDecrypting = true
                        val decrypted =
                                ContentDecryptor.decryptContent(item, sharedSecret) ?: content
                        rawContent = decrypted
                        parsedConversation = parseConversation(decrypted)
                        isDecrypting = false
                } else {
                        rawContent = content
                        parsedConversation = content?.let { parseConversation(it) }
                }
        }

        Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
                Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                        ) {
                                // 100x100 preview thumbnail
                                previewThumbnail?.let { thumb ->
                                        ThumbnailImage(
                                                thumbnail = thumb,
                                                modifier =
                                                        Modifier.size(100.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                        // Show parsed data if available
                                        if (parsedConversation != null) {
                                                val conv = parsedConversation!!
                                                Text("Title: ${conv.title}")
                                                if (conv.recipients.isNotEmpty()) {
                                                        Text(
                                                                "Recipients: ${conv.recipients.joinToString(", ")}"
                                                        )
                                                }
                                        } else if (isDecrypting) {
                                                Text("Decrypting...")
                                        } else if (rawContent != null) {
                                                Text("Content: ${rawContent!!.take(100)}")
                                        } else {
                                                Text("(No content)")
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        // Metadata
                                        Text(
                                                "ID: ${item.fileMetadata.appData.uniqueId ?: item.fileId}"
                                        )
                                        Text("Created: ${item.fileMetadata.created}")
                                        Text("Encrypted: ${item.fileMetadata.isEncrypted}")
                                }
                        }

                        Spacer(Modifier.height(8.dp))
                        Button(modifier = Modifier.align(Alignment.End), onClick = onClick) {
                                Text("View Messages")
                        }
                }
        }
}

/** List of messages for a conversation (fileType 7878). */
@Composable
fun ChatMessageList(
        items: List<SharedSecretEncryptedFileHeader>,
        modifier: Modifier = Modifier,
        sharedSecret: String? = null,
        onMessageClicked: (driveId: String, fileId: String) -> Unit
) {
        LazyColumn(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                items(items, key = { it.fileId.toString() }) { item ->
                        ChatMessageCard(
                                item = item,
                                sharedSecret = sharedSecret,
                                onClick = {
                                        onMessageClicked(
                                                item.driveId.toString(),
                                                item.fileId.toString()
                                        )
                                }
                        )
                }
        }
}

@Composable
fun ChatMessageCard(
        item: SharedSecretEncryptedFileHeader,
        sharedSecret: String? = null,
        onClick: () -> Unit
) {
        var parsedMessage by remember { mutableStateOf<ChatMessageContent?>(null) }
        var rawContent by remember { mutableStateOf<String?>(null) }
        var isDecrypting by remember { mutableStateOf(false) }

        val previewThumbnail =
                item.fileMetadata.appData.previewThumbnail
                        ?: item.fileMetadata.payloads?.firstOrNull()?.previewThumbnail

        LaunchedEffect(item.fileId, sharedSecret) {
                val content = item.fileMetadata.appData.content
                if (content != null && item.fileMetadata.isEncrypted && sharedSecret != null) {
                        isDecrypting = true
                        val decrypted =
                                ContentDecryptor.decryptContent(item, sharedSecret) ?: content
                        rawContent = decrypted
                        parsedMessage = parseMessage(decrypted)
                        isDecrypting = false
                } else {
                        rawContent = content
                        parsedMessage = content?.let { parseMessage(it) }
                }
        }

        Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
                Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                        ) {
                                // 100x100 preview thumbnail
                                previewThumbnail?.let { thumb ->
                                        ThumbnailImage(
                                                thumbnail = thumb,
                                                modifier =
                                                        Modifier.size(100.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                        // Show parsed message data if available
                                        if (parsedMessage != null) {
                                                val msg = parsedMessage!!
                                                Text("Message: ${msg.message}")
                                                Text(
                                                        "Delivery Status: ${msg.getDeliveryStatusEnum()?.name ?: "Unknown (${msg.deliveryStatus})"}"
                                                )
                                                if (msg.replyId != null) {
                                                        Text("Reply To: ${msg.replyId}")
                                                }
                                                if (msg.isEdited) {
                                                        Text("Edited: Yes")
                                                }
                                        } else if (isDecrypting) {
                                                Text("Decrypting...")
                                        } else if (rawContent != null) {
                                                Text("Content: ${rawContent!!.take(150)}")
                                        } else {
                                                Text("(No content)")
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        // Metadata
                                        Text("ID: ${item.fileId}")
                                        Text("Created: ${item.fileMetadata.created}")
                                        Text("Encrypted: ${item.fileMetadata.isEncrypted}")
                                        Text("Payloads: ${item.fileMetadata.payloads?.size ?: 0}")
                                        item.fileMetadata.senderOdinId?.let { Text("Sender: $it") }
                                }
                        }

                        Spacer(Modifier.height(8.dp))
                        Button(modifier = Modifier.align(Alignment.End), onClick = onClick) {
                                Text("View Details")
                        }
                }
        }
}

/** Composable to display a 100x100 thumbnail image from a ThumbnailDescriptor. */
@OptIn(ExperimentalEncodingApi::class)
@Composable
fun ThumbnailImage(thumbnail: ThumbnailDescriptor, modifier: Modifier = Modifier) {
        var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(thumbnail.content) {
                thumbnail.content?.let { base64Content ->
                        try {
                                val bytes = Base64.decode(base64Content)
                                imageBitmap = decodeImageBitmap(bytes)
                        } catch (e: Exception) {
                                imageBitmap = null
                        }
                }
        }

        imageBitmap?.let { bitmap ->
                Image(
                        bitmap = bitmap,
                        contentDescription = "Preview thumbnail",
                        modifier = modifier,
                        contentScale = ContentScale.Crop
                )
        }
                ?: run {
                        thumbnail.content?.let {
                                Text(
                                        text =
                                                "📷 ${thumbnail.pixelWidth ?: 0}x${thumbnail.pixelHeight ?: 0}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = modifier
                                )
                        }
                }
}

/** Platform-specific function to decode image bytes to ImageBitmap. */
expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap?

/** Parse content as UnifiedConversation. */
private fun parseConversation(content: String): UnifiedConversation? {
        return try {
                json.decodeFromString<UnifiedConversation>(content)
        } catch (e: Exception) {
                null
        }
}

/** Parse content as ChatMessageContent. */
private fun parseMessage(content: String): ChatMessageContent? {
        return try {
                json.decodeFromString<ChatMessageContent>(content)
        } catch (e: Exception) {
                null
        }
}
