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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import `in`.delog.R
import `in`.delog.db.model.Draft
import `in`.delog.ui.LocalActiveFeed
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
                        navHostController.navigate("${Scenes.MainFeed.route}")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftEdit(navController: NavHostController, draftId: String) {
    val feed = LocalActiveFeed.current ?: return
    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
    val draftViewModel = koinViewModel<DraftViewModel>(parameters = { parametersOf(feed.ident) })
    val title = stringResource(id = R.string.drafts)


    LaunchedEffect(draftId) {
        draftViewModel.setCurrentDraft(draftId)
        bottomBarViewModel.setTitle(title)

    }
    if (draftViewModel.draft == null) {
        return
    }

    var contentAsText by remember { mutableStateOf(draftViewModel.draft!!.contentAsText) }

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
        if (draftViewModel.dirtyStatus) {
            Spacer(Modifier.weight(1f))
            SaveDraftFab(onClick = {
                val draft = Draft(
                    draftId.toInt(),
                    feed.ident.publicKey,
                    System.currentTimeMillis(),
                    contentAsText
                )
                draftViewModel.update(draft = draft)
                draftViewModel.setDirty(false)
                navController.navigate("${Scenes.DraftEdit.route}/${draftId}")
            })
        } else {
            IconButton(
                modifier = Modifier.height(56.dp),
                onClick = {
                    draftViewModel.setDirty(true)
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

        Card(
            shape = RoundedCornerShape(0.dp),
            elevation = CardDefaults.cardElevation(),
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    //.verticalScroll(rememberScrollState())
                    //.weight(weight =1f, fill = false)
            ) {
                if (draftViewModel.dirtyStatus) {
                    // edit mode
                    TextField(
                        value = contentAsText,
                        onValueChange = {
                            draftViewModel.setDirty(true)
                            contentAsText = it
                        },
                        modifier = Modifier.fillMaxHeight()
                    )
                } else {
                    // preview mode
                    val obj: MessageViewData = draftViewModel.draft!!.toMessageViewData();
                    MessageItem(
                        navController = navController,
                        message = obj,
                        showToolbar = false,
                        expand = true,
                        onClickCallBack = {
                            draftViewModel.dirtyStatus=!draftViewModel.dirtyStatus
                        })
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
