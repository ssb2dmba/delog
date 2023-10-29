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


import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import `in`.delog.R
import `in`.delog.db.AppDatabaseView
import `in`.delog.db.toMessageViewData
import `in`.delog.ssb.SsbService
import `in`.delog.ui.component.*
import `in`.delog.ui.navigation.Scenes
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.MessageListViewModel
import kotlinx.coroutines.flow.Flow
import org.koin.androidx.compose.get
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

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
    val viewModel =
        koinViewModel<MessageListViewModel>(parameters = { parametersOf(feedToReadKey) })
    if (viewModel.identAndAbout ==null) {
        return
    }
    val context = LocalContext.current
    val ssbService: SsbService = get()
    LaunchedEffect(feedToReadKey) {
        try {
            ssbService.reconnect(viewModel.identAndAbout!!.ident)
        } catch (e: Exception) {
            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    if (viewModel.messagesPaged == null) return
    val fpgMessages: Flow<PagingData<AppDatabaseView.MessageInTree>> = viewModel.messagesPaged!!
    val lazyMessageItems: LazyPagingItems<AppDatabaseView.MessageInTree> =
        fpgMessages.collectAsLazyPagingItems()
    Column {
        IdentityBox(
            identAndAbout = viewModel.identAndAbout!!,
            short = true,
        )
        if (lazyMessageItems.itemCount == 0) {
            AppEmptyList()
        } else {
            val firstMsgInList = lazyMessageItems[0]!!
            if (firstMsgInList.level > 0) {

                Text("In reply to "
                        + (firstMsgInList.pName ?: firstMsgInList.pauthor.subSequence(0, 6)
                        ),
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable {
                            val argUri =
                                makeArgUri(firstMsgInList.parents.split('-')[(firstMsgInList.level - 1).toInt()])
                            navController.navigate("${Scenes.MainFeed.route}/${argUri}")
                        }
                )
            }
        }
        var previousRoot: String? = null
        LazyVerticalGrid(columns = GridCells.Fixed(1)) {
            items(
                count = lazyMessageItems.itemCount,
            ) { index ->
                lazyMessageItems[index]?.let {
                    val argUri = makeArgUri(it.key)
                    MessageItem(
                        navController = navController,
                        message = it.toMessageViewData(),
                        showToolbar = true,
                        truncate = it.key != feedToReadKey,
                        hasDivider = it.replies > 0 || it.level > 0,
                        onClickCallBack = {
                            if (feedToReadKey != it.key) {
                                navController.navigate("${Scenes.MainFeed.route}/${argUri}")
                            }
                        }
                    )
                    if (it.root == null || it.root != previousRoot) {
                        ListSpacer()
                    }
                    previousRoot = it.root ?: it.key

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
