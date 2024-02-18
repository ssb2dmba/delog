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
import androidx.room.Transaction
import `in`.delog.db.model.IdentAndAbout
import `in`.delog.db.model.Message
import `in`.delog.db.model.MessageAndAbout

@Dao
interface MessageDao {

    // expensive query used for message deletion check only
    @Query("select count(*) from message where contentAsText like :key")
    fun countMessagesWithBlob(key: String): Int

    @Transaction
    @Query("SELECT * FROM message WHERE type='post' and author = :author or author IN (select follow from contact where author = :author and value = 1) order by oid desc")
    fun getPagedPosts(author: String): PagingSource<Int, MessageAndAbout>

    @Transaction
    @Query(
        "select m1.*  from message m1\n" +
                "left join message  m2 on m2.`key`=m1.root \n" +
                "where  m1.author = :author or m1.author IN (select follow from contact where author = :author  and value = 1)\n" +
                "order by min(coalesce(m2.timestamp ,9223372036854775807),m1.timestamp) desc, m2.timestamp asc\n"
    )
    fun getPagedFeed(author: String): PagingSource<Int, MessageAndAbout>


    @Transaction
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

    @Query(
        "select sequence as seq from message where author= :author\n" +
                "UNION ALL \n" +
                "SELECT 0 as seq\n" +
                "order by seq desc limit 1"
    )
    fun getLastSequence(author: String): Long

    @Query("SELECT * FROM ident WHERE public_key = :pk LIMIT 1")
    fun getFeed(pk: String): IdentAndAbout

    @Transaction
    @Query("SELECT * FROM message WHERE key = :key LIMIT 1")
    fun getMessageAndAbout(key: String): MessageAndAbout?

    @Query("SELECT * FROM message WHERE key = :key LIMIT 1")
    fun getMessage(key: String): Message?

}