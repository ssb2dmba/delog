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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
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
import `in`.delog.ui.component.IdentityBox
import `in`.delog.ui.component.BottomBarMainButton
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.ContactListViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun ContactList(navController: NavController) {
    val feed = LocalActiveFeed.current ?: return

    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
    var showAddContactDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        bottomBarViewModel.setActions {}
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
        var publicKey by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }
        fun validate(text: String) {
            isError = publicKey.length > 5
        }
        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    color = MaterialTheme.colorScheme.onSurface,
                    text = stringResource(id = R.string.follow),
                    style = MaterialTheme.typography.titleSmall
                )
            },
            text = {
                        TextField(
                            label = { Text("Add contact using an ssb identifier") },
                            value = publicKey,
                            onValueChange = {
                                validate(it)
                                publicKey = it
                            },
                            isError = isError,
                            trailingIcon = {
                                IconButton(
                                    onClick = {}
                                ) {
                                    Icon(
                                        Icons.Filled.PhotoCamera,
                                        contentDescription = "Scan",
                                        modifier = Modifier.size(ButtonDefaults.IconSize)
                                    )
                                }
                            }
                        )
            },
            dismissButton = {
                Button(
                    onClick = {
                        showAddContactDialog = false
                    }) {
                    Text(stringResource(id = R.string.dismiss))
                }
            },
            confirmButton = {
                Button(
                    enabled = !isError,
                    onClick = {
                        addContact(publicKey)
                    }) {
                    Text(stringResource(id = R.string.follow))
                }
            }
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
    if (contactAndAbout.about == null) contactAndAbout.about =
        About(about = contactAndAbout.contact.follow)
    Box(modifier = Modifier.fillMaxWidth()) {

        val identAndAbout = IdentAndAbout(
            ident = IdentAndAbout.empty(contactAndAbout.about!!.about),
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

    BottomBarMainButton(
        modifier = Modifier.testTag("new_contact"),
        onClick = callback,
        text = stringResource(R.string.follow)
    )

}
