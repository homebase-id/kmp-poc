package id.homebase.homebasekmppoc.prototype.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface ChatListUiEvent {
    data class NavigateToMessages(val conversationId: String) : ChatListUiEvent
    data object NavigateBack : ChatListUiEvent
}

sealed interface ChatListUiAction {
    data class ConversationClicked(val conversationId: String) : ChatListUiAction
    data object BackClicked : ChatListUiAction
}

class ChatListViewModel : ViewModel() {

    private val _uiEvent = Channel<ChatListUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onAction(action: ChatListUiAction) {
        when (action) {
            is ChatListUiAction.ConversationClicked -> {
                sendEvent(ChatListUiEvent.NavigateToMessages(action.conversationId))
            }
            ChatListUiAction.BackClicked -> {
                sendEvent(ChatListUiEvent.NavigateBack)
            }
        }
    }

    private fun sendEvent(event: ChatListUiEvent) {
        viewModelScope.launch { _uiEvent.send(event) }
    }
}
