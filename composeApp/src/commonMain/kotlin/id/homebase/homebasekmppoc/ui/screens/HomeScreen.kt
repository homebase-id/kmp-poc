package id.homebase.homebasekmppoc.ui.screens

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
import androidx.navigation.NavHostController
import id.homebase.homebasekmppoc.ui.navigation.Route

/**
 * Home screen - main hub after authentication. Provides navigation to other features.
 *
 * @param navController Navigation controller for routing
 * @param onLogout Callback when user logs out
 */
@Composable
fun HomeScreen(navController: NavHostController, onLogout: () -> Unit) {
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

            NavigationButton("Drive Fetch") { navController.navigate(Route.DriveFetch) }

            NavigationButton("Database") { navController.navigate(Route.Database) }

            NavigationButton("WebSocket") { navController.navigate(Route.WebSocket) }

            NavigationButton("Video") { navController.navigate(Route.Video) }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Text("Logout")
            }
        }
    }
}

@Composable
private fun NavigationButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(text) }
}
