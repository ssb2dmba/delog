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


import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import `in`.delog.MainApplication
import `in`.delog.db.model.About
import `in`.delog.db.model.Ident
import `in`.delog.db.model.IdentAndAbout
import `in`.delog.db.model.IdentAndAboutWithBlob
import `in`.delog.db.repository.IdentRepository
import `in`.delog.service.ssb.SsbService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class IdentListViewModel(
    private val repository: IdentRepository,
    private val ssbService: SsbService
) : ViewModel() {


    private var _insertedIdent: MutableStateFlow<Ident?> = MutableStateFlow(null)
    var insertedIdent: StateFlow<Ident?> = _insertedIdent.asStateFlow()
    var idents: LiveData<List<IdentAndAboutWithBlob>> = repository.idents.asLiveData()
    val default: LiveData<IdentAndAboutWithBlob?> = repository.default.asLiveData()
    val count: LiveData<Int> = repository.count

    fun insert(ident: Ident, alias: String? = null) {
        GlobalScope.launch(Dispatchers.IO) {
            // insert complete ident and about
            val about = About(
                ident.publicKey,
                name = alias ?: ident.publicKey.subSequence(0, 6).toString(),
                dirty = true
            )
            val id = repository.insert(IdentAndAbout(ident, about))
            ident.oid = id
            redeemInvite(ident)
        }
    }
    private fun redeemInvite(ident: Ident) {
        viewModelScope.launch {
            ssbService.connectWithInvite(ident,
                {
                    // everything is going according to the plan
                    MainApplication.toastify("Identity redeemed on relay successfully")
                    _insertedIdent.value = ident
                },
                {
                    MainApplication.toastify(it.message.toString())
                })
        }
    }

    fun setFeedAsDefaultFeed(ident: Ident) {
        ssbService.disconnect()
        viewModelScope.launch(Dispatchers.IO) {
            repository.setFeedAsDefaultFeed(ident)
        }
    }

}
