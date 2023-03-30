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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import `in`.delog.R
import `in`.delog.db.model.Draft
import `in`.delog.ui.navigation.Scenes
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.DraftViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftNew(navController: NavHostController) {
    val feed = LocalActiveFeed.current ?: return
    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
    val draftViewModel = koinViewModel<DraftViewModel>(parameters = { parametersOf(feed.ident) })
    draftViewModel.setDirty(true)

    LaunchedEffect(draftViewModel.inserted) {
        if (draftViewModel.inserted != null) {
            navController.navigate("${Scenes.DraftEdit.route}/${draftViewModel.inserted}")
        }
    }

    var contentAsText by remember { mutableStateOf("") }
    val title = stringResource(id = R.string.drafts)
    LaunchedEffect(Unit) {
        bottomBarViewModel.setTitle(title)
        bottomBarViewModel.setActions {
            IconButton(
                modifier = Modifier
                    .height(56.dp)
                    .padding(start = 24.dp),
                onClick = {
                    navController.popBackStack();
                }) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = "cancel",
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            SaveDraftFab(onClick = {
                val draft = Draft(
                    0,
                    feed.ident.publicKey,
                    System.currentTimeMillis(),
                    contentAsText
                )
                draftViewModel.insert(draft = draft)
            })
        }
    }


    Card(
        elevation = CardDefaults.cardElevation(),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp, 8.dp)
                .fillMaxSize()
        ) {
            TextField(
                value = contentAsText,
                onValueChange = {
                    contentAsText = it
                },
                modifier = Modifier
                    .weight(1F)
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))


        }
    }
}

@Composable
fun SaveDraftFab(onClick: () -> Unit) {
    ExtendedFloatingActionButton(onClick = onClick,
        icon = {
            Icon(
                Icons.Filled.Save,
                "",
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        },
        text = { Text(stringResource(id = R.string.save)) })
}

