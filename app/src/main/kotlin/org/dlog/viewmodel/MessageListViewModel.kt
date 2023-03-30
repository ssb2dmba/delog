package org.dlog.viewmodel


import android.annotation.SuppressLint
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.dlog.db.model.Message
import org.dlog.repository.MessageRepository


class MessageViewModel(private val repository: MessageRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagesUIState(loading = true))
    val uiState: StateFlow<MessagesUIState> = _uiState

    val messageDataSource : DataSource.Factory<Int, Message> = repository.getPagedMessages(1)
    @SuppressLint("RestrictedApi")
    val messageList = Pager(
        PagingConfig(pageSize = 10),
        null,
        messageDataSource.asPagingSourceFactory(
            ArchTaskExecutor.getIOThreadExecutor().asCoroutineDispatcher()
        )
    ).liveData

}

data class MessagesUIState(
    val messages: List<Message> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)
