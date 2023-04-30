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
package `in`.delog.ui.scene.identitifiers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import `in`.delog.viewmodel.BottomBarViewModel
import org.apache.tuweni.scuttlebutt.Identity
import org.koin.androidx.compose.koinViewModel

@Composable
fun IdentNew(navController: NavHostController) {
    val startUrl = "http://192.168.0.45:8000/invite/";
    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
    LaunchedEffect(Unit) {
        bottomBarViewModel.setTitle("New identity")
        bottomBarViewModel.setActions { }
    }

    var invite: String? by remember { mutableStateOf(null) }
    fun setInvite(s: String) {
        invite = s
    }

    var identity: Identity? by remember { mutableStateOf(null) }
    fun setIdentity(s: Identity?) {
        identity = s
    }


    if (identity == null) {
        LoadIdentity(navController = navController, ::setIdentity)
    } else {
        if (invite == null) {
            InviteWebRequest(startUrl, ::setInvite)
        } else {
            IdentNewEdit(navController, identity!!, invite!!)
        }
    }

}







