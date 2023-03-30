package org.dlog.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import org.dlog.repository.DraftRepository


class ContactListViewModel(private val author: String,
                           private val repository: DraftRepository) : ViewModel() {

    var draftsPaged = Pager(PagingConfig(
        pageSize = 10,
        prefetchDistance = 10,
        enablePlaceholders = false,
    )) {
        repository.getPagedDraft(author)
    }.flow.cachedIn(viewModelScope)

}
