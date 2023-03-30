package org.dlog.scene

import LocalActiveFeed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.dlog.R
import org.dlog.ui.component.MessageItem
import org.dlog.ui.component.MessageViewData
import org.dlog.ui.component.toMessageViewData
import org.dlog.ui.navigation.Scenes
import org.dlog.viewmodel.DraftViewModel
import org.dlog.viewmodel.TopBarViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun DraftEditTopBarMenu(vm: DraftViewModel) {
    var showMenu by remember { mutableStateOf(false) }

    if (!vm.dirty) {
        Button(onClick = { vm.onPublishDialogClicked() }) {
            Text(text = stringResource(R.string.publish))
        }
    }

    IconButton(onClick = { showMenu = !showMenu }) {
        Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
    }

    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        if (vm.dirty) {
            DropdownMenuItem(
                enabled = false,
                onClick = {
                    vm.onPublishDialogClicked()
                    showMenu = false
                },
                text = { Text(text = "publish message") }
            )
        }
        if (!vm.dirty) {
            DropdownMenuItem(
                enabled = true,
                onClick = {
                    vm.setDirty()
                    showMenu = false

                },
                text = { Text(text = "edit draft") }
            )
        }
        DropdownMenuItem(onClick = { //
            vm.onOpenDeleteDialogClicked()
            showMenu = false
        },
            text = { Text(text = stringResource(R.string.delete_message)) }
        )
    }

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
    val topBarViewModel = koinViewModel<TopBarViewModel>()
    val draftViewModel = koinViewModel<DraftViewModel>(parameters = { parametersOf(feed) })

    val strWhatsUp = stringResource(id = R.string.whats_up)
    LaunchedEffect(draftId) {
        draftViewModel.setCurrentDraft(draftId)
        topBarViewModel.setActions { DraftEditTopBarMenu(draftViewModel) }
        topBarViewModel.setTitle(strWhatsUp.format(feed.alias))
    }

    if (draftViewModel.draft == null) {
        return
    }

    var contentAsText by remember { mutableStateOf(draftViewModel.draft!!.contentAsText) }
    Card(
        shape = RoundedCornerShape(0.dp),
        elevation = CardDefaults.cardElevation(),
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp, 8.dp)
                .fillMaxSize()
        ) {
            if (draftViewModel.dirty) {
                // edit mode
                TextField(
                    value = contentAsText,
                    onValueChange = {
                        draftViewModel.setDirty()
                        contentAsText = it
                    },
                    modifier = Modifier
                        .weight(1F)
                        .fillMaxWidth()
                )
            } else {
                // preview mode
                val obj: MessageViewData = draftViewModel.draft!!.toMessageViewData();
                MessageItem(navController = navController, obj, onClickCallBack = {})
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (draftViewModel.dirty) {
                Button(
                    onClick = {
                        val draft = draftViewModel.draft!!.copy()
                        draft.contentAsText = contentAsText
                        draftViewModel.update(draft = draft)
                        navController.navigate(Scenes.DraftList.route) {
                            popUpTo(Scenes.DraftList.route) {
                                inclusive = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(id = R.string.save_draft))
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
