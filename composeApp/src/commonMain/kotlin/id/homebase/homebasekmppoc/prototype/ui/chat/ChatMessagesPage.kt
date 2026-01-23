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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
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
import id.homebase.homebasekmppoc.prototype.lib.chat.ChatMessageData
import id.homebase.homebasekmppoc.prototype.lib.chat.ChatMessageProvider
import id.homebase.homebasekmppoc.prototype.lib.chat.ChatMessageSenderService
import id.homebase.homebasekmppoc.prototype.lib.chat.ConversationData
import id.homebase.homebasekmppoc.prototype.lib.chat.ConversationProvider
import id.homebase.homebasekmppoc.prototype.lib.chat.SendChatMessageRequest
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
    onNavigateToMessageDetail: (String, String) -> Unit,
) {
    val authState by youAuthFlowManager.authState.collectAsState()
    val localQueryResults = remember {
        mutableStateListOf<ChatMessageData>()
    }

    val scope = CoroutineScope(Dispatchers.Main)
    val chatSenderService: ChatMessageSenderService? = koinInject()

    var composerText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var syncProgress by remember { mutableStateOf<BackendEvent?>(null) }
    var isOnline by remember { mutableStateOf(true) }
    val identityId = Uuid.parse("7b1be23b-48bb-4304-bc7b-db5910c09a92") // TODO: get real identityId

    val driveId = remember { chatTargetDrive.alias }
    val conversationUuid = remember { Uuid.parse(conversationId) }

    val driveQueryProvider: DriveQueryProvider? = koinInject()
    val odinClient: OdinClient? = koinInject()

    val driveSynchronizer =
        remember(driveQueryProvider) {
            driveQueryProvider?.let {
                DriveSync(identityId, driveId, it, DatabaseManager.appDb, appEventBus)
            }
        }

    // Create ChatMessageProvider with OdinClient for decryption
    val chatMessageProvider =
        remember(odinClient) { odinClient?.let { ChatMessageProvider(identityId, it) } }

    fun triggerFetch(withProgress: Boolean) {
        if (driveSynchronizer == null) {
            errorMessage = "Not authenticated"
            return
        }
        errorMessage = null

        if (driveSynchronizer.sync() != null) {
            // TODO: I'm not sure this is right, shouldn't the UI here react on the emitted event,
            isLoading = true
            if (withProgress) {
                syncProgress = null
            }
        }
    }

    suspend fun tiggerSend(text: String): Boolean {
        if (chatSenderService == null) {
            throw Exception("missing services")
        }

        val request = SendChatMessageRequest(
            conversationId = conversationUuid,
            messageText = text,
            recipients = listOf("sam.dotyou.cloud")
        )

        chatSenderService.sendMessage(request)
        return true
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
        localQueryResults.clear()
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
                        // Load messages using ChatMessageProvider
                        chatMessageProvider?.let { provider ->
                            val result =
                                provider.fetchMessages(
                                    dbm = DatabaseManager.appDb,
                                    driveId = driveId,
                                    conversationId = conversationUuid
                                )
                            localQueryResults.clear()
                            localQueryResults.addAll(result.records)
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

                is BackendEvent.ConnectionOnline -> {
                    isOnline = true
                }

                is BackendEvent.ConnectionOffline -> {
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

                        Spacer(modifier = Modifier.height(24.dp))

                        MessageComposer(
                            text = composerText,
                            enabled = !isLoading && isOnline,
                            onTextChange = { composerText = it },
                            onSendMessage = {
                                scope.launch {
                                    try {
                                        tiggerSend(composerText)
                                        composerText = ""
                                    } catch (e: Exception) {
                                        errorMessage = e.message
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (errorMessage != null) {
                            Text(
                                text = "Error: $errorMessage",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            // Content is already decrypted by ChatMessageProvider
                            key(localQueryResults.size) {
                                ChatMessageList(
                                    items = localQueryResults,
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
