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

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BottomBarViewModel() : ViewModel() {

    private val _title = MutableLiveData<String>("")
    val title: LiveData<String> = _title
    fun setTitle(newTitle: String) {
        _title.value = newTitle
    }

    val dummy: (@Composable RowScope.() -> Unit)? = { }
    var _actions = MutableLiveData(dummy)
    val actions: MutableLiveData<@Composable() (RowScope.() -> Unit)?> get() = _actions

    fun setActions(newAction: @Composable() (RowScope.() -> Unit)) {
        _actions.value = newAction
    }


}
