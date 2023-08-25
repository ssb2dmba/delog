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
package `in`.delog.ui.scene

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import `in`.delog.ssb.SsbService
import `in`.delog.viewmodel.IdentAndAboutViewModel
import org.koin.androidx.compose.get
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun FeedInit(navController: NavController, id: String) {
    val vm = koinViewModel<IdentAndAboutViewModel>(parameters = { parametersOf(id) })
    LaunchedEffect(id) {
        id?.let { vm.setCurrentIdentByPk(it) }
    }
    if (vm.identAndAbout == null) {
        return
    }
    val ssbService: SsbService = get()
    vm.identAndAbout!!.ident!!.invite?.let { vm.connectWithInvite(vm.identAndAbout!!.ident!!, it, ssbService) }
    Text(text = "feed init done")
    // TODO follow up on redirect
}




