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

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.internal.isLiveLiteralsEnabled
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import `in`.delog.db.model.About
import `in`.delog.db.model.ContactAndAbout
import `in`.delog.db.model.IdentAndAbout
import `in`.delog.db.model.IdentAndAboutWithBlob
import `in`.delog.service.ssb.SsbService.Companion.TAG
import `in`.delog.ui.CameraQrCodeScanner
import `in`.delog.ui.LocalActiveFeed
import `in`.delog.ui.component.BottomBarMainButton
import `in`.delog.ui.component.IdentityBox
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.ContactListViewModel
import kotlinx.coroutines.flow.Flow
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun ContactList(navController: NavController) {


    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
    var showAddContactDialog by remember { mutableStateOf(false) }

    bottomBarViewModel.setActions {}
    bottomBarViewModel.setActions {
        Spacer(modifier = Modifier.weight(1f))
        ContactListFab { showAddContactDialog = true }
    }

    val feed = LocalActiveFeed.current ?: return
    val contactListViewModel =
        koinViewModel<ContactListViewModel>(parameters = { parametersOf(feed.ident.publicKey) })
    val fpgDrafts: Flow<PagingData<ContactAndAbout>> = contactListViewModel.contactsPaged
    val lazyContactItems: LazyPagingItems<ContactAndAbout> = fpgDrafts.collectAsLazyPagingItems()
    var showCameraScanner: Boolean by remember { mutableStateOf(false) }
    var publicKey by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(true) }
    fun validate(text: String) {
        val regexp="^@[a-zA-Z0-9/+]+=.ed25519@+[a-zA-Z0-9.+]*\$".toRegex()
        isError = !regexp.matches(text)
    }
    if (showCameraScanner) {
        CameraQrCodeScanner {
            validate(it)
            publicKey = it;
            showCameraScanner = false
        }
        return
    }


    if (showAddContactDialog) {

        fun addContact(strContact: String) {
            try {
                contactListViewModel.insert(feed.ident.publicKey, strContact)
            } catch (e: Exception) {
                Log.e(TAG, "Error adding contact", e)
                // TODO toast error
                return
            }
            showAddContactDialog = false
            publicKey = ""
            isError = false
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
                            onClick = {
                                showCameraScanner = true
                            }
                        ) {
                            Icon(
                                Icons.Filled.PhotoCamera,
                                contentDescription = "Scan",
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(autoCorrect = false)
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

    LazyColumn {
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
    var showConfirmRemoveDialog by remember { mutableStateOf(false) }
    if (showConfirmRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmRemoveDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    color = MaterialTheme.colorScheme.onSurface,
                    text = stringResource(id = R.string.confirm_delete),
                    style = MaterialTheme.typography.titleSmall
                )
            },
            text = {
                Text("Remove contact from your contact list")
            },
            dismissButton = {
                Button(
                    onClick = {
                        showConfirmRemoveDialog = false
                    }) {
                    Text(stringResource(id = R.string.dismiss))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        contactListViewModel.remove(contactAndAbout.contact)
                        showConfirmRemoveDialog = false
                    }) {
                    Text(stringResource(id = R.string.confirm_delete))
                }
            }
        )
    }
    Box(modifier = Modifier.fillMaxWidth()) {

        val identAndAbout = IdentAndAboutWithBlob(
            ident = IdentAndAbout.empty(contactAndAbout.about!!.about),
            about = contactAndAbout.about!!,
            profileImage = null // TODO
        )
        IdentityBox(identAndAboutWithBlob = identAndAbout)
        IconButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp),
            onClick = {
                showConfirmRemoveDialog = true
            }
        ) {
            Icon(
                Icons.Filled.RemoveCircle,
                contentDescription = stringResource(id = R.string.unfollow),
                modifier = Modifier.width(ButtonDefaults.MinWidth)
            )
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
