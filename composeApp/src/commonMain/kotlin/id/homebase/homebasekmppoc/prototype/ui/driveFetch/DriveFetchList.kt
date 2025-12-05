package id.homebase.homebasekmppoc.prototype.ui.driveFetch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.homebase.homebasekmppoc.lib.drives.SharedSecretEncryptedFileHeader
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun DriveFetchList(items: List<SharedSecretEncryptedFileHeader>, modifier: Modifier = Modifier) {
    LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
    ) { items(items) { item -> DriveFetchItemCard(item) } }
}

@Composable
@Preview
fun DriveFetchItemCard(item: SharedSecretEncryptedFileHeader) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = item.fileId.toString(), style = MaterialTheme.typography.titleMedium)
            if (item.fileMetadata.appData.content != null) {
                Text(
                        text = item.fileMetadata.appData.content!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                )
            }
        }
    }
}
