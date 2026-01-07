package id.homebase.homebasekmppoc.prototype.ui.driveFetch
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.homebase.homebasekmppoc.prototype.lib.drives.SharedSecretEncryptedFileHeader

@Composable
fun DriveFetchList(
    items: List<SharedSecretEncryptedFileHeader>,
    modifier: Modifier = Modifier,
    onFileClicked: (String, String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.fileId.toString() }) { item ->
            DriveFetchItemCard(
                item = item,
                onClick = { onFileClicked(item.driveId.toString(), item.fileId.toString()) }
            )
        }
    }
}

@Composable
fun DriveFetchItemCard(
    item: SharedSecretEncryptedFileHeader,
    onClick: () -> Unit
) {
    val payloads = item.fileMetadata.payloads.orEmpty()

    val payloadCount = payloads.size
    val thumbnailCount =
        payloads.sumOf { payload ->
            val thumbs = payload.thumbnails ?: emptyList()
            val preview = payload.previewThumbnail
            thumbs.size + if (preview != null) 1 else 0
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── File ID ─────────────────────────────
            LabeledValue(
                label = "File ID",
                value = item.fileId.toString()
            )

            // ── Summary row ─────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip(
                    label = "Encrypted",
                    value = if (item.fileMetadata.isEncrypted) "Yes" else "No"
                )

                InfoChip(
                    label = "Payloads",
                    value = payloadCount.toString()
                )

                InfoChip(
                    label = "Thumbnails",
                    value = thumbnailCount.toString()
                )
            }

            // ── Content preview ─────────────────────
            item.fileMetadata.appData.content?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
            }

            Button(
                modifier = Modifier.align(Alignment.End),
                onClick = onClick
            ) {
                Text("View details")
            }
        }
    }
}



@Composable
private fun InfoChip(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
