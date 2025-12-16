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
import id.homebase.homebasekmppoc.lib.youAuth.YouAuthFlowManager
import id.homebase.homebasekmppoc.lib.youAuth.YouAuthState
import id.homebase.homebasekmppoc.prototype.lib.drives.FileQueryParams
import id.homebase.homebasekmppoc.prototype.lib.drives.FileState
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchResponse
import id.homebase.homebasekmppoc.prototype.lib.drives.QueryBatchResultOptionsRequest
import id.homebase.homebasekmppoc.prototype.lib.drives.query.DriveQueryProvider
import id.homebase.homebasekmppoc.prototype.ui.app.feedTargetDrive
import kotlin.time.measureTimedValue
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DriveFetchPage(youAuthFlowManager: YouAuthFlowManager, onNavigateBack: () -> Unit) {
    val authState by youAuthFlowManager.authState.collectAsState()
    var queryBatchResponse by remember { mutableStateOf<QueryBatchResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var fetchedCount by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // Inject DriveQueryProvider from Koin
    val driveQueryProvider: DriveQueryProvider? = koinInject()

    fun refresh(onProgressUX: (fetchedCount: Int) -> Unit = { _ -> }) {
        val provider = driveQueryProvider
        if (provider == null) {
            errorMessage = "Not authenticated - no credentials stored"
            return
        }

        isLoading = true
        errorMessage = null
        fetchedCount = 0

        var totalCount = 0
        var batchSize = 100
        var cursor: String? = null

        coroutineScope.launch {
            try {
                var keepGoing = true

                while (keepGoing) {
                    val request =
                            QueryBatchRequest(
                                    queryParams =
                                            FileQueryParams(
                                                    targetDrive = feedTargetDrive,
                                                    fileState = listOf(FileState.Active)
                                            ),
                                    resultOptionsRequest =
                                            QueryBatchResultOptionsRequest(
                                                    maxRecords = batchSize,
                                                    includeMetadataHeader = true,
                                                    cursorState = cursor
                                            )
                            )

                    val durationMs = measureTimedValue {
                        queryBatchResponse = provider.queryBatch(request)

                        if (queryBatchResponse?.cursorState != null)
                                cursor = queryBatchResponse?.cursorState

                        if (queryBatchResponse?.searchResults?.isNotEmpty() == true) {
                            totalCount += queryBatchResponse!!.searchResults.size

                            // UX callback
                            onProgressUX(totalCount)

                            // Create FileMetadataProcessor instance to test BaseUpsertEntryZapZap
                            // Call BaseUpsertEntryZapZap function mainIndexMeta.kt line 127

                            //                            processor.BaseUpsertEntryZapZap(
                            //                                identityId = identityId,
                            //                                driveId = driveId,
                            //                                fileHeader =
                            // queryBatchResponse.searchResults,
                            //                                cursor = cursor
                            //                            )
                            //
                        }

                        keepGoing =
                                queryBatchResponse?.searchResults?.let { it.size >= batchSize }
                                        ?: false
                    }

                    // Adaptive package size
                    if ((durationMs.duration.inWholeMilliseconds < 300) && (batchSize < 1000)) {
                        batchSize = (batchSize * 1.5).toInt().coerceAtMost(1000)
                    } else if ((durationMs.duration.inWholeMilliseconds > 800) && (batchSize > 50)
                    ) {
                        batchSize = (batchSize * 0.7).toInt().coerceAtLeast(50)
                    }

                    // Optional: log for debugging
                    Logger.d(
                            "Batch size: $batchSize, took ${durationMs.duration.inWholeMilliseconds}ms"
                    )
                }

                // Final result is already stored in queryBatchResponse from last successful call

            } catch (e: Exception) {
                Logger.e("Error fetching Drive Fetch data", e)
                errorMessage = e.message ?: "Unknown error"
            } finally {
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
                            refresh()
                        }
                    }
            )

    // Reset state when auth changes
    LaunchedEffect(authState) {
        queryBatchResponse = null
        errorMessage = null
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
                                onClick = { refresh { count -> fetchedCount = count } },
                                enabled = !isLoading
                        ) { Text(if (isLoading) "Fetching..." else "Fetch Files") }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isLoading) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                        text = "$fetchedCount items fetched",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (errorMessage != null) {
                            Text(
                                    text = "Error: $errorMessage",
                                    color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            queryBatchResponse?.let { response ->
                                DriveFetchList(items = response.searchResults)
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
