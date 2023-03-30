package org.dlog.scene

import LocalActiveFeed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import org.dlog.R
import org.dlog.db.model.Draft
import org.dlog.ui.component.MessageItem
import org.dlog.ui.component.toMessageViewData
import org.dlog.ui.navigation.Scenes
import org.dlog.viewmodel.DraftListViewModel
import org.dlog.viewmodel.TopBarViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun DraftList(navController: NavController) {
    val feed = LocalActiveFeed.current ?: return
    val topBarViewModel= koinViewModel<TopBarViewModel>()
    val title = stringResource( R.string.drafts)
    LaunchedEffect(Unit) {
        topBarViewModel.setActions {  }
        topBarViewModel.setTitle("%s %s ".format(title ,feed.alias))
    }


    val draftListViewModel =
        koinViewModel<DraftListViewModel>(parameters = { parametersOf(feed.publicKey) })
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
