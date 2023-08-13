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
package `in`.delog.repository

import androidx.paging.PagingSource
import `in`.delog.db.AppDatabaseView
import `in`.delog.db.dao.MessageTreeDao


interface MessageTreeRepository {

    fun getPagedMessageByAuthor(author: String): PagingSource<Int, AppDatabaseView.MessageInTree>
    fun getPagedMessageByKey(author: String): PagingSource<Int, AppDatabaseView.MessageInTree>
}

class MessageTreeRepositoryImpl(private val messageTreeDao: MessageTreeDao) : MessageTreeRepository {

    // by author
    override fun getPagedMessageByAuthor(author: String): PagingSource<Int, AppDatabaseView.MessageInTree> {
        return messageTreeDao.getPagedFeed(author)
    }

    // by message key
    override fun getPagedMessageByKey(key: String): PagingSource<Int, AppDatabaseView.MessageInTree> {
        return messageTreeDao.getPagedMessage(key)
    }

}
