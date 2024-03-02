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

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import `in`.delog.db.model.Ident
import `in`.delog.db.model.IdentAndAbout
import kotlinx.coroutines.flow.Flow

@Dao
interface IdentDao {

    @Query("SELECT * FROM ident")
    fun getAll(): List<Ident>

    @Transaction
    @Query("SELECT * FROM ident")
    fun getAllLive(): Flow<List<IdentAndAbout>>

    @Transaction
    @Query("SELECT * FROM ident WHERE oid = :oid LIMIT 1")
    fun findByOId(oid: String): IdentAndAbout


    @Transaction
    @Query("SELECT * FROM ident WHERE public_key = :pk LIMIT 1")
    fun findByPublicKey(pk: String): IdentAndAbout?

    @Query("SELECT * FROM ident WHERE oid = :oid LIMIT 1")
    fun findByIdLive(oid: String): LiveData<Ident>

    @Insert
    fun insert(feed: Ident): Long

    @Delete
    fun delete(feed: Ident)

    @Update
    fun update(feed: Ident)

    @Query("SELECT count(*) FROM ident")
    fun count(): Int

    @Query("SELECT count(*) FROM ident")
    fun liveCount(): LiveData<Int>

    @Transaction
    @Query("SELECT * FROM ident ORDER BY default_ident desc LIMIT 1")
    fun getDefaultFeed(): Flow<IdentAndAbout?>

    @Query("UPDATE ident set default_ident = 0 ")
    fun unsetDefault()


    @Query("UPDATE ident set default_ident = 1 where oid=:oid ")
    fun setDefault(oid: Long)

    @Query("UPDATE ident set invite = null where oid=:oid ")
    fun cleanInvite(oid: Long)

    @Transaction
    fun setFeedAsDefaultFeed(oid: Long) {
        unsetDefault()
        setDefault(oid)
    }


}

