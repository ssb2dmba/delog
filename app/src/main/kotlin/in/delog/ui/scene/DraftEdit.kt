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

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import `in`.delog.R
import `in`.delog.db.model.IdentAndAboutWithBlob
import `in`.delog.db.model.MessageAndAbout
import `in`.delog.model.SsbMessageContent
import `in`.delog.model.toMessageViewData
import `in`.delog.service.ssb.SsbService.Companion.TAG
import `in`.delog.ui.LocalActiveFeed
import `in`.delog.ui.component.BlobsEdit
import `in`.delog.ui.component.BottomBarMainButton
import `in`.delog.ui.component.CancelIcon
import `in`.delog.ui.component.EmojiPicker
import `in`.delog.ui.component.IdentityBox
import `in`.delog.ui.component.MessageItem
import `in`.delog.ui.component.UploadFromGallery
import `in`.delog.ui.navigation.Scenes
import `in`.delog.ui.observeAsState
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.DraftViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DraftEdit(navController: NavHostController, draftMode: String, draftId: Long, link: String) {
    val identAndAbout = LocalActiveFeed.current ?: return

    val draftViewModel = koinViewModel<DraftViewModel>(parameters = {
        parametersOf(
            identAndAbout.ident,
            draftMode,
            draftId,
            link
        )
    })
    val messageViewData by draftViewModel.messageViewData.observeAsState(null)
    if (messageViewData == null) {
        return
    }
    val isKeyboardOpen by keyboardAsState() // true or false
    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
    var dirtyStatus by remember { mutableStateOf(true) }
    val itemClicked = {
        dirtyStatus = !dirtyStatus
    }
    val linkState by draftViewModel.link.observeAsState(null)
    var tfv by remember {
        val initInput = SsbMessageContent.serialize(messageViewData!!.contentAsText).text
        mutableStateOf(
            TextFieldValue(
                text = initInput
                    ?: "",
                selection = TextRange(initInput?.length ?: 0)
            )
        )
    }
    if (dirtyStatus) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            IdentityBox(identAndAbout, true)
            Row(Modifier.weight(1f)) {
                val state = rememberScrollState()
                Column(
                    Modifier.verticalScroll(state)
                ) {
                    if (linkState != null) {
                        MessageItem(
                            navController = navController,
                            messageViewData = linkState!!.message.toMessageViewData(),
                            showToolbar = false,
                            hasDivider = true,
                            onClickCallBack = {}
                        )
                    }
                    if (linkState != null) {
                        MessageItem(
                            navController = navController,
                            messageViewData = linkState!!.message.toMessageViewData(),
                            showToolbar = false,
                            hasDivider = true,
                            onClickCallBack = {},
                        )
                    }
                    val showInputField: Boolean
                    ReplyHeader(link = linkState, draftMode = draftMode)
                    val coroutineScope = rememberCoroutineScope()
                    when (draftMode) {
                        "repost" -> showInputField = false
                        "vote" -> {
                            showInputField = false
                            Text(
                                text = tfv.text,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(12.dp)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.tertiary,
                                        RectangleShape
                                    ),
                                fontSize = 48.sp

                            )
                            EmojiPicker { draftViewModel.updateDraftContentAsText(it.emoji) }
                        }
                        "reply" -> {
                            showInputField = true
                        }
                        else -> showInputField = true
                    }

                    if (showInputField) {

                        val focusRequester = remember { FocusRequester() }

                        OutlinedTextField(
                            value = tfv,
                            onValueChange = {
                                tfv = it
                            },
                            keyboardOptions = KeyboardOptions(
                                autoCorrect = true,
                            ),
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .fillMaxSize()
                                .defaultMinSize(minHeight = 200.dp)
                                .onFocusEvent { focusState ->
                                    if (focusState.isFocused) {
                                        coroutineScope.launch {
                                            state.animateScrollTo(state.maxValue)
                                        }
                                    }
                                }
                                .padding(8.dp)
                        )
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                    }
                }
            }
            if (messageViewData!!.blobs.isNotEmpty() && !isKeyboardOpen) {
                Row(modifier = Modifier.weight(1f)) {
                    BlobsEdit(
                        blobs = messageViewData!!.blobs,
                        action = { draftViewModel.unSelect(it.key) },
                        actionIcon = { CancelIcon() }
                    )
                }
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.ime))
        }
    } else {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier=Modifier.padding(8.dp)) {
                    item {
                        MessageItem(
                            navController = navController,
                            messageViewData = messageViewData!!,
                            showToolbar = false,
                            hasDivider = linkState != null,
                            onClickCallBack = itemClicked
                        )
                    }
                }
            }
        }
    }


    bottomBarViewModel.setActions {
        if (dirtyStatus) {
            UploadFromGallery(
                isUploading = draftViewModel.isLoadingImage,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier,
                mockPermissionsState = null,
                onImageChosen = { uri ->
                    draftViewModel.selectImage(uri)
                }
            )
        }

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
            val context = LocalContext.current
            val toastText = stringResource(R.string.draft_saved_confirmation)
            Spacer(Modifier.weight(1f))
            SaveDraftFab {
                Log.i(TAG,"update draft content as text with:" + tfv.text)
                draftViewModel.updateDraftContentAsText(tfv.text)
                Log.i(TAG,"got:" + draftViewModel.messageViewData.value.contentAsText)
                draftViewModel.save(messageViewData!!)
                Toast
                    .makeText(
                        context,
                        toastText,
                        Toast.LENGTH_LONG
                    )
                    .show()
                dirtyStatus = false
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

    val showDeleteDialogState: Boolean by draftViewModel.showDeleteDialog.collectAsState()

    val showPublishDialogState: Boolean by draftViewModel.showPublishDialog.collectAsState()

    if (showDeleteDialogState) {
        DraftConfirmDeleteDialog(navController, draftViewModel)
    }

    if (showPublishDialogState) {
        DraftPublishDialog(navController, draftViewModel)
    }
}

@Composable
fun ReplyHeader(link: MessageAndAbout?, draftMode: String?) {
    if (link == null) return
    var txt: String? = null
    when (draftMode) {
        "reply" -> txt = String.format("in reply to %s", link.about!!.name)
        "repost" -> txt = String.format("repost %s message", link.about!!.name)
        "vote" -> txt = String.format("vote for %s message", link.about!!.name)
    }
    if (txt != null) {
        Text(
            modifier = Modifier.padding(8.dp),
            text = txt,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
fun SaveDraftFab(onClick: () -> Unit) {
    BottomBarMainButton(
        onClick = onClick,
        text = stringResource(id = R.string.save)
    )
}

@Composable
fun keyboardAsState(): State<Boolean> {
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    return rememberUpdatedState(isImeVisible)
}

@Composable
fun PublishDraftFab(onClick: () -> Unit) {
    BottomBarMainButton(
        modifier = Modifier.testTag("new_contact"),
        onClick = onClick,
        text = stringResource(id = R.string.publish)
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
                        viewModel.messageViewData.let {
                            viewModel.publishDraft(
                                it.value,
                                viewModel.feed
                            )
                        }
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
                        viewModel.messageViewData.let { viewModel.delete(it.value) }
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