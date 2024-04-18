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


import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import `in`.delog.MainApplication
import `in`.delog.db.SettingStore
import `in`.delog.db.SettingStore.Companion.SERVER_URL
import `in`.delog.db.model.Ident
import `in`.delog.ui.component.makeArgUri
import `in`.delog.ui.navigation.Scenes
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.IdentListViewModel
import org.apache.tuweni.scuttlebutt.Identity
import org.apache.tuweni.scuttlebutt.Invite
import org.apache.tuweni.scuttlebutt.MalformedInviteCodeException
import org.koin.androidx.compose.koinViewModel

@Composable
fun IdentNew(navController: NavHostController) {

    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
    val identListViewModel = koinViewModel<IdentListViewModel>()
    bottomBarViewModel.setActions { }
    var inviteUrl: String? by remember { mutableStateOf(null) }
    var invite: String? by remember { mutableStateOf(null) }
    var identity: Identity? by remember { mutableStateOf(null) }
    val context = LocalContext.current
    val store = SettingStore(context)
    val serverUrl = store.getData(SERVER_URL).collectAsState(initial = null)
    if (serverUrl.value == null) return

    fun setInvite(s: String) {
        try {
            Invite.fromCanonicalForm(s)
        } catch (e: MalformedInviteCodeException) {
            Toast.makeText(context, "invite is malformed !", Toast.LENGTH_LONG).show()
            return
        }
        invite = s
    }

    fun setIdentity(pIdentity: Identity?, pInviteUrl: String?) {
        // check if exists ...
        if (pIdentity!=null) {
            val pk = pIdentity.toCanonicalForm()
            if (identListViewModel.idents.value!!.any { it.ident.publicKey == pk }) {
                val preexist = identListViewModel.idents.value!!.first { it.ident.publicKey == pk }
                MainApplication.toastify("This identity $pk already exists !")
                identListViewModel.setFeedAsDefaultFeed(preexist.ident)
            }
        }
        identity = pIdentity
        inviteUrl = pInviteUrl
    }


    if (identity == null) {
        LoadIdentity(serverUrl.value!!, ::setIdentity)
    } else {
        if (invite == null) {
            if (inviteUrl != null) {
                InviteWebRequest(inviteUrl!!, ::setInvite)
                return
            }
        }
        IdentNewEdit(navController, identity!!, invite)
    }
}
