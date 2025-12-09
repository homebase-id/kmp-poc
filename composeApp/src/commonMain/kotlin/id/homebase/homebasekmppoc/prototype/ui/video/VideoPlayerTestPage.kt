package id.homebase.homebasekmppoc.prototype.ui.video

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.prototype.lib.authentication.AuthState
import id.homebase.homebasekmppoc.prototype.lib.authentication.AuthenticationManager
import id.homebase.homebasekmppoc.prototype.lib.http.PayloadPlayground
import id.homebase.homebasekmppoc.prototype.lib.http.PayloadWrapper
import id.homebase.homebasekmppoc.prototype.lib.http.PublicPostsChannelDrive
import id.homebase.homebasekmppoc.prototype.lib.video.LocalVideoServer
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Stub function to get video metadata from a video header
 * TODO: Implement actual video metadata extraction
 */

/**
 * Test page for video playback with local HTTP server
 * Supports both HLS and regular video playback
 */
@Composable
fun VideoPlayerTestPage(authenticationManager: AuthenticationManager) {
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var odinIdentity by remember { mutableStateOf("frodo.dotyou.cloud") }
    var password by remember { mutableStateOf("a") }

    var showResultDialog by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    var videoHeaders by remember { mutableStateOf<List<PayloadWrapper>?>(null) }
    var selectedVideoHeader by remember { mutableStateOf<PayloadWrapper?>(null) }
    var isLoadingVideos by remember { mutableStateOf(false) }
    var videoErrorMessage by remember { mutableStateOf<String?>(null) }

    // HLS player page state
    var showHlsPlayerPage by remember { mutableStateOf(false) }
    var hlsPlaylistUrl by remember { mutableStateOf<String?>(null) }
    var selectedVideoTitle by remember { mutableStateOf("Video") }

    val authState by authenticationManager.authState.collectAsState()
    val scope = rememberCoroutineScope()

    // Create and start a single LocalVideoServer that runs continuously
    val videoServer = remember { LocalVideoServer() }

    //
    // Auto-start local server once on page load
    //
    LaunchedEffect(Unit) {
        try {
            videoServer.start()
            Logger.i("VideoPlayerTestPage") { "Video server started on page load" }
        } catch (e: Exception) {
            Logger.e("VideoPlayerTestPage", e) { "Failed to start video server on page load" }
        }
    }

    //
    // Handle authentication state changes
    //
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Authenticated -> {
                resultMessage = "Authentication successful!\nIdentity: ${state.identity}"
                isSuccess = true
                showResultDialog = false
            }
            is AuthState.Error -> {
                resultMessage = "Authentication failed:\n${state.message}"
                isSuccess = false
                showResultDialog = true
            }
            else -> {}
        }
    }

    //
    // Fetch video list from backend when authenticated
    //
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            isLoadingVideos = true
            videoErrorMessage = null
            try {
                val payloadPlayground = PayloadPlayground(authState as AuthState.Authenticated)
                videoHeaders = payloadPlayground.getVideosOnDrive(
                    PublicPostsChannelDrive.alias,
                    PublicPostsChannelDrive.type
                )
                isLoadingVideos = false
                Logger.i("VideoPlayerTestPage") { "Loaded ${videoHeaders?.size ?: 0} videos" }
            } catch (e: Exception) {
                videoErrorMessage = e.message ?: "Unknown error"
                Logger.e("VideoPlayerTestPage", e) { "Failed to fetch videos: $videoErrorMessage" }
                isLoadingVideos = false
            }
        } else {
            // Clear videos when logged out
            videoHeaders = null
            selectedVideoHeader = null
        }
    }

    //
    // Error dialog
    //
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Error") },
            text = { Text(errorMessage!!) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) { Text("OK") }
            }
        )
    }

    //
    // Result dialog
    //
    if (showResultDialog) {
        AlertDialog(
            onDismissRequest = { showResultDialog = false },
            title = {
                Text(if (isSuccess) "Success" else "Error")
            },
            text = {
                Text(resultMessage)
            },
            confirmButton = {
                TextButton(onClick = { showResultDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    //
    // Show HLS player page if a video is selected
    //
    if (showHlsPlayerPage && hlsPlaylistUrl != null) {
        val clientAuthToken = (authState as? AuthState.Authenticated)?.clientAuthToken
        Logger.i("VideoPlayerTestPage") { "Opening HLS player with clientAuthToken: $clientAuthToken" }
        HlsVideoPlayerPage(
            hlsPlaylistUrl = hlsPlaylistUrl!!,
            clientAuthToken = clientAuthToken,
            videoTitle = selectedVideoTitle,
            onBack = {
                showHlsPlayerPage = false
                hlsPlaylistUrl = null
                Logger.i("VideoPlayerTestPage") { "Returned from HLS player page" }
            }
        )
    } else {
        // Show video test page
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Video Player Test",
                style = MaterialTheme.typography.headlineMedium
            )

        // Authentication section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Authentication",
                    style = MaterialTheme.typography.titleMedium
                )

                when (val state = authState) {
                    is AuthState.Unauthenticated -> {
                        OutlinedTextField(
                            value = odinIdentity,
                            onValueChange = { odinIdentity = it },
                            label = { Text("Odin Identity") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    authenticationManager.authenticate(odinIdentity, password)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Log in")
                        }
                    }
                    is AuthState.Authenticating -> {
                        CircularProgressIndicator()
                        Text(
                            text = "Authenticating...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    is AuthState.Authenticated -> {
                        Text(
                            text = "Logged in as: ${state.identity}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    authenticationManager.logout()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Log out")
                        }
                    }
                    is AuthState.Error -> {
                        Text(
                            text = "Error: ${state.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        OutlinedTextField(
                            value = odinIdentity,
                            onValueChange = { odinIdentity = it },
                            label = { Text("Odin Identity") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    authenticationManager.authenticate(odinIdentity, password)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Try again")
                        }
                    }
                }
            }
        }

        // Video list card (only shown when authenticated)
        if (authState is AuthState.Authenticated) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Available Videos",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    when {
                        isLoadingVideos -> {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text(
                                text = "Loading videos...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                        videoErrorMessage != null -> {
                            Text(
                                text = "Error: $videoErrorMessage",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                        videoHeaders != null && videoHeaders!!.isNotEmpty() -> {
                            val headerCount = videoHeaders?.size ?: 0
                            Text(
                                text = "Found $headerCount video${if (headerCount != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            videoHeaders!!.forEachIndexed { index, header ->
                                Button(
                                    onClick = {
                                        Logger.i("VideoPlayerTestPage") { "Selected video ${index + 1}: ${header.header.fileId}" }
                                        scope.launch {
                                            try {
                                                // Get current auth token
                                                val currentAuthToken = (authState as? AuthState.Authenticated)?.clientAuthToken
                                                if (currentAuthToken == null) {
                                                    errorMessage = "Not authenticated"
                                                    return@launch
                                                }

                                                // Get the HLS manifest content from backend
                                                val originalManifest = header.getVideoMetaData()
                                                Logger.i("VideoPlayerTestPage") { "Got HLS manifest content for video ${index + 1}" }

                                                // Get the local server URL
                                                val serverUrl = videoServer.getServerUrl()
                                                Logger.d("VideoPlayerTestPage") { "Video server running at: $serverUrl" }

                                                // Register the manifest ID for auth token lookup
                                                val manifestId = "video-${index + 1}-manifest"

                                                // Modify the manifest to proxy remote segment URLs through local server
                                                val modifiedManifest = originalManifest.lines().joinToString("\n") { line ->
                                                    if (line.startsWith("https://")) {
                                                        val encodedUrl = line.encodeURLParameter()
                                                        "$serverUrl/proxy?url=$encodedUrl&manifestId=$manifestId"
                                                    } else {
                                                        line
                                                    }
                                                }
                                                Logger.d("VideoPlayerTestPage") { "Modified manifest to proxy segment URLs through $serverUrl" }

                                                // Register the modified manifest with local server and auth token
                                                videoServer.registerContent(
                                                    id = manifestId,
                                                    data = modifiedManifest.encodeToByteArray(),
                                                    contentType = "application/vnd.apple.mpegurl",
                                                    authTokenHeaderName = "DY0810",
                                                    authToken = currentAuthToken
                                                )

                                                // Get the local URL and pass to player
                                                hlsPlaylistUrl = videoServer.getContentUrl(manifestId)
                                                selectedVideoTitle = "Video ${index + 1}"
                                                showHlsPlayerPage = true

                                                Logger.i("VideoPlayerTestPage") { "Starting HLS playback at: $hlsPlaylistUrl" }
                                            } catch (e: Exception) {
                                                if (e is CancellationException) throw e
                                                errorMessage = e.message ?: "Unknown error"
                                                Logger.e("VideoPlayerTestPage", e) { "Failed to load video" }
                                            }
                                        }
                                    },
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
                                text = "No videos found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
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
