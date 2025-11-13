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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.homebase.homebasekmppoc.youauth.buildAuthorizeUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun DomainPage() {
    // var odinIdentity by remember { mutableStateOf("frodo.baggins.demo.rocks") }
    var odinIdentity by remember { mutableStateOf("frodo.dotyou.cloud") }
    var isAuthenticating by remember { mutableStateOf(false) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // Reset authentication state after 30 seconds (in case auth gets stuck)
    LaunchedEffect(isAuthenticating) {
        if (isAuthenticating) {
            delay(30000) // 30 seconds timeout
            isAuthenticating = false
        }
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
            text = "Domain",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = odinIdentity,
            onValueChange = { odinIdentity = it },
            label = { Text("Odin Identity") },
            modifier = Modifier.padding(horizontal = 16.dp),
            enabled = !isAuthenticating
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isAuthenticating) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Authenticating...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            Button(
                onClick = {
                    isAuthenticating = true
                    coroutineScope.launch {
                        // val url = "https://$odinIdentity/api/v1/kmp/auth"
                        // val authorizeUrl = "https://$odinIdentity/api/owner/v1/youauth/authorize"
                        val authorizeUrl = buildAuthorizeUrl(odinIdentity)

                        // showMessage("uri", authorizeUrl)
                        launchCustomTabs(authorizeUrl)
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text("Log in")
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