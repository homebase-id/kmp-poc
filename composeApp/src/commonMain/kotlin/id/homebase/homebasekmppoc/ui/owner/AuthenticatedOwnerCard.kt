package id.homebase.homebasekmppoc.ui.owner

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import homebasekmppoc.composeapp.generated.resources.Res
import homebasekmppoc.composeapp.generated.resources.compose_multiplatform
import id.homebase.homebasekmppoc.lib.authentication.AuthState
import id.homebase.homebasekmppoc.lib.drives.FileState
import id.homebase.homebasekmppoc.lib.drives.SharedSecretEncryptedFileHeader
import id.homebase.homebasekmppoc.lib.drives.SystemDriveConstants
import id.homebase.homebasekmppoc.lib.http.OdinHttpClient
import id.homebase.homebasekmppoc.lib.http.PayloadPlayground
import id.homebase.homebasekmppoc.lib.http.PublicPostsChannelDrive
import id.homebase.homebasekmppoc.lib.image.toImageBitmap
import org.jetbrains.compose.resources.painterResource

/**
 * A card component that displays authenticated owner information with data from backend.
 *
 * @param authenticatedState The authenticated state containing identity and tokens (optional)
 * @param onVideoClick Callback when a video is clicked, receives the video header
 * @param modifier Optional modifier for the card
 */
@Composable
fun AuthenticatedOwnerCard(
    authenticatedState: AuthState.Authenticated?,
    onVideoClick: (SharedSecretEncryptedFileHeader) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var verifytokenReponse by remember { mutableStateOf<String?>(null) }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var videoHeaders by remember { mutableStateOf<List<SharedSecretEncryptedFileHeader>?>(null) }


    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(authenticatedState != null) }

    // Fetch data from backend when component loads
    LaunchedEffect(authenticatedState) {
        if (authenticatedState != null) {
            try {
                val client = OdinHttpClient(authenticatedState)
                verifytokenReponse = client.verifyOwnerToken()

                val payloadPlayground = PayloadPlayground(authenticatedState)
                val drives = payloadPlayground.getDrivesByType(SystemDriveConstants.publicPostChannelDrive.type)
                imageBytes = payloadPlayground.getImage()

                videoHeaders = payloadPlayground.getVideosOnDrive(
                    PublicPostsChannelDrive.alias,
                    PublicPostsChannelDrive.type)

                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error"
                Logger.e(e) { "$errorMessage" }
                isLoading = false
            }
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
            // User avatar/image
            Image(
                painter = painterResource(Res.drawable.compose_multiplatform),
                contentDescription = "Owner avatar",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Owner identity
            Text(
                text = authenticatedState?.identity ?: "Not authenticated",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Backend data or loading indicator
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Loading data...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                errorMessage != null -> {
                    Text(
                        text = "Error: $errorMessage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    // Verify Token Response Section
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
                            text = "verifyToken response",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = verifytokenReponse ?: "No data",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Payload Image Section
                    val imageBitmap = remember(imageBytes) {
                        imageBytes?.toImageBitmap()
                    }

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
                            text = "Payload Image",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        when {
                            imageBitmap != null -> {
                                Image(
                                    bitmap = imageBitmap,
                                    contentDescription = "Retrieved payload image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            imageBytes != null -> {
                                Text(
                                    text = "Error displaying image",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }
                            else -> {
                                Text(
                                    text = "No image data",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
                            text = "Video list",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        when {
                            videoHeaders != null && videoHeaders!!.isNotEmpty() -> {
                                val headerCount = videoHeaders?.size ?: 0
                                Text(
                                    text = "Header count: $headerCount",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                videoHeaders!!.forEachIndexed { index, header ->
                                    Button(
                                        onClick = { onVideoClick(header) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Video ${index + 1}")
                                    }
                                    if (index < videoHeaders!!.size - 1) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                            else -> {
                                Text(
                                    text = "No header data",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }




                }
            }
        }
    }
}

//@Preview
//@Composable
//fun AuthenticatedOwnerCardPreview() {
//    MaterialTheme {
//        AuthenticatedOwnerCard(
//            authenticatedState = AuthState.Authenticated(
//                identity = "frodo.dotyou.cloud",
//                clientAuthToken = "mock-token",
//                sharedSecret = "mock-secret"
//            ),
//            modifier = Modifier.padding(16.dp)
//        )
//    }
//}
