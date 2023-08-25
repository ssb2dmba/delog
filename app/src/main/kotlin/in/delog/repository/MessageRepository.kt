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

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import `in`.delog.db.dao.MessageDao
import `in`.delog.db.model.IdentAndAbout
import `in`.delog.db.model.Message
import `in`.delog.db.model.MessageAndAbout


interface MessageRepository {
    suspend fun addMessage(feedOid: Message)
    suspend fun existsMessage(key: String): Boolean
    suspend fun maybeAddMessage(message: Message)
    suspend fun findByDefaultFeed(): LiveData<List<Message>>
    fun getPagedMessages(msgKey: String): PagingSource<Int, MessageAndAbout>
    fun getMessagePage(author: String, seqStart: Long, limit: Int): List<Message>
    fun getLastMessage(author: String): Message?
    fun getLastSequence(author: String): Long
    fun getPagedFeed(author: String): PagingSource<Int, MessageAndAbout>
    fun getFeed(key: String): IdentAndAbout
    fun getMessageAndAbout(key: String): MessageAndAbout?

    fun getMessage(key: String): Message?
}

class MessageRepositoryImpl(private val messageDao: MessageDao) : MessageRepository {

    override fun getMessage(key: String): Message? {
        return messageDao.getMessage(key)
    }

    override fun getMessageAndAbout(key: String): MessageAndAbout? {
        return messageDao.getMessageAndAbout(key)
    }

    override fun getLastMessage(author: String): Message? {
        return messageDao.getLastMessage(author)
    }

    override fun getLastSequence(author: String): Long {
        var m = messageDao.getLastMessage(author)
        return if (m == null) 0L else m.sequence // todo sql optimize
    }

    override suspend fun addMessage(message: Message) {
        messageDao.insert(message)
    }

    override suspend fun existsMessage(key: String): Boolean {
        return messageDao.existsByKey(key)
    }

    override suspend fun maybeAddMessage(message: Message) {
        // TODO validate signature here
        if (!existsMessage(message.key)) {
            try {
                addMessage(message)
            } catch (e: SQLiteConstraintException) {
                // some race conditions can occur
                // not in @Transaction for now cause otherwise might introducte too slowess
            }
        }
    }

    override suspend fun findByDefaultFeed(): LiveData<List<Message>> {
        return messageDao.findByDefaultFeed()
    }

    override fun getFeed(key: String): IdentAndAbout {
        return messageDao.getFeed(key)
    }

    override fun getPagedFeed(author: String): PagingSource<Int, MessageAndAbout> {
        return messageDao.getPagedFeed(author)
    }


    override fun getPagedMessages(author: String): PagingSource<Int, MessageAndAbout> {
        return messageDao.getPagedMessages(author)
    }

    override fun getMessagePage(author: String, seqStart: Long, limit: Int): List<Message> {
        return messageDao.getMessagesPage(author, seqStart, limit)
    }

}
