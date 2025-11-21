package id.homebase.homebasekmppoc.pages.owner

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
import id.homebase.homebasekmppoc.authentication.AuthState
import id.homebase.homebasekmppoc.http.OdinHttpClient
import id.homebase.homebasekmppoc.util.toImageBitmap
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A card component that displays authenticated owner information with data from backend.
 *
 * @param authenticatedState The authenticated state containing identity and tokens (optional)
 * @param modifier Optional modifier for the card
 */
@Composable
fun AuthenticatedOwnerCard(
    authenticatedState: AuthState.Authenticated?,
    modifier: Modifier = Modifier
) {
    var verifytokenReponse by remember { mutableStateOf<String?>(null) }
    var payloadResponse by remember { mutableStateOf<ByteArray?>(null) }
//    var isAuthenticatedResponse by remember { mutableStateOf<String?>(null) }
//    var pingResponse by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(authenticatedState != null) }

    // Fetch data from backend when component loads
    LaunchedEffect(authenticatedState) {
        if (authenticatedState != null) {
            try {
                val client = OdinHttpClient(authenticatedState)
                verifytokenReponse = client.verifyOwnerToken()
                // payloadResponse = client.getPayloadBytes()

                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error"
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
                    val imageBitmap = remember(payloadResponse) {
                        payloadResponse?.toImageBitmap()
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
                            payloadResponse != null -> {
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
