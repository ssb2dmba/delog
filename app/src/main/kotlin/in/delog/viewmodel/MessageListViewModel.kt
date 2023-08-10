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
import `in`.delog.db.model.IdentAndAbout
import `in`.delog.db.model.Message
import `in`.delog.repository.MessageRepository
import `in`.delog.ui.component.MessageViewData
import `in`.delog.ui.component.UrlCachedPreviewer
import `in`.delog.ui.component.toMessageViewData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


class MessageListViewModel(
    private var key: String,
    private val repository: MessageRepository
) : ViewModel() {

    var messagesPaged: Flow<PagingData<MessageViewData>>? = null
    var identAndAbout: IdentAndAbout? = null
    init {
        viewModelScope.launch(Dispatchers.IO) {
            System.out.println("init with " + key)
            if (key.startsWith("%")) {
                val m: Message? = repository.getMessage(key)
                if (m != null) key = m.author
            }
            identAndAbout = repository.getFeed(key)
            messagesPaged = Pager(
                PagingConfig(
                    pageSize = 10,
                    prefetchDistance = 10,
                    enablePlaceholders = false
                )
            ) {
                if (key.startsWith("@")) {
                    System.out.println("getPagedFeed " + key)
                    repository.getPagedFeed(key)
                } else { // starts with %
                    System.out.println("getPagedPost")
                    repository.getPagedPosts(key)
                }
            }.flow.map { pagingData ->
                pagingData.map { msgAndAbout ->
                    var msgViewData = msgAndAbout.message.toMessageViewData()
                    if (msgAndAbout.about != null) {
                        msgViewData.authorName = msgAndAbout.about!!.name
                        msgViewData.authorImage = msgAndAbout.about!!.image
                    }
                    UrlCachedPreviewer.preloadPreviewsFor(msgViewData)
                    msgViewData
                }
            }.cachedIn(viewModelScope)
        }
    }


}
