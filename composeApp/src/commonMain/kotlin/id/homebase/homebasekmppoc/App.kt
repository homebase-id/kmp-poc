package id.homebase.homebasekmppoc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import id.homebase.homebasekmppoc.prototype.ui.app.AppPage
import id.homebase.homebasekmppoc.prototype.ui.db.DbPage
import id.homebase.homebasekmppoc.prototype.ui.owner.OwnerPage
import id.homebase.homebasekmppoc.prototype.ui.domain.DomainPage
import id.homebase.homebasekmppoc.prototype.ui.ws.WebsocketPage
import id.homebase.homebasekmppoc.prototype.ui.video.VideoPlayerTestPage
import id.homebase.homebasekmppoc.prototype.lib.authentication.AuthenticationManager
import id.homebase.homebasekmppoc.prototype.lib.youauth.YouAuthManager
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    MaterialTheme {
        var selectedTabIndex by remember { mutableStateOf(5) }
        val tabs = listOf("Owner", "Domain", "App", "db", "ws", "video")
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        // Hoist YouAuthManager to App level so it survives tab navigation
        val authenticationManager = remember { AuthenticationManager() }
        val wsAuthenticationManager = remember { AuthenticationManager() }
        val domainYouAuthManager = remember { YouAuthManager() }
        val appYouAuthManager = remember { YouAuthManager() }
        val videoYouAuthManager = remember { YouAuthManager() }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Text(
                        text = "Navigation",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                    tabs.forEachIndexed { index, title ->
                        NavigationDrawerItem(
                            label = { Text(title) },
                            selected = selectedTabIndex == index,
                            onClick = {
                                selectedTabIndex = index
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .fillMaxSize()
            ) {
                TopAppBar(
                    title = { Text(tabs[selectedTabIndex]) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Text("☰", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    when (selectedTabIndex) {
                        0 -> OwnerPage(authenticationManager)
                        1 -> DomainPage(domainYouAuthManager)
                        2 -> AppPage(appYouAuthManager)
                        3 -> DbPage()
                        4 -> WebsocketPage(wsAuthenticationManager)
                        5 -> VideoPlayerTestPage(videoYouAuthManager)
                    }
                }
            }
        }
    }
}