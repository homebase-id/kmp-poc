package id.homebase.homebasekmppoc.prototype.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import id.homebase.homebasekmppoc.prototype.lib.crypto.ContentDecryptor
import id.homebase.homebasekmppoc.prototype.lib.drives.SharedSecretEncryptedFileHeader

/**
 * List of conversations (fileType 8888). Clicking a conversation navigates to its messages.
 *
 * @param items List of conversation file headers
 * @param sharedSecret Base64-encoded shared secret for decryption (optional)
 * @param onConversationClicked Callback with conversationId when clicked
 */
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
                        // Use uniqueId as the conversationId for groupId filter
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
    // State for decrypted content
    var displayContent by remember { mutableStateOf<String?>(null) }
    var isDecrypting by remember { mutableStateOf(false) }

    // Decrypt content when sharedSecret is available and content is encrypted
    LaunchedEffect(item.fileId, sharedSecret) {
        val rawContent = item.fileMetadata.appData.content
        if (rawContent != null && item.fileMetadata.isEncrypted && sharedSecret != null) {
            isDecrypting = true
            displayContent =
                    ContentDecryptor.decryptContent(item, sharedSecret)
                            ?: rawContent // Fallback to encrypted if decryption fails
            isDecrypting = false
        } else {
            displayContent = rawContent
        }
    }

    Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Conversation title/content preview
            when {
                isDecrypting ->
                        Text(
                                text = "Decrypting...",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                displayContent != null ->
                        Text(
                                text = displayContent!!,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2
                        )
                else -> Text(text = "Conversation", style = MaterialTheme.typography.titleMedium)
            }

            // Encryption indicator
            if (item.fileMetadata.isEncrypted) {
                Text(
                        text = "🔒 Encrypted",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                )
            }

            // UniqueId (this will be the groupId for messages)
            val uniqueId = item.fileMetadata.appData.uniqueId?.toString() ?: item.fileId.toString()
            val displayId = if (uniqueId.length > 20) uniqueId.take(20) + "..." else uniqueId

            Text(
                    text = "ID: $displayId",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Created timestamp
            Text(
                    text = "Created: ${item.fileMetadata.created}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(modifier = Modifier.align(Alignment.End), onClick = onClick) {
                Text("View Messages")
            }
        }
    }
}

/**
 * List of messages for a conversation (fileType 7878).
 *
 * @param items List of message file headers
 * @param sharedSecret Base64-encoded shared secret for decryption (optional)
 * @param onMessageClicked Callback with driveId and fileId when clicked
 */
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
                    onClick = { onMessageClicked(item.driveId.toString(), item.fileId.toString()) }
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
    val payloads = item.fileMetadata.payloads.orEmpty()
    val payloadCount = payloads.size

    // State for decrypted content
    var displayContent by remember { mutableStateOf<String?>(null) }
    var isDecrypting by remember { mutableStateOf(false) }

    // Decrypt content when sharedSecret is available and content is encrypted
    LaunchedEffect(item.fileId, sharedSecret) {
        val rawContent = item.fileMetadata.appData.content
        if (rawContent != null && item.fileMetadata.isEncrypted && sharedSecret != null) {
            isDecrypting = true
            displayContent =
                    ContentDecryptor.decryptContent(item, sharedSecret)
                            ?: rawContent // Fallback to encrypted if decryption fails
            isDecrypting = false
        } else {
            displayContent = rawContent
        }
    }

    Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // File ID (truncated for readability)
            val fileIdStr = item.fileId.toString()
            val displayId = if (fileIdStr.length > 16) fileIdStr.take(16) + "..." else fileIdStr

            LabeledValue(label = "Message ID", value = displayId)

            // Summary row
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoChip(
                        label = "Encrypted",
                        value = if (item.fileMetadata.isEncrypted) "Yes" else "No"
                )

                InfoChip(label = "Payloads", value = payloadCount.toString())
            }

            // Content preview - this is the key field showing message content
            when {
                isDecrypting ->
                        Text(
                                text = "Decrypting...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                displayContent != null ->
                        Text(
                                text = displayContent!!,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3
                        )
                else ->
                        Text(
                                text = "(No content preview available)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
            }

            Button(modifier = Modifier.align(Alignment.End), onClick = onClick) {
                Text("View details")
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column {
        Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column {
        Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}
