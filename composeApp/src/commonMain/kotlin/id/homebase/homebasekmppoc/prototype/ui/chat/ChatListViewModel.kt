package id.homebase.homebasekmppoc.prototype.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.homebase.homebasekmppoc.prototype.lib.chat.ConversationData
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface ChatListUiEvent {
    data class NavigateToMessages(val conversation: ConversationData) : ChatListUiEvent
    data object NavigateBack : ChatListUiEvent
}

sealed interface ChatListUiAction {
    data class ConversationClicked(val conversation: ConversationData) : ChatListUiAction
    data object BackClicked : ChatListUiAction
}

class ChatListViewModel : ViewModel() {

    private val _uiEvent = Channel<ChatListUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onAction(action: ChatListUiAction) {
        when (action) {
            is ChatListUiAction.ConversationClicked -> {
                sendEvent(ChatListUiEvent.NavigateToMessages(action.conversation))
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
