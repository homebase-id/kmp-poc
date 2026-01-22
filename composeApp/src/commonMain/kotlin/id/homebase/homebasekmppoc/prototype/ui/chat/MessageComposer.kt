package id.homebase.homebasekmppoc.prototype.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MessageComposer(
    text: String,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onSendMessage: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.material3.OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Hard coded to send to sam.dotyou.cloud...") },
            minLines = 2,
            maxLines = 4,
            enabled = enabled
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onSendMessage,
            enabled = enabled && text.isNotBlank(),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Send")
        }
    }
}
