package org.dlog.repository

import androidx.paging.PagingSource
import org.dlog.db.dao.DraftDao
import org.dlog.db.model.Draft


interface DraftRepository {
    suspend fun insert(draft: Draft)
    suspend fun deleteDraft(draft: Draft)
    fun getPagedDraft(author: String): PagingSource<Int, Draft>
    fun getById(oid: Int): Draft
    fun update(draft: Draft)
}

class DraftRepositoryImpl (private val draftDao: DraftDao) : DraftRepository  {

    override suspend fun insert(draft: Draft) {
        draftDao.insert(draft)
    }

    override suspend fun deleteDraft(draft: Draft) {
        draftDao.deleteDraft(draft)
    }

    override fun getPagedDraft(author: String): PagingSource<Int, Draft> {
        println("getPagedDraft:" + author)
        return draftDao.getPagedDraft(author)
    }

    override fun getById(oid: Int): Draft {
        return draftDao.getById(oid)
    }

    override fun update(draft: Draft) {
        System.out.println("repo is updating " + draft)
        return draftDao.update(draft)
    }

}
