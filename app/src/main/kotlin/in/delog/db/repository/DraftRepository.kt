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
package `in`.delog.db.repository

import androidx.paging.PagingSource
import `in`.delog.db.dao.DraftDao
import `in`.delog.db.model.Draft


interface DraftRepository {
    suspend fun insert(draft: Draft): Long
    suspend fun deleteDraft(draft: Draft)
    fun getPagedDraft(author: String): PagingSource<Int, Draft>
    fun getById(oid: Long): Draft
    fun update(draft: Draft)
    fun last(author: String): Draft
}

class DraftRepositoryImpl(private val draftDao: DraftDao) : DraftRepository {

    override fun last(author: String): Draft {
        return draftDao.last(author)
    }

    override suspend fun insert(draft: Draft): Long {
        return draftDao.insert(draft)
    }

    override suspend fun deleteDraft(draft: Draft) {
        draftDao.deleteDraft(draft)
    }

    override fun getPagedDraft(author: String): PagingSource<Int, Draft> {
        return draftDao.getPagedDraft(author)
    }

    override fun getById(oid: Long): Draft {
        return draftDao.getById(oid)
    }

    override fun update(draft: Draft) {
        return draftDao.update(draft)
    }

}
