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

import androidx.room.Delete
import androidx.room.Insert
import `in`.delog.db.dao.RelayDao
import `in`.delog.db.model.RelayServer


interface RelayRepository {
    suspend fun insert(relayServer: RelayServer)
    suspend fun delete(relayServer: RelayServer)
    fun insertOrUpdate(relayServer: RelayServer)

    suspend fun getByOid(oid: Long): RelayServer?
    suspend fun getByUrl(server: String): RelayServer?

}

class RelayRepositoryImpl(private val relayDao: RelayDao) : RelayRepository {

    @Insert
    override suspend fun insert(relayServer: RelayServer) {
        relayDao.insert(relayServer)
    }

    @Delete
    override suspend fun delete(relayServer: RelayServer) {
        relayDao.delete(relayServer)
    }

    override fun insertOrUpdate(relayServer: RelayServer) {
        TODO("Not yet implemented")
    }

    override suspend fun getByOid(oid: Long): RelayServer? {
        return relayDao.getByOid(oid)
    }

    override suspend fun getByUrl(server: String): RelayServer? {
        return relayDao.getByUrl(server)
    }


}
