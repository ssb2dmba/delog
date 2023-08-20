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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import `in`.delog.R
import `in`.delog.db.model.*
import `in`.delog.ui.LocalActiveFeed
import `in`.delog.ui.component.EditDialog
import `in`.delog.ui.component.IdentityBox
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.ContactListViewModel
import kotlinx.coroutines.flow.Flow
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun ContactList(navController: NavController) {
    val feed = LocalActiveFeed.current ?: return

    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
    val bottomBarTitle = stringResource(id = R.string.contacts)
    var showAddContactDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        bottomBarViewModel.setActions {}
        bottomBarViewModel.setTitle(bottomBarTitle)
        bottomBarViewModel.setActions {
            Spacer(modifier = Modifier.weight(1f))
            ContactListFab({ showAddContactDialog = true })
        }
    }

    val contactListViewModel =
        koinViewModel<ContactListViewModel>(parameters = { parametersOf(feed.ident.publicKey) })
    val fpgDrafts: Flow<PagingData<ContactAndAbout>> = contactListViewModel.contactsPaged
    val lazyContactItems: LazyPagingItems<ContactAndAbout> = fpgDrafts.collectAsLazyPagingItems()

    if (showAddContactDialog) {

        fun addContact(strContact: String) {
            var contact = Contact(0, feed.ident.publicKey, strContact, true)
            contactListViewModel.insert(contact)
            showAddContactDialog = false
        }
        EditDialog(
            title = R.string.follow,
            value = "",
            closeDialog = { showAddContactDialog = false },
            setValue = ::addContact
        )
    }

    LazyVerticalGrid(columns = GridCells.Fixed(1)) {
        items(
            count = lazyContactItems.itemCount,
        ) { index ->
            lazyContactItems[index]?.let {
                ContactListItem(contactAndAbout = it, contactListViewModel)
            }
        }
    }
}

@Composable
fun ContactListItem(
    contactAndAbout: ContactAndAbout,
    contactListViewModel: ContactListViewModel,
) {
    if (contactAndAbout.about == null) contactAndAbout.about = About(about = contactAndAbout.contact.follow)
    Box(modifier = Modifier.fillMaxWidth()) {

        val identAndAbout = IdentAndAbout(ident = Ident(
            -1,
            publicKey = contactAndAbout.about!!.about,
            "",
            -1,
            "",
            false,
            -1,
            "",
        ),
            about = contactAndAbout.about
        )
        IdentityBox(identAndAbout = identAndAbout)
        Button(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            onClick = {
                contactListViewModel.remove(contactAndAbout.contact)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        ) {
            Text(stringResource(id = R.string.unfollow))
        }
    }
}

@Composable
fun ContactListFab(callback: () -> Unit) {
    ExtendedFloatingActionButton(
        modifier = Modifier.testTag("new_contact"),
        onClick = {
            callback()
        },
        icon = { Icon(Icons.Filled.Add, "", tint = MaterialTheme.colorScheme.onPrimary) },
        text = { Text(text = stringResource(R.string.follow)) }
    )
}
