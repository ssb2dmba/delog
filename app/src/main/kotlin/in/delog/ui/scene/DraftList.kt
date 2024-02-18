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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import `in`.delog.ui.LocalActiveFeed
import `in`.delog.ui.component.AppEmptyList
import `in`.delog.ui.component.IdentityBox
import `in`.delog.ui.component.ListSpacer
import `in`.delog.ui.component.MessageItem
import `in`.delog.ui.component.MessageViewData
import `in`.delog.ui.navigation.Scenes
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.DraftListViewModel
import kotlinx.coroutines.flow.Flow
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun DraftList(navController: NavHostController) {
    val identAndAbout = LocalActiveFeed.current ?: return
    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
    LaunchedEffect(Unit) {
        bottomBarViewModel.setActions {
            Spacer(modifier = Modifier.weight(1f))
            NewDraftFab(navController = navController)
        }
    }

    val draftListViewModel =
        koinViewModel<DraftListViewModel>(parameters = { parametersOf(identAndAbout.ident.publicKey) })
    val fpgDrafts: Flow<PagingData<MessageViewData>>? = draftListViewModel.messageViewData
    val lazyMessageItems: LazyPagingItems<MessageViewData> = fpgDrafts?.collectAsLazyPagingItems()
        ?: return
    Column {
        IdentityBox(identAndAbout = identAndAbout)
        if (lazyMessageItems.itemCount == 0) {
            AppEmptyList()
        }
        LazyVerticalGrid(columns = GridCells.Fixed(1)) {
            items(
                count = lazyMessageItems.itemCount,
            ) { index ->
                lazyMessageItems[index]?.let {
                    Card(
                        colors = CardDefaults.cardColors(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(bottom = 0.dp, top = 16.dp, start = 16.dp, end = 16.dp)
                            .wrapContentHeight()
                            .clickable {
                                navController.navigate("${Scenes.DraftEdit.route}/${it.oid}")
                            }
                    ) {
                        MessageItem(
                            navController = navController,
                            messageViewData = it,
                            showToolbar = false,
                            truncate = true,
                            onClickCallBack = {
                                navController.navigate("${Scenes.DraftEdit.route}/${it.oid}")
                            }
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        ListSpacer()
                    }
                }
            }
        }
    }
}
