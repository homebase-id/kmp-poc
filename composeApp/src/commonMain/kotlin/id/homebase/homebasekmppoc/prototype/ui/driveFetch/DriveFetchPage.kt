package id.homebase.homebasekmppoc.prototype.ui.driveFetch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import id.homebase.homebasekmppoc.lib.youauth.YouAuthFlowManager
import id.homebase.homebasekmppoc.lib.youauth.YouAuthState
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import id.homebase.homebasekmppoc.prototype.lib.database.QueryBatch
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchSortField
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchSortOrder
import id.homebase.homebasekmppoc.prototype.lib.drives.SharedSecretEncryptedFileHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.query.DriveQueryProvider
import id.homebase.homebasekmppoc.ui.screens.login.feedTargetDrive
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.uuid.Uuid


@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DriveFetchPage(youAuthFlowManager: YouAuthFlowManager, onNavigateBack: () -> Unit) {
    val authState by youAuthFlowManager.authState.collectAsState()
    var localQueryResults by remember { mutableStateOf<List<SharedSecretEncryptedFileHeader>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var syncProgress by remember { mutableStateOf<BackendEvent?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val database = DatabaseManager.getDatabase()
    val identityId = Uuid.parse("7b1be23b-48bb-4304-bc7b-db5910c09a92") // TODO: <- get the real identityId

    // Inject DriveQueryProvider from Koin
    val driveQueryProvider: DriveQueryProvider? = koinInject()

    fun triggerFetch(withProgress: Boolean) {
        val provider = driveQueryProvider
        if (provider == null) {
            errorMessage = "Not authenticated - no credentials stored"
            return
        }
        isLoading = true
        errorMessage = null
        if (withProgress) syncProgress = null
        coroutineScope.launch {
            try {
                // TODO: Where does the identityId live? Need to get it instead of random.
                val backend = DriveSync(identityId, feedTargetDrive, driveQueryProvider)
                
                // Collect Flow updates and update UI state
                backend.sync().collect { progress ->
                    syncProgress = progress
                    when (progress) {
                        is BackendEvent.SyncUpdate.BatchReceived -> {
                            // You can optionally use progress.batchData directly, e.g., add to a in-memory list or update UI
                        }
                        is BackendEvent.SyncUpdate.Completed -> {
                            // Sync completed, we can fetch local results
                            // We don't really need to - this is just a demo and should return up to 1000 rows from the local DB
                            // which probably matches what was just synced.
                            val localResult = QueryBatch(DatabaseManager, identityId).queryBatchAsync(
                                feedTargetDrive.alias,
                                1000,
                                null,
                                QueryBatchSortOrder.OldestFirst,
                                QueryBatchSortField.AnyChangeDate,
                                fileSystemType = 0);
                            localQueryResults = localResult.records
                            isLoading = false
                            isRefreshing = false
                        }
                        is BackendEvent.SyncUpdate.Failed -> {
                            errorMessage = progress.errorMessage
                            isLoading = false
                            isRefreshing = false
                        }
                        is BackendEvent.GoingOnline -> {
                        }
                        is BackendEvent.GoingOffline -> {
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.e("Error fetching Drive Fetch data", e)
                errorMessage = e.message ?: "Unknown error"
                isLoading = false
                isRefreshing = false
            }
        }
    }


    val pullRefreshState =
        rememberPullRefreshState(
            isRefreshing,
            {
                if (authState is YouAuthState.Authenticated) {
                    isRefreshing = true
                    triggerFetch(false)
                }
            }
        )

    // Reset state when auth changes
    LaunchedEffect(authState) {
        localQueryResults = null
        errorMessage = null
        syncProgress = null
        isLoading = false
        isRefreshing = false
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Drive Fetch") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Text("←", style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                )
            }
    ) { paddingValues ->
        Box(
                modifier =
                        Modifier.fillMaxSize().padding(paddingValues).pullRefresh(pullRefreshState)
        ) {
            Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (authState) {
                    is YouAuthState.Authenticated -> {
                        Button(
                                onClick = { triggerFetch(true) },
                                enabled = !isLoading
                        ) { Text(if (isLoading) "Fetching..." else "Fetch Files") }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isLoading) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                when (val progress = syncProgress) {
                                    is BackendEvent.SyncUpdate.BatchReceived -> {
                                        Text(
                                                text = "Fetched ${progress.totalCount} items (${progress.batchCount} in this batch)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        progress.latestModified?.let { modified ->
                                            Text(
                                                    text = "Latest modified: ${modified}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    else -> {
                                        Text(
                                                text = "Starting sync...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else if (errorMessage != null) {
                            Text(
                                    text = "Error: $errorMessage",
                                    color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            localQueryResults?.let { items ->
                                DriveFetchList(items = items)
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
            PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
