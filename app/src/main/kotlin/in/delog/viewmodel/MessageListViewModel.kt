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

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import `in`.delog.db.AppDatabaseView
import `in`.delog.db.model.IdentAndAboutWithBlob
import `in`.delog.db.model.Message
import `in`.delog.db.repository.BlobRepository
import `in`.delog.db.repository.IdentRepository
import `in`.delog.db.repository.MessageRepository
import `in`.delog.db.repository.MessageTreeRepository
import `in`.delog.model.MessageViewData
import `in`.delog.model.toMessageViewData
import `in`.delog.service.ssb.SsbService
import `in`.delog.service.ssb.SsbService.Companion.TAG
import `in`.delog.service.ssb.SsbService.Companion.format
import `in`.delog.service.ssb.TorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class FeedMainUIState(
    val messagesPaged: Flow<PagingData<AppDatabaseView.MessageInTree>>? = null,
    val identAndAbout: IdentAndAboutWithBlob? = null,
    val torStatus: Int? = -1
)

class MessageListViewModel(
    private var key: String,
    private val ssbService: SsbService,
    private val messageTreeRepository: MessageTreeRepository,
    private val identRepository: IdentRepository,
    private val messageRepository: MessageRepository,
    private val blobRepository: BlobRepository,
    private val torService: TorService
    ) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedMainUIState())
    val uiState: StateFlow<FeedMainUIState> = _uiState.asStateFlow()
    var messagesPaged: Flow<PagingData<MessageViewData>>? = null

    fun synchronize() {
            viewModelScope.launch (Dispatchers.IO){
                ssbService.replicateSync()
            }
    }

    fun clearError() {
        ssbService.clearError()
    }

    fun onDeletePost(messageViewData: MessageViewData) {
        Log.i(TAG, "delete not implemented ${messageViewData.key}")
    }


    init {
        torService.status.observeForever {
            _uiState.update { it.copy(torStatus = torService.status.value) }
        }
        // setup the feed of interest
        viewModelScope.launch(Dispatchers.IO) {
            if (key.startsWith("%")) {
                val m: Message? = messageRepository.getMessage(key)
                if (m != null) {
                    _uiState.update { it.copy(identAndAbout = identRepository.getFeed(m.author)) }
                }
            }
            if (key.startsWith("@")) {
                _uiState.update { it.copy(identAndAbout = identRepository.getFeed(key)) }
            }
            if (_uiState.value.identAndAbout == null) { // fallback to our
                if (key.startsWith("@")) {
                    _uiState.update { it.copy(identAndAbout = identRepository.getFeed(key)) }
                } else {
                    identRepository.default.collect { ident ->
                        _uiState.update { it.copy(identAndAbout = ident) }
                    }
                }

            }
        }
        // set up the message list
        viewModelScope.launch(Dispatchers.IO) {
            messagesPaged = Pager(
                PagingConfig(
                    pageSize = 10,
                    prefetchDistance = 10,
                    enablePlaceholders = false
                )
            ) {
                if (key.startsWith("@")) {
                    messageTreeRepository.getPagedMessageByAuthor(key)
                } else { // starts with %
                    messageTreeRepository.getPagedMessageByKey(key)
                }
            }.flow.map { pagingData ->
                    pagingData.map { msgInTree ->
                        msgInTree.toMessageViewData(format, blobRepository)
                    }
                }.cachedIn(viewModelScope)
            synchronize()
        }
    }
}
