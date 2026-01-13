package id.homebase.homebasekmppoc.prototype.ui.driveFetch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.homebase.homebasekmppoc.lib.image.toImageBitmap
import id.homebase.homebasekmppoc.prototype.lib.drives.files.PayloadDescriptor
import id.homebase.homebasekmppoc.prototype.lib.drives.files.BytesResponse
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment

@Composable
fun PayloadPreview(
    payload: PayloadDescriptor,
    bytes: BytesResponse,
    onGetPayloadRange: (key: String, start: Long, length: Long) -> Unit
) {
    Column {
        Text(
            text = "Payload (${bytes.bytes.size} bytes)",
            style = MaterialTheme.typography.labelMedium
        )

        Spacer(Modifier.height(4.dp))

        val contentType = if (payload.contentType.isNullOrEmpty()) "application/octet-stream" else bytes.contentType

        Text(
            text = contentType,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        when {
            contentType.startsWith("image/") -> {
                PayloadImage(bytes.bytes)
            }

            contentType == "text/plain" -> {
                PayloadText(
                    payloadKey = payload.key,
                    bytes = bytes.bytes,
                    onGetPayloadRange = onGetPayloadRange
                )
            }

            else -> {
                PayloadHex(bytes.bytes)
            }
        }
    }
}


@Composable
private fun PayloadImage(bytes: ByteArray) {
    val imageBitmap = remember(bytes) {
        bytes.toImageBitmap()
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "Payload image",
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
        )
    } else {
        Text(
            text = "Unable to render image",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun PayloadText(
    payloadKey: String,
    bytes: ByteArray,
    onGetPayloadRange: (key: String, start: Long, length: Long) -> Unit
) {
    val text = remember(bytes) {
        runCatching {
            bytes.decodeToString()
        }.getOrElse {
            "Unable to decode text payload"
        }
    }

    val totalSize = bytes.size.toLong()
    val rangeLength = 10L
    val rangeStart =
        if (totalSize > rangeLength)
            (totalSize / 2) - (rangeLength / 2)
        else
            0L

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Text preview",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f)
            )

            TextButton(
                onClick = {
                    onGetPayloadRange(
                        payloadKey,
                        rangeStart.coerceAtLeast(0),
                        minOf(rangeLength, totalSize)
                    )
                }
            ) {
                Text("Get middle range")
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@Composable
private fun PayloadHex(bytes: ByteArray) {
    val hex = remember(bytes) {
        bytes
            .take(512)
            .joinToString(" ") { it.toHex() }
    }

    Text(
        text = hex,
        style = MaterialTheme.typography.bodySmall
    )
}


private fun Byte.toHex(): String {
    val v = toInt() and 0xFF
    return "0123456789ABCDEF"[v ushr 4].toString() +
            "0123456789ABCDEF"[v and 0x0F]
}
