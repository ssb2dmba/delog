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


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import `in`.delog.R
import `in`.delog.db.model.About
import `in`.delog.ssb.SsbService
import `in`.delog.ui.component.IdentityBox
import `in`.delog.ui.component.MessageItem
import `in`.delog.ui.component.MessageViewData
import `in`.delog.ui.navigation.Scenes
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.MessageListViewModel
import kotlinx.coroutines.flow.Flow
import org.koin.androidx.compose.get
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.net.URLEncoder
import java.nio.charset.Charset

@Composable
fun FeedMain(navController: NavController, feedToReadKey: String? = null) {
    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
    if (feedToReadKey == null) {
        return
    }



    LaunchedEffect(feedToReadKey) {
        bottomBarViewModel.setActions {
            Spacer(modifier = Modifier.weight(1f))
            NewDraftFab(navController)
        }
        bottomBarViewModel.setTitle("main")
    }
    System.out.println(navController.currentBackStackEntry?.arguments?.getString("id"))
    var id = navController.currentBackStackEntry?.arguments?.getString("id") ?: feedToReadKey

    val viewModel = koinViewModel<MessageListViewModel>(parameters = { parametersOf(id) })
    System.out.println(" newviewModel. " + id)
    if (viewModel.identAndAbout == null) return

    val ssbService: SsbService = get()
    LaunchedEffect(feedToReadKey) {
        try {
            ssbService.reconnect(viewModel.identAndAbout!!.ident)
        } catch (e: Exception) {
            // TODO snackbar
            e.printStackTrace()
        }
    }
    if (viewModel.messagesPaged==null) return
    val fpgMessages: Flow<PagingData<MessageViewData>> = viewModel.messagesPaged!!
    val lazyMessageItems: LazyPagingItems<MessageViewData> = fpgMessages.collectAsLazyPagingItems()
    Column {
        LazyVerticalGrid(columns = GridCells.Fixed(1)) {
            item {
                IdentityBox(
                    about = viewModel.identAndAbout?.about ?: About("error", "error", "error"),
                    navController = navController,
                    short = true,
                    mine = false
                )
            }
            items(
                count = lazyMessageItems.itemCount,
            ) { index ->
                lazyMessageItems[index]?.let {
                    val argUri = URLEncoder.encode(it.key, Charset.defaultCharset().toString())
                    MessageItem(
                        navController = navController,
                        message = it,
                        showToolbar = false,
                        expand = false,
                        onClickCallBack = {
                            System.out.println("navigate:" + "${Scenes.MainFeed.route}/${argUri} ${it.key}" )
                            navController.navigate("${Scenes.MainFeed.route}/${argUri}")
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun NewDraftFab(navController: NavController) {

    ExtendedFloatingActionButton(
        onClick = {
            navController.navigate(Scenes.DraftNew.route)
        },
        icon = {
            Icon(
                Icons.Filled.Draw,
                "",
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        },
        text = { Text(text = stringResource(id = R.string.compose)) }
    )
}
