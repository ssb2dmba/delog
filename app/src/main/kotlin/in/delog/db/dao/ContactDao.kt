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

import androidx.paging.PagingSource
import androidx.room.*
import `in`.delog.db.model.Contact
import `in`.delog.db.model.ContactAndAbout

@Dao
interface ContactDao {

    @Query("SELECT * FROM contact WHERE author = :author order by oid desc")
    fun getPagedContact(author: String): PagingSource<Int, ContactAndAbout>

    @Delete
    fun deleteContact(contact: Contact)

    @Insert
    fun insert(contact: Contact)

    @Query("SELECT * FROM contact WHERE author = :author and follow = :follow")
    fun getByAuthorAndFollow(author: String, follow: String): Contact?

    @Update
    fun update(contact: Contact)

    @Query("SELECT * FROM contact WHERE author = :author")
    fun getContacts(author: String): List<Contact>

}
