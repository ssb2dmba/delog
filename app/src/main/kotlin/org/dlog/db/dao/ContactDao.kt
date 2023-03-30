package org.dlog.db.dao

import androidx.paging.PagingSource
import androidx.room.*
import org.dlog.db.model.Draft

@Dao
interface ContactDao {

    @Query("SELECT * FROM draft WHERE author = :author order by timestamp desc")
    fun getPagedDraft(author: String): PagingSource<Int, Draft>

    @Delete
    fun deleteDraft(feedOid: Draft)

    @Insert
    fun insert(draft: Draft)

    @Query("SELECT * FROM draft WHERE oid = :oid ")
    fun getById(oid: Int): Draft

    @Update
    fun update(draft: Draft)

}
