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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import `in`.delog.R
import `in`.delog.model.MessageViewData
import `in`.delog.service.ssb.SsbService
import `in`.delog.service.ssb.SsbUIState
import `in`.delog.ui.component.AppEmptyList
import `in`.delog.ui.component.BottomBarMainButton
import `in`.delog.ui.component.GoToTop
import `in`.delog.ui.component.IdentityBox
import `in`.delog.ui.component.ListSpacer
import `in`.delog.ui.component.MessageItem
import `in`.delog.ui.component.makeArgUri
import `in`.delog.ui.navigation.Scenes
import `in`.delog.ui.observeAsState
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.FeedMainUIState
import `in`.delog.viewmodel.MessageListViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.get
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun MessagesList(navController: NavController, feedToReadKey: String) {
    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
    val viewModel =
        koinViewModel<MessageListViewModel>(parameters = { parametersOf(feedToReadKey) })
    val uiState by viewModel.uiState.observeAsState(FeedMainUIState())
    val ssbService = get<SsbService>()
    val ssbUiState by ssbService.uiState.observeAsState(SsbUIState())

    
    LaunchedEffect(feedToReadKey) {
        bottomBarViewModel.setActions {
            Spacer(modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.onSurfaceVariant)
            )
            NewDraftFab(navController)
        }
    }

    if (uiState.identAndAbout == null ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val jumpToBottomThreshold = 56.dp
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val fpgMessages: Flow<PagingData<MessageViewData>> = viewModel.messagesPaged!!
    val lazyMessageItems: LazyPagingItems<MessageViewData> =
        fpgMessages.collectAsLazyPagingItems()
    Column {
        if (ssbUiState.syncing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        for (r in ssbUiState.blobUp.keys) {
            if (ssbUiState.blobSize[r]!=null
                && ssbUiState.blobUp[r]!!.toFloat() < ssbUiState.blobSize[r]!!.toFloat()) {
                LinearProgressIndicator(
                    progress = ssbUiState.blobUp[r]!!.toFloat() / ssbUiState.blobSize[r]!!.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        for (r in ssbUiState.blobDown.keys) {
            if (ssbUiState.blobSize[r]!=null &&
                ssbUiState.blobDown[r]!!.toFloat() < ssbUiState.blobSize[r]!!.toFloat()) {
                LinearProgressIndicator(
                    progress = ssbUiState.blobDown[r]!!.toFloat() / ssbUiState.blobSize[r]!!.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }




    Box {
        var previousRoot: String? = null
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
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
                            .padding(bottom = 8.dp)
                            .wrapContentHeight()
                    ) {
                        IdentityBox(
                            identAndAboutWithBlob = uiState.identAndAbout!!,
                            short = true,
                        )
                    }
                }

                lazyMessageItems[index]?.let {
                    if ((it.root != previousRoot)) { // // it.root == null || (it.replies == 0L) ||
                        ListSpacer()
                    }
                    val argUri = makeArgUri(it.key)
                    MessageItem(
                        navController = navController,
                        messageViewData = it,
                        showToolbar = true,
                        truncate = it.key != feedToReadKey,
                        hasDivider = it.replies > 0 || it.level > 0,
                        onClickCallBack = {
                            if (feedToReadKey != it.key) {
                                navController.navigate("${Scenes.MainFeed.route}/${argUri}")
                            }
                        }
                    )

                    previousRoot = it.root ?: it.key
                }
            }
        }


        // Jump to bottom button shows up when user scrolls past a threshold.
        // Convert to pixels:
        val jumpThreshold = with(LocalDensity.current) {
            jumpToBottomThreshold.toPx()
        }

        // Show the button if the first visible item is not the first one or if the offset is
        // greater than the threshold.
        val jumpToBottomButtonEnabled by remember {
            derivedStateOf {
                scrollState.firstVisibleItemIndex != 0 ||
                        scrollState.firstVisibleItemScrollOffset > jumpThreshold
            }
        }
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

    if (ssbUiState.error != null && ssbUiState.error?.message != null) {
        val context = LocalContext.current
        Toast
            .makeText(
                context,
                String.format(ssbUiState.error!!.message!!),
                Toast.LENGTH_LONG
            )
            .show()
        viewModel.clearError()

    }
}

@Composable
fun NewDraftFab(navController: NavController) {
    BottomBarMainButton(
        onClick = {
            navController.navigate(Scenes.DraftNew.route + "/post")
        },
        text = stringResource(id = R.string.compose)
    )
}
