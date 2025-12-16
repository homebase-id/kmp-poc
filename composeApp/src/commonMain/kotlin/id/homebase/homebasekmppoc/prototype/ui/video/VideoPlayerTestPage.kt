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
import id.homebase.homebasekmppoc.prototype.lib.http.AppOrOwner
import id.homebase.homebasekmppoc.prototype.lib.http.PayloadPlayground
import id.homebase.homebasekmppoc.prototype.lib.http.PayloadWrapper
import id.homebase.homebasekmppoc.prototype.lib.http.PublicPostsChannelDrive
import id.homebase.homebasekmppoc.prototype.lib.video.LocalVideoServer
import id.homebase.homebasekmppoc.prototype.lib.youauth.YouAuthManager
import id.homebase.homebasekmppoc.prototype.ui.app.getFeedAppParams
import kotlinx.coroutines.launch

/**
 * Test page for video playback with local HTTP server
 * Supports both HLS and regular video playback
 */
@Composable
fun VideoPlayerTestPage(youAuthManager: YouAuthManager) {
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // var odinIdentity by remember { mutableStateOf("frodo.dotyou.cloud") }
    var odinIdentity by remember { mutableStateOf("frodo.baggins.demo.rocks") }

    var password by remember { mutableStateOf("a") }

    var showLogionResultDialog by remember { mutableStateOf(false) }
    var loginResultMessage by remember { mutableStateOf("") }
    var isLoginSuccess by remember { mutableStateOf(false) }

    var videoPayloads by remember { mutableStateOf<List<PayloadWrapper>?>(null) }

    var selectedVideoPayload by remember { mutableStateOf<PayloadWrapper?>(null) }

    var isLoadingVideos by remember { mutableStateOf(false) }
    var videoErrorMessage by remember { mutableStateOf<String?>(null) }

    val authState by youAuthManager.youAuthState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Create and start a single LocalVideoServer that runs continuously
    // SEB:TODO how do we stop
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
                loginResultMessage = "Authentication successful!\nIdentity: ${state.identity}"
                isLoginSuccess = true
                showLogionResultDialog = false
            }

            is AuthState.Error -> {
                loginResultMessage = "Authentication failed:\n${state.message}"
                isLoginSuccess = false
                showLogionResultDialog = true
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
                videoPayloads = payloadPlayground.getVideosOnDrive(
                    AppOrOwner.Apps,
                    PublicPostsChannelDrive.alias,
                    PublicPostsChannelDrive.type
                )
                isLoadingVideos = false
                Logger.i("VideoPlayerTestPage") { "Loaded ${videoPayloads?.size ?: 0} videos" }
            } catch (e: Exception) {
                videoErrorMessage = e.message ?: "Unknown error"
                Logger.e("VideoPlayerTestPage", e) { "Failed to fetch videos: $videoErrorMessage" }
                isLoadingVideos = false
            }
        } else {
            // Clear videos when logged out
            videoPayloads = null
            selectedVideoPayload = null
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
    if (showLogionResultDialog) {
        AlertDialog(
            onDismissRequest = { showLogionResultDialog = false },
            title = {
                Text(if (isLoginSuccess) "Success" else "Error")
            },
            text = {
                Text(loginResultMessage)
            },
            confirmButton = {
                TextButton(onClick = { showLogionResultDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    //
    // Show HLS player page if a video is selected
    //
    if (selectedVideoPayload != null) {
        val clientAuthToken = (authState as? AuthState.Authenticated)?.clientAuthToken
        Logger.i("VideoPlayerTestPage") { "Opening player page with clientAuthToken: $clientAuthToken" }
        VideoPlayerPage(
            AppOrOwner.Apps,
            localVideoServer = videoServer,
            videoPayload = selectedVideoPayload!!,
            onBack = {
                selectedVideoPayload = null
                Logger.i("VideoPlayerTestPage") { "Returned from player page" }
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
                                    coroutineScope.launch {
                                        val appParams = getFeedAppParams()
                                        youAuthManager.authorize(
                                            odinIdentity,
                                            coroutineScope,
                                            appParams
                                        )
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
                                    coroutineScope.launch {
                                        youAuthManager.logout()
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
                                    coroutineScope.launch {
                                        val appParams = getFeedAppParams()
                                        youAuthManager.authorize(
                                            odinIdentity,
                                            coroutineScope,
                                            appParams
                                        )
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

            //
            // Video list card (only shown when authenticated)
            //
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

                            videoPayloads != null && videoPayloads!!.isNotEmpty() -> {
                                val headerCount = videoPayloads?.size ?: 0
                                Text(
                                    text = "Found $headerCount video${if (headerCount != 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                videoPayloads!!.forEachIndexed { index, header ->
                                    Button(
                                        onClick = {
                                            selectedVideoPayload = header
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Video ${index + 1}")
                                    }
                                    if (index < videoPayloads!!.size - 1) {
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
