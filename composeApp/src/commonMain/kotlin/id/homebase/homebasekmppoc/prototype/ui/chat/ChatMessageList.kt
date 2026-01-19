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
import id.homebase.homebasekmppoc.prototype.lib.chat.ChatMessageData
import id.homebase.homebasekmppoc.prototype.lib.chat.ConversationData
import id.homebase.homebasekmppoc.prototype.lib.drives.files.ThumbnailDescriptor
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * List of conversations (fileType 8888). Clicking a conversation navigates to its messages.
 *
 * Note: Content is already decrypted by ConversationProvider.
 */
@Composable
fun ConversationList(
        items: List<ConversationData>,
        modifier: Modifier = Modifier,
        onConversationClicked: (conversationId: String) -> Unit
) {
        LazyColumn(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                items(items, key = { it.fileId.toString()}) { item ->
                        ConversationCard(
                                item = item,
                                onClick = { onConversationClicked(item.uniqueId.toString()) }
                        )
                }
        }
}

@Composable
fun ConversationCard(item: ConversationData, onClick: () -> Unit) {
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
                                item.previewThumbnail?.let { thumb ->
                                        ThumbnailImage(
                                                thumbnail = thumb,
                                                modifier =
                                                        Modifier.size(100.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                        // Show parsed data - already decrypted
                                        Text("Title: ${item.content.title}")
                                        if (item.content.recipients.isNotEmpty()) {
                                                Text(
                                                        "Recipients: ${item.content.recipients.joinToString(", ")}"
                                                )
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        // Metadata
                                        Text("ID: ${item.uniqueId}")
                                        Text("Created: ${item.created}")
//                                        Text("Encrypted: ${item.isEncrypted}")

                                        Text("Tiny thumb: ${item.previewThumbnail?.content}")
                                        Text("Conversation Id: ${item.conversationMeta?.conversationId}")
                                        Text("Last-read time: ${item.conversationMeta?.lastReadTime}")
                                        Text("Unread count: BISHWA TODO ??? let's discuss how - loaded from localAppData probably?")
                                        Text("Last-message-text: BISHWA TODO 40 CHARS STORED IN APPDATA")
                                        Text("Last-message-timestamp: BISHWA TODO UNIXTIMEUTC STORED IN APPDATA")
                                        Text("Last-message-delivery-status: BISHWA TODO DELIVERY STATUS STORED IN APPDATA")
                                        Text("Reaction-summary: BISHWA ADD REACTION SUMMARY")
                                }
                        }

                        Spacer(Modifier.height(8.dp))
                        Button(modifier = Modifier.align(Alignment.End), onClick = onClick) {
                                Text("View Messages")
                        }
                }
        }
}

/**
 * List of messages for a conversation (fileType 7878).
 *
 * Note: Content is already decrypted by ChatMessageProvider.
 */
@Composable
fun ChatMessageList(
        items: List<ChatMessageData>,
        modifier: Modifier = Modifier,
        onMessageClicked: (driveId: String, fileId: String) -> Unit
) {
        LazyColumn(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                items(items, key = { it.fileId }) { item ->
                        ChatMessageCard(
                                item = item,
                                onClick = { onMessageClicked(item.driveId, item.fileId.toString()) }
                        )
                }
        }
}

@Composable
fun ChatMessageCard(item: ChatMessageData, onClick: () -> Unit) {
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
                                item.previewThumbnail?.let { thumb ->
                                        ThumbnailImage(
                                                thumbnail = thumb,
                                                modifier =
                                                        Modifier.size(100.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                        // Show parsed message data - already decrypted
                                        Text("Message: ${item.content.message}")
                                        Text("IsEdited?: ${item.content.isEdited}")
                                        Text("More text?: ${!item.contentIsComplete}")
                                        Text("More text function: BISHWA-TODO-FUCNTION THAT RETURNS FULL TEXT CLICKING more...")

                                        Text(
                                                "Delivery Status: ${item.content.getDeliveryStatusEnum()?.name ?: "Unknown (${item.content.deliveryStatus})"}"
                                        )
                                        if (item.content.replyId != null) {
                                                Text("Reply To: ${item.content.replyId}")
                                        }
                                        if (item.content.isEdited) {
                                                Text("Edited: Yes")
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        // Metadata
                                        Text("ID: ${item.fileId}")
                                        Text("Created: ${item.created}")
                                        Text("Encrypted: ${item.isEncrypted}")
                                        Text("Payloads: ${item.payloads?.size ?: 0}")
                                        item.sender?.let { Text("Sender: $it") }
                                        Text("tiny Thumb: ${item.previewThumbnail?.content}")

                                        Text("Reply-preview: BISHWA-TODO output the reply preview from AppData (no load)")
                                        Text("URL-preview: BISHWA-TODO output the URL preview from local contentDescriptor (no load)")
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
