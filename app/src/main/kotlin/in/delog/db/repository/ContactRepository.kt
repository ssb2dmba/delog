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

import androidx.paging.PagingSource
import `in`.delog.db.dao.ContactDao
import `in`.delog.db.model.Contact
import `in`.delog.db.model.ContactAndAbout


interface ContactRepository {
    suspend fun insert(contact: Contact)
    suspend fun deleteContact(contact: Contact)
    fun getPagedContacts(author: String): PagingSource<Int, ContactAndAbout>
    fun update(contact: Contact)
    fun getByAuthorAndFollow(author: String, follow: String): Contact?
    fun geContacts(author: String): List<Contact>
}

class ContactRepositoryImpl(private val contactDao: ContactDao) : ContactRepository {

    override suspend fun insert(contact: Contact) {
        contactDao.insert(contact)
    }

    override suspend fun deleteContact(contact: Contact) {
        contactDao.deleteContact(contact)
    }

    override fun getPagedContacts(author: String): PagingSource<Int, ContactAndAbout> {
        return contactDao.getPagedContact(author)
    }

    override fun geContacts(author: String): List<Contact> {
        return contactDao.getContacts(author)
    }

    override fun update(contact: Contact) {
        return contactDao.update(contact)
    }

    override fun getByAuthorAndFollow(author: String, follow: String): Contact? {
        return contactDao.getByAuthorAndFollow(author, follow)
    }

}
