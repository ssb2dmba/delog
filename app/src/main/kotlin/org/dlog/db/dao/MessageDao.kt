package org.dlog.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.dlog.db.model.Feed

@Dao
interface MessageDao {

    @Query("SELECT * FROM feed")
    fun getAll(): LiveData<List<Feed>>

    @Query("SELECT * FROM feed WHERE oid = :oid LIMIT 1")
    fun findById(oid: String): Feed

    @Query("SELECT * FROM feed WHERE oid = :oid LIMIT 1")
    fun findByIdLive(oid: String): LiveData<Feed>

    @Insert
    fun insert(feed: Feed)

    @Delete
    fun delete(feed: Feed)

    @Update
    fun update(feed: Feed)

    @Query("SELECT count(*) FROM feed")
    fun count(): Int

    @Query("SELECT count(*) FROM feed")
    fun liveCount(): LiveData<Int>

    @Query("SELECT * FROM feed ORDER BY default_feed desc LIMIT 1")
    fun getDefaultFeed(): Feed

}