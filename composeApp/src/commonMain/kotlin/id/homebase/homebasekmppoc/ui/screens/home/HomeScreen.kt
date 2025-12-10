package id.homebase.homebasekmppoc.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Home screen - dumb composable. Takes state and action callback, contains no logic.
 *
 * @param state Current UI state
 * @param onAction Callback for user actions
 */
@Composable
fun HomeScreen(state: HomeUiState, onAction: (HomeUiAction) -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                    text = "Welcome Home",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            NavigationButton("Drive Fetch") { onAction(HomeUiAction.DriveFetchClicked) }

            NavigationButton("Database") { onAction(HomeUiAction.DatabaseClicked) }

            NavigationButton("WebSocket") { onAction(HomeUiAction.WebSocketClicked) }

            NavigationButton("Video") { onAction(HomeUiAction.VideoClicked) }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                    onClick = { onAction(HomeUiAction.LogoutClicked) },
                    modifier = Modifier.fillMaxWidth()
            ) { Text("Logout") }
        }
    }
}

@Composable
private fun NavigationButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(text) }
}
