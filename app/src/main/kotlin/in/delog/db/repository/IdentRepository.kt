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

import androidx.lifecycle.LiveData
import androidx.room.Transaction
import `in`.delog.db.dao.AboutDao
import `in`.delog.db.dao.IdentDao
import `in`.delog.db.model.About
import `in`.delog.db.model.Ident
import `in`.delog.db.model.IdentAndAbout
import `in`.delog.db.model.IdentAndAboutWithBlob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


/**
 * Repository to provide Identity data
 */

interface IdentRepository {
    val default: Flow<IdentAndAboutWithBlob?>
    val idents: Flow<List<IdentAndAboutWithBlob>>
    val count: LiveData<Int>
    suspend fun insert(feed: IdentAndAbout): Long
    suspend fun update(feed: Ident)
    suspend fun delete(feed: Ident)
    suspend fun findByIdOLD(id: String): IdentAndAbout

    suspend fun findById(id: String): IdentAndAboutWithBlob
    suspend fun getLive(id: String): LiveData<Ident>
    fun setFeedAsDefaultFeed(it: Ident)
    suspend fun findByPublicKey(pk: String): IdentAndAbout?
    suspend fun cleanInvite(newIdent: Ident)
    suspend fun getFeed(author: String): IdentAndAboutWithBlob?

}

class FeedRepositoryImpl(
    private val identDao: IdentDao,
    private val aboutDao: AboutDao,
    private val blobRepository: BlobRepository
) :
    IdentRepository {

    override val idents: Flow<List<IdentAndAboutWithBlob>> = identDao.getAllLive().map { list ->
        list.map { identAndAbout ->
            makeIdentAndAboutWithBlob(identAndAbout)
        }
    }

    override val default: Flow<IdentAndAboutWithBlob?> = identDao.getDefaultFeed().map { identAndAbout ->
        if (identAndAbout != null) {
            makeIdentAndAboutWithBlob(identAndAbout)
        } else {
            null
        }
    }

    override val count = identDao.liveCount()


    override fun setFeedAsDefaultFeed(it: Ident) {
        identDao.setFeedAsDefaultFeed(it.oid)
    }

    override suspend fun insert(feed: IdentAndAbout): Long {
        val id = identDao.insert(feed = feed.ident)
        identDao.setFeedAsDefaultFeed(id)
        aboutDao.insert(feed.about!!)
        return id
    }

    override suspend fun update(feed: Ident) {
        identDao.update(feed)
    }

    override suspend fun delete(feed: Ident) {
        identDao.delete(feed)
    }

    override suspend fun findByIdOLD(id: String): IdentAndAbout {
        return identDao.findByOId(id)
    }

    override suspend fun findById(id: String): IdentAndAboutWithBlob {
        return makeIdentAndAboutWithBlob(identDao.findByOId(id))
    }

    override suspend fun getFeed(author: String): IdentAndAboutWithBlob? {
        return identDao.findByPublicKey(author)?.let { makeIdentAndAboutWithBlob(it) }
    }

    private suspend fun makeIdentAndAboutWithBlob(identAndAbout: IdentAndAbout): IdentAndAboutWithBlob {
        if (identAndAbout==null) { // TODO used at startup ...
            return IdentAndAboutWithBlob(
                ident = Ident.empty(""),
                about = About.empty(""),
                profileImage = null
            )
        }
        return IdentAndAboutWithBlob(
            ident = identAndAbout.ident,
            about = identAndAbout.about ?: About.empty(identAndAbout.ident.publicKey),
            profileImage = identAndAbout.about?.image?.let {
                blobRepository.getAsBlobItem(it).uri
            }
        )
    }

    override suspend fun findByPublicKey(pk: String): IdentAndAbout? {
        return identDao.findByPublicKey(pk)
    }

    override suspend fun cleanInvite(newIdent: Ident) {
        return identDao.cleanInvite(newIdent.oid)
    }

    override suspend fun getLive(id: String): LiveData<Ident> {
        return identDao.findByIdLive(id)
    }

}
