package id.homebase.homebasekmppoc.prototype.ui.cdn

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.homebase.homebasekmppoc.prototype.lib.authentication.AuthState
import id.homebase.homebasekmppoc.prototype.lib.youauth.YouAuthManager
import id.homebase.homebasekmppoc.prototype.ui.app.getFeedAppParams
import kotlinx.coroutines.launch

/**
 * Test page for CDN functionality.
 */
@Composable
fun CdnTestPage(youAuthManager: YouAuthManager) {
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // var odinIdentity by remember { mutableStateOf("frodo.dotyou.cloud") }
    var odinIdentity by remember { mutableStateOf("frodo.baggins.demo.rocks") }

    var password by remember { mutableStateOf("a") }

    var showLogionResultDialog by remember { mutableStateOf(false) }
    var loginResultMessage by remember { mutableStateOf("") }
    var isLoginSuccess by remember { mutableStateOf(false) }

    val authState by youAuthManager.youAuthState.collectAsState()
    val coroutineScope = rememberCoroutineScope()


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
    // XXX
    //
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "CDN Test",
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
    }
}

