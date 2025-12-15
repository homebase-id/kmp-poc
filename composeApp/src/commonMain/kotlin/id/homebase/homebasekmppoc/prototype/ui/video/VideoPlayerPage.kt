package id.homebase.homebasekmppoc.prototype.ui.video

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.homebase.homebasekmppoc.prototype.lib.drives.PayloadDescriptor
import id.homebase.homebasekmppoc.prototype.lib.http.AppOrOwner
import id.homebase.homebasekmppoc.prototype.lib.http.PayloadWrapper
import id.homebase.homebasekmppoc.prototype.lib.video.LocalVideoServer

/**
 * Page for playing HLS videos
 *
 */
@Composable
fun VideoPlayerPage(
    appOrOwner:  AppOrOwner,
    localVideoServer: LocalVideoServer,
    videoPayload: PayloadWrapper,
    videoTitle: String = "Video Player",
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Header
        Text(
            text = videoTitle,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Video player card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            VideoPlayerContainer(appOrOwner, localVideoServer, videoPayload)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Video info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "some info here",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Back button
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Video List")
        }
    }
}

//
