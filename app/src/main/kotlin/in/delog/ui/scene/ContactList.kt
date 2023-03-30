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

import `in`.delog.ui.LocalActiveFeed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import `in`.delog.db.model.About
import `in`.delog.db.model.ContactAndAbout
import `in`.delog.ui.component.IdentityBox
import `in`.delog.viewmodel.ContactListViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun ContactList(navController: NavController) {
    val feed = LocalActiveFeed.current ?: return
    val contactListViewModel =
        koinViewModel<ContactListViewModel>(parameters = { parametersOf(feed.ident.publicKey) })
    val fpgDrafts: Flow<PagingData<ContactAndAbout>> = contactListViewModel.contactsPaged
    val lazyContactItems: LazyPagingItems<ContactAndAbout> = fpgDrafts.collectAsLazyPagingItems()
    LazyVerticalGrid(columns = GridCells.Fixed(1)) {
        items(
            count = lazyContactItems.itemCount,
        ) { index ->
            lazyContactItems[index]?.let {
                if (it.about == null) it.about = About(about = it.contact.follow)
                ContactListItem(about = it.about!!, navController)
            }
        }
    }
}


@Composable
fun ContactListItem(about: About, navController: NavController) {

    IdentityBox(about = about, navController = navController, mine = false)

}

