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
import androidx.paging.*
import `in`.delog.db.AppDatabaseView
import `in`.delog.db.model.IdentAndAbout
import `in`.delog.db.model.Message
import `in`.delog.repository.MessageRepository
import `in`.delog.repository.MessageTreeRepository
import `in`.delog.ui.component.UrlCachedPreviewer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


class MessageListViewModel(
    private var key: String,
    private val messageTreeRepository: MessageTreeRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {

    var messagesPaged: Flow<PagingData<AppDatabaseView.MessageInTree>>? = null
    var identAndAbout: IdentAndAbout? = null

    init {
        GlobalScope.launch(Dispatchers.IO) {
            if (key.startsWith("%")) {
                val m: Message? = messageRepository.getMessage(key)
                if (m != null) {
                    identAndAbout = messageRepository.getFeed(m.author)
                }
            }
            if (identAndAbout==null) { // fallback to our
                identAndAbout = messageRepository.getFeed(key)
            }
        }

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
                pagingData.map { msgAndAbout ->
                    UrlCachedPreviewer.preloadPreviewsFor(msgAndAbout)
                    msgAndAbout
                }
            }.cachedIn(viewModelScope)
        }
    }
}
