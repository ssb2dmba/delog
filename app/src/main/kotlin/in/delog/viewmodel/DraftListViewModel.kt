/**
 * Delog
 * Copyright (C) 2023 dmba.info
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package `in`.delog.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import `in`.delog.db.repository.BlobRepository
import `in`.delog.db.repository.DraftRepository
import `in`.delog.model.MessageViewData
import `in`.delog.model.toMessageViewData
import `in`.delog.service.ssb.SsbService.Companion.format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


class DraftListViewModel(
    private val author: String,
    private val repository: DraftRepository,
    private val blobRepository: BlobRepository,
) : ViewModel() {

    var messageViewData: Flow<PagingData<MessageViewData>>? = null
    init {
        viewModelScope.launch(Dispatchers.IO){
            messageViewData = Pager(
                PagingConfig(
                    pageSize = 10,
                    prefetchDistance = 10,
                    enablePlaceholders = false,
                )
            ) {
                repository.getPagedDraft(author)
            }.flow.map { pagingData ->
                pagingData.map { draft ->
                    draft.toMessageViewData(format, blobRepository)
                }
            }.cachedIn(viewModelScope)
        }
    }

}
