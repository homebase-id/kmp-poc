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
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import id.homebase.homebasekmppoc.prototype.lib.database.QueryBatch
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchSortField
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchSortOrder
import id.homebase.homebasekmppoc.prototype.lib.drives.SharedSecretEncryptedFileHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.query.DriveQueryProvider
import id.homebase.homebasekmppoc.prototype.lib.eventbus.BackendEvent
import id.homebase.homebasekmppoc.prototype.lib.eventbus.appEventBus
import id.homebase.homebasekmppoc.prototype.ui.driveFetch.DriveSync
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject

/** Chat message file type constant */
const val CHAT_MESSAGE_FILE_TYPE = 7878

/**
 * Messages page - shows messages for a specific conversation. Filters by fileType 7878 and groupId
 * = conversationId.
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatMessagesPage(
        conversationId: String,
        youAuthFlowManager: YouAuthFlowManager,
        onNavigateBack: () -> Unit,
        onNavigateToMessageDetail: (String, String) -> Unit
) {
    val authState by youAuthFlowManager.authState.collectAsState()
    var localQueryResults by remember {
        mutableStateOf<List<SharedSecretEncryptedFileHeader>?>(null)
    }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var syncProgress by remember { mutableStateOf<BackendEvent?>(null) }
    var isOnline by remember { mutableStateOf(true) }
    val identityId = Uuid.parse("7b1be23b-48bb-4304-bc7b-db5910c09a92") // TODO: get real identityId

    val driveId = remember { chatTargetDrive.alias }
    val conversationUuid = remember { Uuid.parse(conversationId) }

    val driveQueryProvider: DriveQueryProvider? = koinInject()

    val driveSynchronizer =
            remember(driveQueryProvider) {
                driveQueryProvider?.let {
                    DriveSync(identityId, driveId, it, DatabaseManager.appDb, appEventBus)
                }
            }

    fun triggerFetch(withProgress: Boolean) {
        if (driveSynchronizer == null) {
            errorMessage = "Not authenticated"
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
                        // Load messages filtered by conversationId (groupId)
                        val localResult =
                                QueryBatch(identityId)
                                        .queryBatchAsync(
                                                DatabaseManager.appDb,
                                                driveId,
                                                1000,
                                                null,
                                                QueryBatchSortOrder.NewestFirst,
                                                QueryBatchSortField.CreatedDate,
                                                fileSystemType = 0,
                                                filetypesAnyOf = listOf(CHAT_MESSAGE_FILE_TYPE),
                                                groupIdAnyOf = listOf(conversationUuid)
                                        )
                        localQueryResults = localResult.records
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
                        title = { Text("Messages") },
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
                        // Show conversation ID
                        Text(
                                text = "Conversation: ${conversationId.take(16)}...",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = { triggerFetch(true) }, enabled = !isLoading) {
                            Text(if (isLoading) "Fetching..." else "Fetch Messages")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (errorMessage != null) {
                            Text(
                                    text = "Error: $errorMessage",
                                    color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            // Extract sharedSecret for decryption
                            val sharedSecret =
                                    (authState as? YouAuthState.Authenticated)?.sharedSecret

                            localQueryResults?.let { items ->
                                ChatMessageList(
                                        items = items,
                                        sharedSecret = sharedSecret,
                                        onMessageClicked = { itemDriveId, fileId ->
                                            onNavigateToMessageDetail(itemDriveId, fileId)
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
