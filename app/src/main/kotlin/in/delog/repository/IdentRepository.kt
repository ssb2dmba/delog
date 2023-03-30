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

import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow
import `in`.delog.db.dao.IdentDao
import `in`.delog.db.dao.setDefault
import `in`.delog.db.model.Ident
import `in`.delog.db.model.IdentAndAbout


/**
 * Repository to provide Identity data
 */

interface IdentRepository {
    val default: Flow<IdentAndAbout>
    val idents: Flow<List<IdentAndAbout>>
    val count: LiveData<Int>
    suspend fun insert(feed: Ident): Long
    suspend fun update(feed: Ident)
    suspend fun delete(feed: Ident)
    suspend fun findById(id: String): IdentAndAbout
    suspend fun getLive(id: String): LiveData<Ident>
    fun setDefault(it: Ident)
    suspend fun findByPublicKey(pk: String): IdentAndAbout
}

class FeedRepositoryImpl(private val identDao: IdentDao) : IdentRepository {

    override val idents: Flow<List<IdentAndAbout>> = identDao.getAllLive()

    override val default: Flow<IdentAndAbout> = identDao.getDefault()

    override val count = identDao.liveCount()

    override fun setDefault(it: Ident) {
        identDao.setDefault(it.oid)
    }

    override suspend fun insert(feed: Ident): Long {
        return identDao.insert(feed = feed)
    }

    override suspend fun update(feed: Ident) {
        identDao.update(feed)
    }

    override suspend fun delete(feed: Ident) {
        identDao.delete(feed)
    }

    override suspend fun findById(id: String): IdentAndAbout {
        return identDao.findById(id)
    }

    override suspend fun findByPublicKey(pk: String): IdentAndAbout {
        return identDao.findByPublicKey(pk)
    }

    override suspend fun getLive(id: String): LiveData<Ident> {
        return identDao.findByIdLive(id)
    }


}
