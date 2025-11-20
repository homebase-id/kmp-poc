package id.homebase.homebasekmppoc.pages.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.homebase.homebasekmppoc.authentication.AuthState
import id.homebase.homebasekmppoc.authentication.AuthenticationManager
import id.homebase.homebasekmppoc.youauth.YouAuthState
import id.homebase.homebasekmppoc.youauth.YouAuthManager
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun OwnerPage(authenticationManager: AuthenticationManager) {
    var odinIdentity by remember { mutableStateOf("frodo.dotyou.cloud") }
    var password by remember { mutableStateOf("a") }

    var showResultDialog by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    val authState by authenticationManager.authState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Show dialog when authentication completes
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Authenticated -> {
                resultMessage = "Authentication successful!\nIdentity: ${state.identity}"
                isSuccess = true
                showResultDialog = true
            }
            is AuthState.Error -> {
                resultMessage = "Authentication failed:\n${state.message}"
                isSuccess = false
                showResultDialog = true
            }
            else -> {}
        }
    }

    // Result dialog
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Owner",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Always show the authenticated owner card
        AuthenticatedOwnerCard(
            authenticatedState = if (authState is AuthState.Authenticated) authState as AuthState.Authenticated else null,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Show auth state information
        when (val state = authState) {
            is AuthState.Unauthenticated -> {
                OutlinedTextField(
                    value = odinIdentity,
                    onValueChange = { odinIdentity = it },
                    label = { Text("Odin Identity") },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            authenticationManager.authenticate(odinIdentity, password)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text("Log in")
                }
            }
            is AuthState.Authenticating -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Authenticating...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            is AuthState.Authenticated -> {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            authenticationManager.logout()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text("Log out")
                }
            }
            is AuthState.Error -> {
                Text(
                    text = "Error: ${state.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = odinIdentity,
                    onValueChange = { odinIdentity = it },
                    label = { Text("Odin Identity") },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            authenticationManager.authenticate(odinIdentity, password)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text("Try again")
                }
            }
        }
    }
}

@Preview
@Composable
fun OwnerPagePreview() {
    MaterialTheme {
        OwnerPage(AuthenticationManager())
    }
}