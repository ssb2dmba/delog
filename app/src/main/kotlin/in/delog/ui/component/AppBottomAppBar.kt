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
package `in`.delog.ui.component


import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import `in`.delog.viewmodel.BottomBarViewModel
import org.koin.androidx.compose.koinViewModel


@Composable
fun AppBottomAppBar(onNavIconClick: () -> Job) {

    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
    val actions: (@Composable RowScope.() -> Unit)? by bottomBarViewModel.actions.observeAsState(
        null
    )

    BottomAppBar(
        contentPadding = PaddingValues(start = 24.dp, end = 8.dp),
        content = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedIconButton(
                        modifier = Modifier.testTag("openDrawer"),
                        onClick = { onNavIconClick() }
                    ) {
                        Icon(Icons.Outlined.Menu, contentDescription = "menu")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    actions?.let { it() }
                }
            }
        })
}
