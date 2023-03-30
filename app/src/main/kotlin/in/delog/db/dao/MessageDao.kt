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
package `in`.delog.db.dao

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import `in`.delog.db.model.IdentAndAbout
import `in`.delog.db.model.Message
import `in`.delog.db.model.MessageAndAbout
import java.util.*

@Dao
interface MessageDao {

    @Query("SELECT * FROM message WHERE type='post' and author = :author or author IN (select follow from contact where author = :author and value = 1) order by oid desc")
    fun getPagedPosts(author: String): PagingSource<Int, MessageAndAbout>

    @Query("SELECT * FROM message WHERE type='post' and author = :author or author IN (select follow from contact where author = :author and value = 1) order by oid desc")
    fun getPagedPostsAndAbout(author: String): PagingSource<Int, MessageAndAbout>

    @Query("SELECT * FROM message WHERE type='post' and author = :author order by oid desc")
    fun getPagedFeed(author: String): PagingSource<Int, MessageAndAbout>


    @Query("SELECT * FROM message WHERE key = :key order by oid desc") // union in reply to
    fun getPagedMessages(key: String): PagingSource<Int, MessageAndAbout>

    @Query("SELECT * FROM message WHERE author = :author and sequence > :sequence order by sequence asc LIMIT :limit")
    fun getMessagesPage(author: String, sequence: Long, limit: Int): List<Message>

    @Query("SELECT * FROM message WHERE key = :key LIMIT 1")
    fun findByKey(key: String): Message

    @Query("SELECT EXISTS(SELECT * FROM message WHERE key = :key)")
    fun existsByKey(key: String): Boolean

    @Insert
    fun insert(message: Message)

    @Query("SELECT * FROM message ORDER BY timestamp desc")
    fun findByDefaultFeed(): LiveData<List<Message>>


    @Query("SELECT * FROM message WHERE author = :author order by sequence desc limit 1")
    fun getLastMessage(author: String): Message?

    @Query("SELECT * FROM ident WHERE public_key = :pk LIMIT 1")
    fun getFeed(pk: String): IdentAndAbout

    @Query("SELECT * FROM message WHERE key = :key LIMIT 1")
    fun getMessage(key: String): Message?


}