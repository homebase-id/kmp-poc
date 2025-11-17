package id.homebase.homebasekmppoc.pages.app

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.homebase.homebasekmppoc.serialization.OdinSystemSerializer
import id.homebase.homebasekmppoc.youauth.YouAuthAppParameters
import id.homebase.homebasekmppoc.youauth.YouAuthDriveParameters
import id.homebase.homebasekmppoc.youauth.YouAuthManager
import id.homebase.homebasekmppoc.youauth.YouAuthState
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AppPage(youAuthManager: YouAuthManager) {
    var odinIdentity by remember { mutableStateOf("frodo.dotyou.cloud") }

    val authState by youAuthManager.youAuthState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "App",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Always show the authenticated app card
        AuthenticatedAppCard(
            authenticatedState = if (authState is YouAuthState.Authenticated) authState as YouAuthState.Authenticated else null,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                            val appParams = getAppParams()
                            youAuthManager.authorize(odinIdentity, coroutineScope, appParams)
                        }
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
                Button(
                    onClick = {
                        youAuthManager.logout()
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
                            youAuthManager.authorize(odinIdentity, coroutineScope)
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
fun AppPagePreview() {
    MaterialTheme {
        AppPage(YouAuthManager())
    }
}

//

private fun getAppParams(): YouAuthAppParameters {
    val driveParams = listOf(
        YouAuthDriveParameters(
            driveAlias = "11111111111111111111111111111111",
            driveType = "22222222222222222222222222222222",
            name = "Third Part Library",
            description = "Place for your third parties",
            permission = 3
        )
    )

    val appParams = YouAuthAppParameters(
        appName = "third party app",
        appOrigin = "dev.dotyou.cloud:3005",
        appId = "aaaaaaaa-bbbb-cccc-dddd-cccccccccccc",
        clientFriendly = "KMP App",
        drivesParam = OdinSystemSerializer.serialize(driveParams),
        returnParam = "backend-will-decide"
    )

    return appParams
}
