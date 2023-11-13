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
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import `in`.delog.viewmodel.FeedMainUIState
import `in`.delog.viewmodel.MessageListViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MessagesList(navController: NavController, feedToReadKey: String) {
    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
    if (feedToReadKey == null) {
        return
    }
    val viewModel =
        koinViewModel<MessageListViewModel>(parameters = { parametersOf(feedToReadKey) })
    val uiState by viewModel.uiState.observeAsState(FeedMainUIState())


    LaunchedEffect(feedToReadKey) {
        bottomBarViewModel.setActions {
            Spacer(modifier = Modifier.weight(1f))
            NewDraftFab(navController)
        }
    }

    if (uiState.identAndAbout == null || !uiState.loaded) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val JumpToBottomThreshold = 56.dp
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val fpgMessages: Flow<PagingData<AppDatabaseView.MessageInTree>> = viewModel.messagesPaged!!
    val lazyMessageItems: LazyPagingItems<AppDatabaseView.MessageInTree> =
        fpgMessages.collectAsLazyPagingItems()
    if (uiState.syncing || uiState.identAndAbout == null) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    Box {

        var previousRoot: String? = null
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = lazyMessageItems.itemCount,
            ) { index ->
                if (lazyMessageItems.itemCount == 0) {
                    AppEmptyList()
                }

                if (index == 0) {
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
                }

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


        // Jump to bottom button shows up when user scrolls past a threshold.
        // Convert to pixels:
        val jumpThreshold = with(LocalDensity.current) {
            JumpToBottomThreshold.toPx()
        }

        // Show the button if the first visible item is not the first one or if the offset is
        // greater than the threshold.
        val jumpToBottomButtonEnabled by remember {
            derivedStateOf {
                scrollState.firstVisibleItemIndex != 0 ||
                        scrollState.firstVisibleItemScrollOffset > jumpThreshold
            }
        }
        Log.i("SCROLL", scrollState.firstVisibleItemScrollOffset.toString())
        GoToTop(
            // Only show if the scroller is not at the bottom
            enabled = jumpToBottomButtonEnabled,
            onClicked = {
                scope.launch {
                    scrollState.animateScrollToItem(0)
                }
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )


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
    MainActionButton(
        onClick = {
            navController.navigate(Scenes.DraftNew.route)
        },
        text = stringResource(id = R.string.compose)
    )
}
