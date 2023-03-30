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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import `in`.delog.R
import `in`.delog.db.model.Draft
import `in`.delog.ui.component.MessageItem
import `in`.delog.ui.component.toMessageViewData
import `in`.delog.ui.navigation.Scenes
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.DraftListViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun DraftList(navController: NavHostController) {
    val feed = LocalActiveFeed.current ?: return
    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
    val title = stringResource(R.string.drafts)
    LaunchedEffect(Unit) {
        bottomBarViewModel.setActions {
            Spacer(modifier = Modifier.weight(1f))
            NewDraftFab(navController = navController)
        }
        bottomBarViewModel.setTitle("%s".format(feed.ident.alias))

    }


    val draftListViewModel =
        koinViewModel<DraftListViewModel>(parameters = { parametersOf(feed.ident.publicKey) })
    val fpgDrafts: Flow<PagingData<Draft>> = draftListViewModel.draftsPaged
    val lazyMovieItems: LazyPagingItems<Draft> = fpgDrafts.collectAsLazyPagingItems()


    LazyVerticalGrid(columns = GridCells.Fixed(1)) {
        items(
            count = lazyMovieItems.itemCount,
        ) { index ->
            lazyMovieItems[index]?.let {
                MessageItem(
                    message = it.toMessageViewData(),
                    onClickCallBack = {
                        navController.navigate("${Scenes.DraftEdit.route}/${it.oid}")
                    },
                    showToolbar = false,
                    navController = navController
                )
            }
        }
    }
}
