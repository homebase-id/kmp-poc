package id.homebase.homebasekmppoc.prototype.ui.db

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.random.Random
import kotlin.uuid.Uuid

@Composable
fun DbPage() {
    var dbTestResult by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Database Test",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    try {
                        val db = DatabaseManager.getDatabase()

                        // Test data - create sample UUIDs
                        val randomId = Random.nextLong()
                        val currentTime = randomId // Use random ID as timestamp for testing
                        val identityId = Uuid.random()
                        val driveId = Uuid.random()
                        val fileId = Uuid.random()
                        val versionTag = "version-tag-$randomId".encodeToByteArray()
                        val driveAlias = "alias".encodeToByteArray()
                        val driveType = "type".encodeToByteArray()

                        // Write a record to DriveMainIndex
                        db.driveMainIndexQueries.upsertDriveMainIndex(
                            identityId = identityId,
                            driveId = driveId,
                            fileId = fileId,
                            uniqueId = Uuid.random(),
                            globalTransitId = null,
                            groupId = null,
                            senderId = "sender@example.com",
                            fileType = 1L,
                            dataType = 1L,
                            archivalStatus = 0L,
                            historyStatus = 0L,
                            userDate = currentTime,
                            created = currentTime,
                            modified = currentTime,
                            systemFileType = 1L,
                            jsonHeader = """{"versionTag":"${versionTag.contentToString()}","byteCount":1024,"appData":{},"serverData":{},"fileMetaData":{}}"""
                        )

                        // Read back all records
                        val records = db.driveMainIndexQueries.selectAll().executeAsList()
                        val count = db.driveMainIndexQueries.countAll().executeAsOne()

                        dbTestResult = "Success!\nWrote 1 record\nTotal records: $count\nLast record fileId: ${records.lastOrNull()?.fileId}"
                    } catch (e: Exception) {
                        dbTestResult = "Error: ${e.message}\n${e.stackTraceToString()}"
                    }
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text("Test Database Write/Read")
        }

        if (dbTestResult.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = dbTestResult,
                style = MaterialTheme.typography.bodySmall,
                color = if (dbTestResult.startsWith("Success"))
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Preview
@Composable
fun DbPagePreview() {
    MaterialTheme {
        DbPage()
    }
}
