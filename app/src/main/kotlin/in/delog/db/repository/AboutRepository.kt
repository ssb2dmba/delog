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

import androidx.room.Transaction
import `in`.delog.db.dao.AboutDao
import `in`.delog.db.model.About


interface AboutRepository {
    suspend fun insert(about: About)
    suspend fun delete(about: About)
    fun insertOrUpdate(about: About)
}

class AboutRepositoryImpl(private val aboutDao: AboutDao) : AboutRepository {

    override suspend fun insert(about: About) {
        aboutDao.insert(about)
    }

    override suspend fun delete(about: About) {
        aboutDao.delete(about)
    }

    @Transaction
    override fun insertOrUpdate(about: About) {
        val existing: About? = aboutDao.getByAuthor(about.about)
        return if (existing != null) {
            if (about.image != null) {
                existing.image = about.image
            }
            if (about.name != null) {
                existing.name = about.name
            }
            if (about.description != null) {
                existing.description = about.description
            }
            aboutDao.update(about)
        } else {
            aboutDao.insert(about)
        }
    }

}
