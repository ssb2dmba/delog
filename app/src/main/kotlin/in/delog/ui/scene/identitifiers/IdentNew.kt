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


import android.util.Log
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import `in`.delog.db.model.Ident
import `in`.delog.ui.component.makeArgUri
import `in`.delog.ui.navigation.Scenes
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.IdentListViewModel
import org.apache.tuweni.scuttlebutt.Identity
import org.koin.androidx.compose.koinViewModel

@Composable
fun IdentNew(navController: NavHostController) {

    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()

    LaunchedEffect(Unit) {
        bottomBarViewModel.setTitle("New identity")
        bottomBarViewModel.setActions { }
    }

    var inviteUrl: String? by remember { mutableStateOf(null) }
    var invite: String? by remember { mutableStateOf(null) }
    var identity: Identity? by remember { mutableStateOf(null) }

    val identListViewModel = koinViewModel<IdentListViewModel>()
    var hasNavigated by remember { mutableStateOf(false) }

    fun setInvite(s: String) {
        invite = s
        Log.i("invite", "invite set to " + s)
    }

    fun setIdentity(pIdentity: Identity?, pInviteUrl: String?) {
        identity = pIdentity
        inviteUrl = pInviteUrl
    }

    fun doneWithoutInvite() {
        if (hasNavigated == true) return
        hasNavigated = true
        val ident = Ident(
            0,
            identity!!.toCanonicalForm(),
            "",
            8008,
            identity!!.privateKeyAsBase64String(),
            false,
            -1,
            null
        );
        val exists = identListViewModel.idents.value!!.any { it.ident.publicKey == ident.publicKey }
        if (exists) {
            val argUri = makeArgUri(ident.publicKey)
            navController.navigate("${Scenes.MainFeed.route}/${argUri}")
        } else {
            identListViewModel.insert(ident = ident)
            navController.navigate("${Scenes.FeedList.route}")
        }
    }

    if (identity == null) {
        LoadIdentity(identListViewModel, ::setIdentity)
    } else {
        if (invite == null) {
            if (inviteUrl != null) {
                InviteWebRequest(inviteUrl!!, ::setInvite)
            } else {
                doneWithoutInvite();
            }
        } else {
            IdentNewEdit(navController, identity!!, invite!!)
        }
    }
}
