package org.dlog.repository

import androidx.paging.PagingSource
import org.dlog.db.dao.AuthorInfoDao
import org.dlog.db.model.AuthorInfo


interface AuthorInfoRepository {
    suspend fun insert(authorInfo: AuthorInfo)
    suspend fun delete(authorInfo: AuthorInfo)
    fun update(authorInfo: AuthorInfo)
}

class AuthorInfoRepositoryImpl (private val authorInfoDao: AuthorInfoDao) : AuthorInfoRepository  {

    override suspend fun insert(authorInfo: AuthorInfo) {
        authorInfoDao.insert(authorInfo)
    }

    override suspend fun delete(authorInfo: AuthorInfo) {
        authorInfoDao.delete(authorInfo)
    }

    override fun update(authorInfo: AuthorInfo) {
        return authorInfoDao.update(authorInfo)
    }

}
