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
import `in`.delog.db.model.Ident
import `in`.delog.db.model.IdentAndAbout
import `in`.delog.repository.IdentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class IdentListViewModel(private val repository: IdentRepository) : ViewModel() {

    private var _insertedIdent: MutableStateFlow<Ident?> = MutableStateFlow(null)
    var insertedIdent: StateFlow<Ident?> = _insertedIdent.asStateFlow()
    var idents: LiveData<List<IdentAndAbout>> = repository.idents.asLiveData()
    val default: LiveData<IdentAndAbout> = repository.default.asLiveData()
    val count: LiveData<Int> = repository.count

    fun insert(ident: Ident) {
        GlobalScope.launch(Dispatchers.IO) {
            var id = repository.insert(ident)
            ident.oid = id
            _insertedIdent.value = ident
        }
    }

    fun reset() {
        _insertedIdent.value = null
    }

}
