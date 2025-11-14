package id.homebase.homebasekmppoc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.homebase.homebasekmppoc.youauth.YouAuthState
import id.homebase.homebasekmppoc.youauth.YouAuthManager
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun DomainPage() {
    var odinIdentity by remember { mutableStateOf("frodo.dotyou.cloud") }
    val authState by YouAuthManager.youAuthState.collectAsState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Domain",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Show auth state information
        when (val state = authState) {
            is YouAuthState.Unauthenticated -> {
                OutlinedTextField(
                    value = odinIdentity,
                    onValueChange = { odinIdentity = it },
                    label = { Text("Odin Identity") },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            YouAuthManager.authorize(odinIdentity, coroutineScope)                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text("Log in")
                }
            }
            is YouAuthState.Authenticating -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Authenticating...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            is YouAuthState.Authenticated -> {
                Text(
                    text = "Logged in as: ${state.identity}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        YouAuthManager.logout()
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text("Log out")
                }
            }
            is YouAuthState.Error -> {
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
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            YouAuthManager.authorize(odinIdentity, coroutineScope)
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
fun DomainPagePreview() {
    MaterialTheme {
        DomainPage()
    }
}