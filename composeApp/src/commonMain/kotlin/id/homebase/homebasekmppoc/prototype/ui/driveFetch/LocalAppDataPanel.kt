package id.homebase.homebasekmppoc.prototype.ui.driveFetch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import id.homebase.homebasekmppoc.prototype.lib.drives.files.LocalAppMetadata
import kotlin.uuid.Uuid
import androidx.compose.runtime.mutableStateListOf


@Composable
fun LocalAppDataPanel(
    localAppData: LocalAppMetadata?,
    onSaveContent: (String) -> Unit,
    onSaveTags: (List<Uuid>) -> Unit
) {
    var showContentDialog by remember { mutableStateOf(false) }
    var showTagsDialog by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "Local App Data",
            style = MaterialTheme.typography.titleSmall
        )

        Spacer(Modifier.height(8.dp))

        val hasLocalAppData = localAppData != null
        val hasContent = !localAppData?.content.isNullOrBlank()
        val hasTags = !localAppData?.tags.isNullOrEmpty()

        when {
            // ── 1. No LocalAppMetadata ───────────────────────
            !hasLocalAppData -> {
                Text(
                    text = "No local app data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showContentDialog = true }) {
                        Text("Add Content")
                    }
                    Button(onClick = { showTagsDialog = true }) {
                        Text("Add Tags")
                    }
                }
            }

            // ── 2. Metadata exists, content blank ────────────
            hasLocalAppData && !hasContent -> {
                Text(
                    text = "No content set",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showContentDialog = true }) {
                        Text("Add Content")
                    }
                    Button(onClick = { showTagsDialog = true }) {
                        Text(if (hasTags) "Edit Tags" else "Add Tags")
                    }
                }
            }

            // ── 3. Metadata exists, content present ───────────
            else -> {
                LabeledValue("Content", localAppData!!.content)

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showContentDialog = true }) {
                        Text("Edit Content")
                    }
                    Button(onClick = { showTagsDialog = true }) {
                        Text(if (hasTags) "Edit Tags" else "Add Tags")
                    }
                }
            }
        }

    }

    if (showContentDialog) {
        LocalAppDataContentDialog(
            initialContent = localAppData?.content,
            onDismiss = { showContentDialog = false },
            onSave = {
                onSaveContent(it)
                showContentDialog = false
            }
        )
    }

    if (showTagsDialog) {
        LocalAppDataTagsDialog(
            initialTags = localAppData?.tags,
            onDismiss = { showTagsDialog = false },
            onSave = {
                onSaveTags(it)
                showTagsDialog = false
            }
        )
    }
}


@Composable
fun LocalAppDataContentDialog(
    initialContent: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var content by remember { mutableStateOf(initialContent ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = content.isNotBlank(),
                onClick = { onSave(content) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Local App Data – Content") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                Text(
                    text = "Content is free-form text data (for example JSON or any other format you choose).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        }
    )
}

@Composable
fun LocalAppDataTagsDialog(
    initialTags: List<Uuid>?,
    onDismiss: () -> Unit,
    onSave: (List<Uuid>) -> Unit
) {
    val tags = remember {
        mutableStateListOf<Uuid>().apply {
            initialTags?.let { addAll(it) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(tags.toList()) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Local App Data – Tags") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                Text(
                    text = "A tag is just a UUID value to which you assign the meaning. " +
                            "Here we simply drop in a random one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                tags.forEach { tag ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tag.toString(),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(onClick = { tags.remove(tag) }) {
                            Text("Remove")
                        }
                    }
                }

                Button(onClick = { tags.add(Uuid.random()) }) {
                    Text("Add random tag")
                }
            }
        }
    )
}

