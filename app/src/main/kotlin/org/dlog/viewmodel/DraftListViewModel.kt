package org.dlog.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.dlog.db.model.Message
import org.dlog.repository.MessageRepository


class DraftListViewModel(private val repository: MessageRepository) : ViewModel() {

    val messagesPaged = Pager(PagingConfig(
        pageSize = 10,
        prefetchDistance = 10,
        enablePlaceholders = false,
    )) {
        repository.getPagedMessages(1)
    }.flow.cachedIn(viewModelScope)

}
