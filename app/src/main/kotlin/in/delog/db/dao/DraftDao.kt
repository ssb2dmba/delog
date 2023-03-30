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
import androidx.room.*
import `in`.delog.db.model.Draft

@Dao
interface DraftDao {

    @Query("SELECT * FROM draft WHERE author = :author order by timestamp desc")
    fun getPagedDraft(author: String): PagingSource<Int, Draft>

    @Delete
    fun deleteDraft(feedOid: Draft)

    @Insert
    fun insert(draft: Draft): Long

    @Query("SELECT * FROM draft WHERE oid = :oid ")
    fun getById(oid: Int): Draft

    @Update
    fun update(draft: Draft)

    @Query("SELECT * FROM draft WHERE author = :author order by oid desc limit 1")
    fun last(author: String): Draft

}
