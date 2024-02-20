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


import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import `in`.delog.R
import `in`.delog.db.model.IdentAndAbout
import `in`.delog.ui.component.BottomBarMainButton
import `in`.delog.ui.component.IdentityBox
import `in`.delog.ui.navigation.Scenes
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.IdentListViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IdentList(navController: NavHostController) {
    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
    val identListViewModel = koinViewModel<IdentListViewModel>()

    val idents: State<List<IdentAndAbout>> = identListViewModel.idents.observeAsState(listOf())

    LaunchedEffect(Unit) {
        bottomBarViewModel.setActions {}
        bottomBarViewModel.setActions {
            Spacer(modifier = Modifier.weight(1f))
            IdentListFab(navController = navController)
        }
    }


    LazyColumn {
        items(idents.value) { identAndAbout ->
            var argUri = identAndAbout.ident.oid

            Card(
                colors = CardDefaults.cardColors(),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .padding(bottom = 0.dp, top = 16.dp, start = 16.dp, end = 16.dp)
                    .wrapContentHeight()
                    .combinedClickable(
                        onLongClick = {
                            identListViewModel.setFeedAsDefaultFeed(identAndAbout.ident)
                        },
                        onClick = {
                            identListViewModel.setFeedAsDefaultFeed(identAndAbout.ident)
                            navController.navigate("${Scenes.MainFeed.route}/${argUri}")
                        },
                        onDoubleClick = {
                            navController.navigate("${Scenes.AboutEdit.route}/${argUri}")
                        }
                    )
            ) {
                IdentityBox(
                    identAndAbout = identAndAbout,
                    short = true,
                    onClick = {
                        navController.navigate("${Scenes.AboutEdit.route}/${argUri}")
                    }
                )
            }
        }
    }
}

@Composable
fun IdentListFab(navController: NavController) {
    BottomBarMainButton(
        modifier = Modifier.testTag("new_identifier"),
        onClick = {
            navController.navigate(Scenes.NewFeed.route)
        },
        text = stringResource(R.string.identifier)
    )
}
