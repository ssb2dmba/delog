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


import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import `in`.delog.R
import `in`.delog.db.model.About
import `in`.delog.db.model.IdentAndAbout
import `in`.delog.ui.component.IdentityBox
import `in`.delog.ui.navigation.Scenes
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.IdentListViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun IdentList(navController: NavHostController) {
    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
    val identListViewModel = koinViewModel<IdentListViewModel>()

    val idents: State<List<IdentAndAbout>> = identListViewModel.idents.observeAsState(listOf())
    var strIdents = stringResource(R.string.identifiers)

    LaunchedEffect(Unit) {
        bottomBarViewModel.setActions {}
        bottomBarViewModel.setTitle(strIdents)
        bottomBarViewModel.setActions {
            Spacer(modifier = Modifier.weight(1f))
            IdentListFab(navController = navController)
        }
    }

    LazyColumn(
    ) {
        items(idents.value) {

                identAndAbout ->
            IdentityBox(
                about = if (identAndAbout.about != null) identAndAbout.about!! else About(
                    about = identAndAbout.ident.publicKey
                ),
                short = true,
                navController = navController,
                mine = true,
                defaultIdent = identAndAbout.ident.defaultIdent
            )
        }

    }
}


@Composable
fun IdentListFab(navController: NavController) {
    ExtendedFloatingActionButton(
        modifier = Modifier.testTag("new_identifier"),
        onClick = {
            navController.navigate(Scenes.NewFeed.route)
        },
        icon = { Icon(Icons.Filled.Add, "", tint = MaterialTheme.colorScheme.onPrimary) },
        text = { Text(text = stringResource(R.string.identifier)) }
    )
}
