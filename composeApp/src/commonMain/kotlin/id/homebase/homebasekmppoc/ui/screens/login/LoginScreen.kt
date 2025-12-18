package id.homebase.homebasekmppoc.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import id.homebase.homebasekmppoc.ui.assets.Homebase
import id.homebase.homebasekmppoc.ui.assets.HomebaseIcons

/**
 * Login screen - dumb composable. Takes state and action callback, contains no logic.
 *
 * @param state Current UI state
 * @param onAction Callback for user actions
 */
@Composable
fun LoginScreen(state: LoginUiState, onAction: (LoginUiAction) -> Unit) {
    // Detect when app resumes from background (e.g., after browser auth was cancelled)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onAction(LoginUiAction.AppResumed)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState())
                                .imePadding(), // Ensure content is not hidden by keyboard
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            // Homebase Icon
            Icon(
                    imageVector = HomebaseIcons.Homebase,
                    contentDescription = "Homebase Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

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

            when {
                state.isLoading -> {
                    LoadingContent()
                }
                state.isAuthenticated -> {
                    SuccessContent()
                }
                state.errorMessage != null -> {
                    ErrorContent(
                            message = state.errorMessage,
                            homebaseId = state.homebaseId,
                            onHomebaseIdChange = { onAction(LoginUiAction.HomebaseIdChanged(it)) },
                            onRetryClick = { onAction(LoginUiAction.RetryClicked) }
                    )
                }
                else -> {
                    LoginFormContent(
                            homebaseId = state.homebaseId,
                            onHomebaseIdChange = { onAction(LoginUiAction.HomebaseIdChanged(it)) },
                            onLoginClick = { onAction(LoginUiAction.LoginClicked) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
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

@Composable
private fun SuccessContent() {
    Text(
            text = "Login successful!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun LoginFormContent(
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
private fun ErrorContent(
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
