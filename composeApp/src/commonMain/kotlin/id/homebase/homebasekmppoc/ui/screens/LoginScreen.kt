package id.homebase.homebasekmppoc.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.homebase.homebasekmppoc.prototype.lib.authentication.AuthState
import id.homebase.homebasekmppoc.prototype.lib.youauth.YouAuthManager
import id.homebase.homebasekmppoc.prototype.ui.app.getAppParams
import kotlinx.coroutines.launch

/**
 * Login screen for Homebase authentication. Asks for homebase.id and initiates YouAuth flow.
 *
 * @param youAuthManager Manager for YouAuth authentication flow
 * @param onLoginSuccess Callback when login succeeds
 */
@Composable
fun LoginScreen(youAuthManager: YouAuthManager, onLoginSuccess: () -> Unit) {
    var homebaseId by remember { mutableStateOf("frodo.baggins.demo.rocks") }
    val authState by youAuthManager.youAuthState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Navigate on successful authentication
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onLoginSuccess()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                    text = "Welcome to Homebase",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                    text = "Sign in with your Homebase ID",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            when (val state = authState) {
                is AuthState.Unauthenticated -> {
                    LoginForm(
                            homebaseId = homebaseId,
                            onHomebaseIdChange = { homebaseId = it },
                            onLoginClick = {
                                coroutineScope.launch {
                                    val appParams = getAppParams()
                                    youAuthManager.authorize(homebaseId, coroutineScope, appParams)
                                }
                            }
                    )
                }
                is AuthState.Authenticating -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                                text = "Authenticating...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is AuthState.Authenticated -> {
                    // Will navigate via LaunchedEffect
                    Text(
                            text = "Login successful!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                    )
                }
                is AuthState.Error -> {
                    ErrorState(
                            message = state.message,
                            homebaseId = homebaseId,
                            onHomebaseIdChange = { homebaseId = it },
                            onRetryClick = {
                                coroutineScope.launch {
                                    val appParams = getAppParams()
                                    youAuthManager.authorize(homebaseId, coroutineScope, appParams)
                                }
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginForm(
        homebaseId: String,
        onHomebaseIdChange: (String) -> Unit,
        onLoginClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
                value = homebaseId,
                onValueChange = onHomebaseIdChange,
                label = { Text("Homebase ID") },
                placeholder = { Text("your.identity.id") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onLoginClick, modifier = Modifier.fillMaxWidth()) { Text("Sign In") }
    }
}

@Composable
private fun ErrorState(
        message: String,
        homebaseId: String,
        onHomebaseIdChange: (String) -> Unit,
        onRetryClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
                text = "Error: $message",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
                value = homebaseId,
                onValueChange = onHomebaseIdChange,
                label = { Text("Homebase ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetryClick, modifier = Modifier.fillMaxWidth()) { Text("Try Again") }
    }
}
