package id.homebase.homebasekmppoc.prototype.ui.chat

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.homebase.homebasekmppoc.lib.config.chatTargetDrive
import id.homebase.homebasekmppoc.lib.youauth.YouAuthFlowManager
import id.homebase.homebasekmppoc.lib.youauth.YouAuthState
import id.homebase.homebasekmppoc.prototype.lib.chat.ConversationData
import id.homebase.homebasekmppoc.prototype.lib.chat.ConversationProvider
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import id.homebase.homebasekmppoc.prototype.lib.drives.query.DriveQueryProvider
import id.homebase.homebasekmppoc.prototype.lib.eventbus.BackendEvent
import id.homebase.homebasekmppoc.prototype.lib.eventbus.appEventBus
import id.homebase.homebasekmppoc.prototype.lib.http.OdinClient
import id.homebase.homebasekmppoc.prototype.ui.driveFetch.DriveSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatListPage(
        youAuthFlowManager: YouAuthFlowManager,
        onNavigateBack: () -> Unit,
        onNavigateToMessages: (conversationId: String) -> Unit,
        viewModel: ChatListViewModel = koinViewModel()
) {
    val scope = CoroutineScope(Dispatchers.Main)

    val authState by youAuthFlowManager.authState.collectAsState()
    var localQueryResults by remember { mutableStateOf<List<ConversationData>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var syncProgress by remember { mutableStateOf<BackendEvent?>(null) }
    var isOnline by remember { mutableStateOf(true) }
    val identityId =
            Uuid.parse("7b1be23b-48bb-4304-bc7b-db5910c09a92") // TODO: <- get the real identityId

    // Use chatTargetDrive to get the drive ID
    val driveId = remember {
        // We need to query the database to get the driveId for the chatTargetDrive
        // For now, we'll construct a synthetic driveId from alias+type
        // This matches how other drives work in the app
        chatTargetDrive.alias
    }

    // Inject DriveQueryProvider and OdinClient from Koin
    val driveQueryProvider: DriveQueryProvider? = koinInject()
    val odinClient: OdinClient? = koinInject()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ChatListUiEvent.NavigateToMessages -> onNavigateToMessages(event.conversationId)
                ChatListUiEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    // Create driveSynchronizer once
    val driveSynchronizer =
            remember(driveQueryProvider) {
                driveQueryProvider?.let {
                    DriveSync(identityId, driveId, it, DatabaseManager.appDb, appEventBus)
                }
            }

    // Create ConversationProvider with OdinClient for decryption
    val conversationProvider =
            remember(odinClient) { odinClient?.let { ConversationProvider(identityId, it) } }

    fun triggerClear() {
        if (driveSynchronizer == null) {
            errorMessage = "Not authenticated - no credentials stored"
            return
        }

        scope.launch {
            driveSynchronizer.clearStorage()
        }
    }

    fun triggerFetch(withProgress: Boolean) {
        if (driveSynchronizer == null) {
            errorMessage = "Not authenticated - no credentials stored"
            return
        }
        errorMessage = null

        if (driveSynchronizer.sync()) {
            isLoading = true
            if (withProgress) {
                syncProgress = null
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

    // Collect events from the bus, filter by driveId
    LaunchedEffect(Unit) {
        appEventBus.events.collectLatest { event ->
            when (event) {
                is BackendEvent.DriveEvent.BatchReceived -> {
                    if (event.driveId == driveId) {
                        syncProgress = event
                    }
                }
                is BackendEvent.DriveEvent.Completed -> {
                    if (event.driveId == driveId) {
                        syncProgress = event
                        // Fetch local results using ConversationProvider
                        conversationProvider?.let { provider ->
                            val result =
                                    provider.fetchConversations(
                                            dbm = DatabaseManager.appDb,
                                            driveId = driveId
                                    )
                            localQueryResults = result.records
                        }
                        isLoading = false
                        isRefreshing = false
                    }
                }
                is BackendEvent.DriveEvent.Failed -> {
                    if (event.driveId == driveId) {
                        errorMessage = event.errorMessage
                        isLoading = false
                        isRefreshing = false
                    }
                }
                is BackendEvent.DriveEvent.Started -> {
                    if (event.driveId == driveId) {
                        isLoading = true
                        syncProgress = null
                    }
                }
                is BackendEvent.GoingOnline -> {
                    isOnline = true
                }
                is BackendEvent.GoingOffline -> {
                    isOnline = false
                }
                else -> {
                    // Some other event
                }
            }
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Conversations") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Text("←", style = MaterialTheme.typography.headlineMedium)
                            }
                        },
                        actions = {
                            // Spinner when loading
                            if (isLoading) {
                                CircularProgressIndicator(
                                        modifier = Modifier.padding(end = 8.dp),
                                        strokeWidth = 2.dp
                                )
                            }
                            // Numerical progress
                            if (syncProgress is BackendEvent.DriveEvent.BatchReceived) {
                                val progress = syncProgress as BackendEvent.DriveEvent.BatchReceived
                                Text(
                                        text = "${progress.totalCount}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            // Online/offline indicator
                            Text(
                                    text = if (isOnline) "🟢" else "🔴",
                                    style = MaterialTheme.typography.headlineSmall
                            )
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
                            onClick = { triggerClear() },
                            enabled = !isLoading
                        ) {
                            Text("Clear sync storage")
                        }


                        Button(onClick = { triggerFetch(true) }, enabled = !isLoading) {
                            Text(if (isLoading) "Fetching..." else "Fetch Conversations")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (errorMessage != null) {
                            Text(
                                    text = "Error: $errorMessage",
                                    color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            // Content is already decrypted by ConversationProvider
                            localQueryResults?.let { items ->
                                ConversationList(
                                        items = items,
                                        onConversationClicked = { uniqueId ->
                                            viewModel.onAction(
                                                    ChatListUiAction.ConversationClicked(uniqueId)
                                            )
                                        }
                                )
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
