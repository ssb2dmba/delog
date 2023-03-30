 package org.dlog.db.dao

import androidx.room.*
import org.dlog.db.model.AuthorInfo

@Dao
interface AuthorInfoDao {
    
    @Delete
    fun delete(authorInfo: AuthorInfo)

    @Insert
    fun insert(authorInfo: AuthorInfo)

    @Query("SELECT * FROM authorinfo WHERE author = :author")
    fun getByAuthor(author: String): AuthorInfo?

    @Update
    fun update(authorInfo: AuthorInfo)

}
