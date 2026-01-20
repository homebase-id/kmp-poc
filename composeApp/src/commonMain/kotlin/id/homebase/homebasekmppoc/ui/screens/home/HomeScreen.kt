package id.homebase.homebasekmppoc.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onAction: (HomeUiAction) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Homebase POC",
                            style = MaterialTheme.typography.titleLarge
                        )

                        state.identity?.let { identity ->
                            Text(
                                text = identity,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    ConnectionStatusIndicator(
                        status = state.connectionStatus
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Welcome Home",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            NavigationButton("Drive Fetch") {
                onAction(HomeUiAction.DriveFetchClicked)
            }

            NavigationButton("Database") {
                onAction(HomeUiAction.DatabaseClicked)
            }

            NavigationButton("WebSocket") {
                onAction(HomeUiAction.WebSocketClicked)
            }

            NavigationButton("Video") {
                onAction(HomeUiAction.VideoClicked)
            }

            NavigationButton("CdnTest") {
                onAction(HomeUiAction.CdnTestClicked)
            }

            NavigationButton("Drive Upload") {
                onAction(HomeUiAction.DriveUploadClicked)
            }

            NavigationButton("FFmpeg Test") {
                onAction(HomeUiAction.FFmpegTestClicked)
            }

            NavigationButton("Chat Messages") {
                onAction(HomeUiAction.ChatListClicked)
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = { onAction(HomeUiAction.LogoutClicked) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }
        }
    }

    if (state.showPermissionDialog) {
        MissingPermissionDialog(
            appName = state.appName,
            onExtend = { onAction(HomeUiAction.ExtendPermissionsClicked) },
            onDismiss = { onAction(HomeUiAction.DismissPermissionDialog) }
        )
    }
}

@Composable
private fun ConnectionStatusIndicator(
    status: ConnectionStatus
) {
    when (status) {
        ConnectionStatus.Connecting -> {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .height(15.dp),
                strokeWidth = 2.dp
            )
        }

        ConnectionStatus.Online -> {
            Text(
                text =  "🟢",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 12.dp)
            )
        }

        ConnectionStatus.Offline -> {
            Text(
                text = "🔴",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
    }
}


@Composable
private fun NavigationButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text)
    }
}

@Composable
fun MissingPermissionDialog(
    appName: String,
    onExtend: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Missing Permissions") },
        text = {
            Text(
                "The $appName app is missing permissions. Without the necessary permissions, " +
                        "the functionality of $appName will be limited."
            )
        },
        confirmButton = {
            Button(onClick = onExtend) {
                Text("Extend Permissions")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
