package id.homebase.homebasekmppoc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import id.homebase.homebasekmppoc.authentication.AuthenticationManager
import id.homebase.homebasekmppoc.pages.app.AppPage
import id.homebase.homebasekmppoc.pages.db.DbPage
import id.homebase.homebasekmppoc.pages.owner.OwnerPage
import id.homebase.homebasekmppoc.pages.domain.DomainPage
import id.homebase.homebasekmppoc.youauth.YouAuthManager
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        var selectedTabIndex by remember { mutableStateOf(0) }
        val tabs = listOf("Owner", "Domain", "App", "db")

        // Hoist YouAuthManager to App level so it survives tab navigation
        val authenticationManager = remember { AuthenticationManager() }
        val domainYouAuthManager = remember { YouAuthManager() }
        val appYouAuthManager = remember { YouAuthManager() }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedTabIndex) {
                    0 -> OwnerPage(authenticationManager)
                    1 -> DomainPage(domainYouAuthManager)
                    2 -> AppPage(appYouAuthManager)
                    3 -> DbPage()
                }
            }
        }
    }
}