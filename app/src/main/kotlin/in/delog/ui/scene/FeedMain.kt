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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.modifier.modifierLocalConsumer
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
import `in`.delog.ui.component.*
import `in`.delog.ui.navigation.Scenes
import `in`.delog.ui.observeAsState
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.MessageListState
import `in`.delog.viewmodel.MessageListViewModel
import kotlinx.coroutines.flow.Flow
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FeedMain(navController: NavController, feedToReadKey: String) {
    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
    if (feedToReadKey == null) {
        return
    }
    val viewModel =
        koinViewModel<MessageListViewModel>(parameters = { parametersOf(feedToReadKey) })
    val uiState by viewModel.uiState.observeAsState(MessageListState())


    LaunchedEffect(feedToReadKey) {
        bottomBarViewModel.setActions {
            Spacer(modifier = Modifier.weight(1f))
            NewDraftFab(navController)
        }
        bottomBarViewModel.setTitle("main")
    }

    if (uiState.identAndAbout == null && !uiState.loaded) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val fpgMessages: Flow<PagingData<AppDatabaseView.MessageInTree>> = viewModel.messagesPaged!!
    val lazyMessageItems: LazyPagingItems<AppDatabaseView.MessageInTree> =
        fpgMessages.collectAsLazyPagingItems()
    if (uiState.syncing) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    Column {
        Card(
            colors = CardDefaults.cardColors(),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .padding(16.dp)
                .wrapContentHeight()
        ) {
            IdentityBox(
                identAndAbout = uiState.identAndAbout!!,
                short = true,
            )
        }

        if (lazyMessageItems.itemCount == 0) {
            AppEmptyList()
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

    if (uiState.error!= null && uiState.error?.message!=null) {
        val context = LocalContext.current
        Toast
            .makeText(
                context,
                String.format(uiState.error!!.message!!),
                Toast.LENGTH_LONG
            )
            .show()
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
