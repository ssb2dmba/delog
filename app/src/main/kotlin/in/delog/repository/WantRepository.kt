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


import `in`.delog.db.dao.BlobDao
import `in`.delog.db.model.Blob


interface WantRepository {
    suspend fun insert(blob: Blob)
    suspend fun delete(blob: Blob)

}
class WantRepositoryImpl(private val wantDao: BlobDao) : WantRepository {

    override suspend fun insert(blob: Blob) {
        wantDao.insert(blob)
    }

    override suspend fun delete(blob: Blob){
        wantDao.delete(blob)
    }

}
