package id.homebase.homebasekmppoc.prototype.ui.driveFetch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.lib.drives.QueryBatchResponse
import id.homebase.homebasekmppoc.prototype.lib.authentication.AuthState
import id.homebase.homebasekmppoc.prototype.lib.drives.DriveQueryProvider
import id.homebase.homebasekmppoc.prototype.lib.youauth.YouAuthManager
import id.homebase.homebasekmppoc.prototype.ui.app.exampleDriveAlias
import id.homebase.homebasekmppoc.prototype.ui.app.exampleDriveType
import kotlinx.coroutines.launch

@Composable
fun DriveFetchPage(youAuthManager: YouAuthManager) {
    val authState by youAuthManager.youAuthState.collectAsState()
    var queryBatchResponse by remember { mutableStateOf<QueryBatchResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Reset state when auth changes
    LaunchedEffect(authState) {
        queryBatchResponse = null
        errorMessage = null
        isLoading = false
    }

    Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Drive Fetch", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        when (val state = authState) {
            is AuthState.Authenticated -> {
                Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                try {
                                    val dqr = DriveQueryProvider.create()
                                    queryBatchResponse =
                                            dqr.queryBatch(
                                                    state.identity,
                                                    state.clientAuthToken,
                                                    state.sharedSecret,
                                                    exampleDriveAlias,
                                                    exampleDriveType
                                            )
                                } catch (e: Exception) {
                                    Logger.e("Error fetching Drive Fetch data", e)
                                    errorMessage = e.message ?: "Unknown error"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading
                ) { Text(if (isLoading) "Fetching..." else "Fetch Files") }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    CircularProgressIndicator()
                } else if (errorMessage != null) {
                    Text(text = "Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                } else {
                    queryBatchResponse?.let { response ->
                        DriveFetchList(items = response.searchResults)
                    }
                }
            }
            else -> {
                Text(
                        text = "Please authenticate in the App tab first.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                )
            }
        }
    }
}
