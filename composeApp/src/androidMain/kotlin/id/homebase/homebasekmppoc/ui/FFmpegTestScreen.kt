package id.homebase.homebasekmppoc.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.homebase.homebasekmppoc.media.FFmpegUtils
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import java.io.File
import kotlinx.coroutines.launch

@Composable
actual fun FFmpegTestScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedFilePath by remember { mutableStateOf<String?>(null) }
    var processing by remember { mutableStateOf(false) }
    var logText by remember { mutableStateOf("Logs will appear here...") }

    fun pickVideo() {
        scope.launch {
            try {
                processing = true
                val file =
                        FileKit.openFilePicker(type = FileKitType.Video, title = "Select a Video")
                if (file != null) {
                    val bytes = file.readBytes()
                    val paramsName = file.name
                    val cacheFile = File(context.cacheDir, "input_$paramsName")
                    cacheFile.writeBytes(bytes)
                    selectedFilePath = cacheFile.absolutePath
                    logText = "Selected: ${cacheFile.absolutePath} (${cacheFile.length()} bytes)"
                }
            } catch (e: Exception) {
                logText = "Error picking file: ${e.message}"
            } finally {
                processing = false
            }
        }
    }

    Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { onBack() }, modifier = Modifier.align(Alignment.Start)) { Text("Back") }

        Spacer(modifier = Modifier.height(16.dp))

        Text("FFmpeg Test Playground", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { pickVideo() }, enabled = !processing) {
            Text(if (selectedFilePath == null) "Select Video" else "Change Video")
        }

        if (selectedFilePath != null) {
            Text(
                    "Input: ${File(selectedFilePath!!).name}",
                    style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                    onClick = {
                        scope.launch {
                            processing = true
                            logText += "\nStarting Compression..."
                            try {
                                val result = FFmpegUtils.compressVideo(selectedFilePath!!)
                                logText += "\nComputed: $result"
                                if (result != null)
                                        Toast.makeText(context, "Compressed!", Toast.LENGTH_SHORT)
                                                .show()
                            } catch (e: Exception) {
                                logText += "\nError: ${e.message}"
                            }
                            processing = false
                        }
                    },
                    enabled = !processing
            ) { Text("Compress Video") }

            Button(
                    onClick = {
                        scope.launch {
                            processing = true
                            logText += "\nStarting Segmentation..."
                            try {
                                val result = FFmpegUtils.segmentVideo(selectedFilePath!!)
                                logText +=
                                        "\nPlaylist: ${result?.first}\nSegment: ${result?.second}"
                                if (result != null)
                                        Toast.makeText(context, "Segmented!", Toast.LENGTH_SHORT)
                                                .show()
                            } catch (e: Exception) {
                                logText += "\nError: ${e.message}"
                            }
                            processing = false
                        }
                    },
                    enabled = !processing
            ) { Text("Segment (HLS)") }

            Button(
                    onClick = {
                        scope.launch {
                            processing = true
                            logText += "\nExtracting Thumbnail..."
                            try {
                                val result = FFmpegUtils.grabThumbnail(selectedFilePath!!)
                                logText += "\nThumbnail: $result"
                                if (result != null)
                                        Toast.makeText(
                                                        context,
                                                        "Thumbnail Generated!",
                                                        Toast.LENGTH_SHORT
                                                )
                                                .show()
                            } catch (e: Exception) {
                                logText += "\nError: ${e.message}"
                            }
                            processing = false
                        }
                    },
                    enabled = !processing
            ) { Text("Grab Thumbnail") }

            Button(
                    onClick = {
                        scope.launch {
                            processing = true
                            logText += "\nChecking Rotation..."
                            try {
                                val rotation = FFmpegUtils.getRotationFromFile(selectedFilePath!!)
                                logText += "\nRotation: $rotation degrees"
                            } catch (e: Exception) {
                                logText += "\nError: ${e.message}"
                            }
                            processing = false
                        }
                    },
                    enabled = !processing
            ) { Text("Get Rotation") }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (processing) {
            CircularProgressIndicator()
        }

        Text(
                logText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
        )
    }
}
