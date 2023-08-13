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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import `in`.delog.R
import `in`.delog.db.model.Draft
import `in`.delog.db.model.MessageAndAbout
import `in`.delog.ui.LocalActiveFeed
import `in`.delog.ui.component.MessageItem
import `in`.delog.ui.component.toMessageViewData
import `in`.delog.ui.navigation.Scenes
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.DraftViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftNew(
    navController: NavHostController,
    linkedKey: String? = null,
    draftMode: String? = null
) {
    val feed = LocalActiveFeed.current ?: return
    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
    val draftViewModel = koinViewModel<DraftViewModel>(parameters = { parametersOf(feed.ident) })
    val focusRequester = remember { FocusRequester() }
    var link: MessageAndAbout? by remember {
        mutableStateOf(null)
    }
    draftViewModel.setDirty(true)

    var contentAsText by remember { mutableStateOf("") }
    val title = stringResource(id = R.string.drafts)

    LaunchedEffect(Unit) {
        if (linkedKey != null) {
            draftViewModel.getLink(linkedKey!!)
        }
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
            val root = if (link != null) {
                if (link!!.message.root != null) {
                    link!!.message.root
                } else {
                    link!!.message.key
                }
            } else {
                null
            }
            val branch = if (link != null) link!!.message.key else null
            SaveDraftFab(onClick = {
                val draft = Draft(
                    oid =0,
                    author =feed.ident.publicKey,
                    timestamp = System.currentTimeMillis(),
                    contentAsText= contentAsText,
                    type = if (draftMode=="vote") "vote" else "post",
                    branch = branch,
                    root = root
                )
                draftViewModel.insert(draft = draft)
            })
        }
        focusRequester.requestFocus()
    }

    LaunchedEffect(draftViewModel.inserted) {
        if (draftViewModel.inserted != null) {
            navController.navigate("${Scenes.DraftEdit.route}/${draftViewModel.inserted}")
        }
    }

    LaunchedEffect(draftViewModel.link) {
        link = draftViewModel.link
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (link != null) {
            MessageItem(
                navController = navController,
                message = link!!.message.toMessageViewData(),
                showToolbar = false,
                expand = false,
                hasLine = true,
                onClickCallBack = {}
            )
        }
        Card(
            colors = CardDefaults.cardColors(),
            shape = RoundedCornerShape(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(44.dp)
                ) {
                    AsyncImage(
                        model = "https://robohash.org/${feed.ident.publicKey}.png",
                        placeholder = rememberAsyncImagePainter("https://robohash.org/${feed.ident.publicKey}.png"),
                        contentDescription = "Profile Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(size = 36.dp)
                            .clip(shape = CircleShape)
                            .background(MaterialTheme.colorScheme.outline),
                    )
                }


                var placeholder =
                    if (draftMode == "reply") "write your answer" else "write you message"
                Column(modifier = Modifier.fillMaxHeight()) {
                    if (draftMode == null || draftMode == "reply") {
                        if (link != null && link!!.about != null && draftMode == "reply") {
                            Text(String.format("in reply to %s", link!!.about!!.name))
                        }
                        OutlinedTextField(
                            label = { placeholder },
                            placeholder = { placeholder },
                            value = contentAsText,
                            onValueChange = {
                                contentAsText = it
                            },
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .fillMaxHeight(0.85f)
                                .padding(16.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
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
