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


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import `in`.delog.db.model.Contact
import `in`.delog.db.model.RelayServer
import `in`.delog.db.repository.ContactRepository
import `in`.delog.db.repository.RelayRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ContactListViewModel(
    private val author: String,
    private val contactRepository: ContactRepository,
    private val relayRepository: RelayRepository,
) : ViewModel() {

    var contactsPaged = Pager(
        PagingConfig(
            pageSize = 10,
            prefetchDistance = 10,
            enablePlaceholders = false,
        )
    ) {
        contactRepository.getPagedContacts(author)
    }.flow.cachedIn(viewModelScope)

    fun insert(author: String, strContact: String) {
        viewModelScope.launch(Dispatchers.IO){
            val publicKey=strContact.split("@")[1]
            val serverUrl=strContact.split("@").last()
            val exist = contactRepository.getByAuthorAndFollow(author, publicKey)
            if (exist!=null) {
                Log.d("ContactListViewModel", "Contact already exists")
                return@launch
            }
            var relay = relayRepository.getByUrl(serverUrl)
            if (relay==null) {
                relay = RelayServer(0, serverUrl)
                relayRepository.insert(relay)
                relay = relayRepository.getByUrl(serverUrl)
            }
            val contact = Contact(0, author, publicKey, true, relay!!.oid)
            contactRepository.insert(contact)
        }
    }

    fun remove(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO){
            contactRepository.deleteContact(contact)
        }
    }

}
