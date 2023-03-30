package org.dlog.db.dao

import androidx.room.*
import org.dlog.db.model.AuthorInfo

@Dao
interface PubServerDao {
    
    @Delete
    fun delete(contact: AuthorInfo)

    @Insert
    fun insert(contact: AuthorInfo)

    @Query("SELECT * FROM authorinfo WHERE author = :author")
    fun getByAuthor(author: String): AuthorInfo?

    @Update
    fun update(authorInfo: AuthorInfo)

}
