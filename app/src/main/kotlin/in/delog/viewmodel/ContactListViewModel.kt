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
package `in`.delog.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import `in`.delog.db.model.Contact
import `in`.delog.db.repository.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class ContactListViewModel(
    private val author: String,
    private val repository: ContactRepository
) : ViewModel() {

    var contactsPaged = Pager(
        PagingConfig(
            pageSize = 10,
            prefetchDistance = 10,
            enablePlaceholders = false,
        )
    ) {
        repository.getPagedContacts(author)
    }.flow.cachedIn(viewModelScope)

    fun insert(contact: Contact) {
        GlobalScope.launch(Dispatchers.IO) {
            val exist = repository.getByAuthorAndFollow(contact.author, contact.follow)
            if (exist == null) {
                repository.insert(contact)
            }
        }
    }

    fun remove(contact: Contact) {
        GlobalScope.launch(Dispatchers.IO) {
            repository.deleteContact(contact)
        }
    }

}
