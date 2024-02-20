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

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import `in`.delog.db.AppDatabaseView

@Dao
interface MessageTreeDao {

    @Transaction
    @Query("select m1.*  from MessageTree m1 where  m1.author = :author or m1.author IN (select follow from contact where author = :author  and value = 1)")
    fun getPagedFeed(author: String): PagingSource<Int, AppDatabaseView.MessageInTree>


    @Transaction
    @Query("select m1.*  from MessageTree m1 where  m1.key = :key or m1.root = :key")
    fun getPagedMessage(key: String): PagingSource<Int, AppDatabaseView.MessageInTree>

}