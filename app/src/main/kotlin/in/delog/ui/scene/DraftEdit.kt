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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusOrder
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import `in`.delog.R
import `in`.delog.db.model.Draft
import `in`.delog.db.model.MessageAndAbout
import `in`.delog.ui.LocalActiveFeed
import `in`.delog.ui.component.IdentityBox
import `in`.delog.ui.component.MessageItem
import `in`.delog.ui.component.MessageViewData
import `in`.delog.ui.component.toMessageViewData
import `in`.delog.ui.navigation.Scenes
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.DraftViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun PublishDraftFab(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = {
            Icon(
                Icons.Filled.Send,
                "send",
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        },
        text = { Text(text = stringResource(id = R.string.publish)) }
    )
}

@Composable
fun DraftPublishDialog(navHostController: NavHostController, viewModel: DraftViewModel) {
    AlertDialog(onDismissRequest = { viewModel.onPublishDialogDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                color = MaterialTheme.colorScheme.onSurface,
                text = stringResource(id = R.string.are_you_sure),
                style = MaterialTheme.typography.titleSmall
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.confirm_delete_irreversible),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        dismissButton = {
            Text(
                text = stringResource(id = R.string.dismiss),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(15.dp)
                    .clickable { viewModel.onPublishDialogDismiss() }
            )
        },
        confirmButton = {
            Text(
                text = stringResource(R.string.publish),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(15.dp)
                    .clickable {
                        viewModel.onPublishDialogDismiss()
                        viewModel.draft?.let { viewModel.publishDraft(it, viewModel.feed) }
                        navHostController.navigate(Scenes.MainFeed.route)
                    }
            )
        }
    )

}

@Composable
fun DraftConfirmDeleteDialog(navHostController: NavHostController, viewModel: DraftViewModel) {
    AlertDialog(onDismissRequest = { viewModel.onDeleteDialogDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                color = MaterialTheme.colorScheme.onSurface,
                text = stringResource(id = R.string.are_you_sure),
                style = MaterialTheme.typography.titleSmall
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.confirm_delete_irreversible),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        dismissButton = {
            Text(
                text = stringResource(id = R.string.dismiss),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(15.dp)
                    .clickable { viewModel.onDeleteDialogDismiss() }
            )
        },
        confirmButton = {
            Text(
                text = stringResource(id = R.string.confirm_delete),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(15.dp)
                    .clickable {
                        viewModel.onDeleteDialogDismiss()
                        viewModel.draft?.let { viewModel.deleteDraft(it) }
                        navHostController.navigate(Scenes.DraftList.route) {
                            popUpTo(Scenes.FeedDetail.route) {
                                inclusive = true
                            }
                        }
                    }
            )
        }
    )

}

@Composable
fun DraftEdit(navController: NavHostController, draftId: String) {
    val identAndAbout = LocalActiveFeed.current ?: return
    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
    val draftViewModel = koinViewModel<DraftViewModel>(parameters = { parametersOf(identAndAbout.ident) })
    val title = stringResource(id = R.string.save_draft)
    var link: MessageAndAbout? by remember {
        mutableStateOf(null)
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(draftId) {
        draftViewModel.setCurrentDraft(draftId)
        bottomBarViewModel.setTitle(title)
    }

    LaunchedEffect(draftViewModel.draft) {
        if (draftViewModel.draft != null && draftViewModel.draft!!.branch!=null) {
            draftViewModel.getLink(draftViewModel.draft!!.branch!!)
        }
    }

    LaunchedEffect(draftViewModel.link) {
        link = draftViewModel.link
    }

    if (draftViewModel.draft == null) {
        return
    }
    var dirtyStatus by remember { mutableStateOf(false) }
    var contentAsText by remember { mutableStateOf(draftViewModel.draft!!.contentAsText) }
    bottomBarViewModel.setTitle(title)
    bottomBarViewModel.setActions {
        IconButton(
            modifier = Modifier.height(56.dp),
            onClick = {
                draftViewModel.onOpenDeleteDialogClicked()
            }
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "delete"
            )
        }
        if (dirtyStatus) {
            Spacer(Modifier.weight(1f))
            SaveDraftFab {
                val draft = Draft(
                    oid = draftId.toInt(),
                    author = identAndAbout.ident.publicKey,
                    timestamp = System.currentTimeMillis(),
                    type = draftViewModel.draft!!.type,
                    contentAsText = contentAsText,
                    root = draftViewModel.draft!!.root,
                    branch = draftViewModel.draft!!.branch
                )
                draftViewModel.update(draft = draft)
                dirtyStatus = false
                navController.navigate("${Scenes.DraftEdit.route}/${draftId}")
            }
        } else {
            IconButton(
                modifier = Modifier.height(56.dp),
                onClick = {
                    dirtyStatus = true
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Open Navigation Drawer"
                )
            }
            Spacer(Modifier.weight(1f))
            PublishDraftFab(
                onClick = {
                    draftViewModel.onPublishDialogClicked()
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
    ) {
        if (link != null) {
            MessageItem(
                navController = navController,
                message = link!!.message.toMessageViewData(),
                showToolbar = false,
                hasDivider = true,
                onClickCallBack = {}
            )
        }
        Card(
            shape = RoundedCornerShape(0.dp),
            elevation = CardDefaults.cardElevation(),
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
        ) {
            IdentityBox(identAndAbout = identAndAbout)
            val itemClicked = {
                dirtyStatus=!dirtyStatus
                focusRequester.requestFocus()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (dirtyStatus) {
                    OutlinedTextField(
                        value = contentAsText,
                        onValueChange = {
                            contentAsText = it
                        },
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .focusTarget()
                            .fillMaxHeight(0.85f)
                            .padding(16.dp)
                            .fillMaxWidth()
                    )

                } else {
                    // preview mode
                    val obj: MessageViewData = draftViewModel.draft!!.toMessageViewData()

                    MessageItem(
                        navController = navController,
                        message = obj,
                        showToolbar = false,
                        hasDivider = link!=null,
                        onClickCallBack = itemClicked)
                }
            }
        }
    }

    val showDeleteDialogState: Boolean by draftViewModel.showDeleteDialog.collectAsState()

    val showPublishDialogState: Boolean by draftViewModel.showPublishDialog.collectAsState()

    if (showDeleteDialogState) {
        DraftConfirmDeleteDialog(navController, draftViewModel)
    }

    if (showPublishDialogState) {
        DraftPublishDialog(navController, draftViewModel)
    }

}
