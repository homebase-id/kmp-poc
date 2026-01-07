package id.homebase.homebasekmppoc.prototype.ui.cdn

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.homebase.homebasekmppoc.lib.youauth.YouAuthFlowManager
import id.homebase.homebasekmppoc.lib.youauth.YouAuthState
import id.homebase.homebasekmppoc.prototype.lib.ApiServiceExample.ApiService
import id.homebase.homebasekmppoc.prototype.showMessage
import kotlinx.coroutines.launch

/** Test page for CDN functionality. Uses shared YouAuthFlowManager for authentication state. */
@Composable
fun CdnTestPage(
    youAuthFlowManager: YouAuthFlowManager,
    apiService: ApiService) {
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val authState by youAuthFlowManager.authState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    //
    // Handle authentication state changes
    //
    LaunchedEffect(authState) {
        when (val state = authState) {
            is YouAuthState.Authenticated -> {
                val result = apiService.echoSharedSecretEncryptedParam()
                showMessage("echoSharedSecretEncryptedParam", result)
            }
            else -> {}
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
                confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("OK") } }
        )
    }

    Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "API-SERVICE-EXAMPLE", style = MaterialTheme.typography.headlineMedium)

        // Authentication section
        Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
        ) {
            Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Authentication", style = MaterialTheme.typography.titleMedium)

                when (val state = authState) {
                    is YouAuthState.Unauthenticated -> {
                        Text(
                                text = "Not authenticated. Please log in from the Home screen.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                        )
                    }
                    is YouAuthState.Authenticating -> {
                        CircularProgressIndicator()
                        Text(
                                text = "Authenticating...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    is YouAuthState.Authenticated -> {
                        Text(
                                text = "Logged in as: ${state.identity}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                        )
                        Button(
                                onClick = { coroutineScope.launch { youAuthFlowManager.logout() } },
                                modifier = Modifier.fillMaxWidth()
                        ) { Text("Log out") }
                    }
                    is YouAuthState.Error -> {
                        Text(
                                text = "Error: ${state.message}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
