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
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import `in`.delog.db.model.Ident
import `in`.delog.db.model.IdentAndAbout
import `in`.delog.repository.IdentRepository


class IdentListViewModel(private val repository: IdentRepository) : ViewModel() {

    private var _identToNavigate: MutableStateFlow<Ident?> = MutableStateFlow(null)
    var identToNavigate: StateFlow<Ident?> = _identToNavigate.asStateFlow()
    var idents: LiveData<List<IdentAndAbout>> = repository.idents.asLiveData()
    val default: LiveData<IdentAndAbout> = repository.default.asLiveData()
    val count: LiveData<Int> = repository.count

    fun insertAndNavigate(ident: Ident, navController: NavHostController) {
        GlobalScope.launch(Dispatchers.IO) {
            var id = repository.insert(ident)
            ident.oid = id
            _identToNavigate.value = ident
        }
    }

    fun reset() {
        _identToNavigate.value = null
    }

}
