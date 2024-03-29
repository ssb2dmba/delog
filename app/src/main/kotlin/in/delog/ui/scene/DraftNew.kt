///**
// * Delog
// * Copyright (C) 2023 dmba.info
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package `in`.delog.ui.scene
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxHeight
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Cancel
//import androidx.compose.material3.Card
//import androidx.compose.material3.CardDefaults
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.OutlinedTextField
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment.Companion.CenterHorizontally
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.focus.FocusRequester
//import androidx.compose.ui.focus.focusRequester
//import androidx.compose.ui.graphics.RectangleShape
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavHostController
//import coil.compose.AsyncImage
//import coil.compose.rememberAsyncImagePainter
//import `in`.delog.R
//import `in`.delog.db.model.Draft
//import `in`.delog.db.model.MessageAndAbout
//import `in`.delog.ui.LocalActiveFeed
//import `in`.delog.ui.component.BottomBarMainButton
//import `in`.delog.ui.component.EmojiPicker
//import `in`.delog.ui.component.IdentityBox
//import `in`.delog.ui.component.MessageItem
//import `in`.delog.ui.component.toMessageViewData
//import `in`.delog.ui.navigation.Scenes
//import `in`.delog.viewmodel.BottomBarViewModel
//import `in`.delog.viewmodel.DraftViewModel
//import org.koin.androidx.compose.koinViewModel
//import org.koin.core.parameter.parametersOf
//
//@Composable
//fun DraftNew(
//    navController: NavHostController,
//    linkedKey: String? = null,
//    draftMode: String? = null
//) {
//    val identAndAbout = LocalActiveFeed.current ?: return
//    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
//    val draftViewModel =
//        koinViewModel<DraftViewModel>(parameters = { parametersOf(identAndAbout.ident, null, linkedKey) })
//
//    var link: MessageAndAbout? by remember {
//        mutableStateOf(null)
//    }
//
//    var contentAsText by remember { mutableStateOf("") }
//
//    LaunchedEffect(Unit) {
////        if (linkedKey != null) {
////            draftViewModel.getLink(linkedKey)
////        }
//        bottomBarViewModel.setActions {
//            IconButton(
//                modifier = Modifier
//                    .height(56.dp)
//                    .padding(start = 24.dp),
//                onClick = {
//                    navController.popBackStack()
//                }) {
//                Icon(
//                    imageVector = Icons.Filled.Cancel,
//                    contentDescription = "cancel",
//                )
//            }
//            Spacer(modifier = Modifier.weight(1f))
//            val root = if (link != null) {
//                if (link!!.message.root != null) {
//                    link!!.message.root
//                } else {
//                    link!!.message.key
//                }
//            } else {
//                null
//            }
//            val branch = if (link != null) link!!.message.key else null
//            SaveDraftFab(onClick = {
//                val draft = Draft(
//                    oid = 0,
//                    author = identAndAbout.ident.publicKey,
//                    timestamp = System.currentTimeMillis(),
//                    contentAsText = contentAsText,
//                    type = if (draftMode == "vote") "vote" else "post",
//                    branch = branch,
//                    root = root
//                )
//                draftViewModel.save(draft)
//            })
//        }
//    }
//
////    LaunchedEffect(draftViewModel.link) {
////        link = draftViewModel.link
////    }
//    val focusRequester = remember { FocusRequester() }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//    ) {
//        IdentityBox(identAndAbout = identAndAbout)
//        if (link != null) {
//            MessageItem(
//                navController = navController,
//                message = link!!.message.toMessageViewData(),
//                showToolbar = false,
//                hasDivider = true,
//                onClickCallBack = {}
//            )
//        }
//        Card(
//            colors = CardDefaults.cardColors(),
//            shape = RoundedCornerShape(0.dp)
//        ) {
//            Row(
//                modifier = Modifier
//                    .fillMaxHeight()
//                    .padding(12.dp)
//            ) {
//                if (linkedKey != null) {
//                    Box(
//                        modifier = Modifier
//                            .fillMaxHeight()
//                            .width(44.dp)
//                    ) {
//                        AsyncImage(
//                            model = "https://robohash.org/${identAndAbout.ident.publicKey}.png",
//                            placeholder = rememberAsyncImagePainter("https://robohash.org/${identAndAbout.ident.publicKey}.png"),
//                            contentDescription = "Profile Image",
//                            contentScale = ContentScale.Crop,
//                            modifier = Modifier
//                                .size(size = 36.dp)
//                                .clip(shape = CircleShape)
//                                .background(MaterialTheme.colorScheme.outline),
//                        )
//                    }
//                }
//
//
//                Column(modifier = Modifier.fillMaxSize()) {
//                    val showInputField: Boolean
//                    ReplyHeader(link = link, draftMode = draftMode)
//                    when (draftMode) {
//                        "repost" -> showInputField = false
//                        "vote" -> {
//                            showInputField = false
//                            Text(
//                                text = contentAsText,
//                                modifier = Modifier
//                                    .align(CenterHorizontally)
//                                    .padding(12.dp)
//                                    .border(
//                                        1.dp,
//                                        MaterialTheme.colorScheme.tertiary,
//                                        RectangleShape
//                                    ),
//                                fontSize = 48.sp
//
//                            )
//                            EmojiPicker { contentAsText = it.emoji }
//                        }
//
//                        "reply" -> {
//                            showInputField = true
//                        }
//
//                        else -> showInputField = true
//                    }
//
//                    if (showInputField) {
//                        OutlinedTextField(
//                            value = contentAsText,
//                            onValueChange = {
//                                contentAsText = it
//                            },
//                            modifier = Modifier
//                                .focusRequester(focusRequester)
//                                .fillMaxHeight(0.85f)
//                                .padding(16.dp)
//                                .fillMaxWidth()
//                        )
//                        LaunchedEffect(Unit) {
//                            focusRequester.requestFocus()
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
//
//
//

