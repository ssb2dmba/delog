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

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toIntRect
import androidx.navigation.NavHostController
import com.google.accompanist.insets.navigationBarsWithImePadding
import `in`.delog.R
import `in`.delog.db.model.Draft
import `in`.delog.db.model.MessageAndAbout
import `in`.delog.ui.LocalActiveFeed
import `in`.delog.ui.component.BottomBarMainButton
import `in`.delog.ui.component.EmojiPicker
import `in`.delog.ui.component.IdentityBox
import `in`.delog.ui.component.MessageItem
import `in`.delog.ui.component.MessageViewData
import `in`.delog.ui.component.toMessageViewData
import `in`.delog.ui.navigation.Scenes
import `in`.delog.ui.observeAsState
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.DraftViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


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
                        viewModel.draft?.let { viewModel.publishDraft(it.value, viewModel.feed) }
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
                        viewModel.draft?.let { viewModel.delete(it.value) }
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
fun DraftEdit(navController: NavHostController, draftMode: String, draftId: Long, link: String) {
    val identAndAbout = LocalActiveFeed.current ?: return
    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()

    val draftViewModel = koinViewModel<DraftViewModel>(parameters = {
        parametersOf(
            identAndAbout.ident,
            draftMode,
            draftId,
            link
        )
    })
    val draft by draftViewModel.draft.observeAsState(null)

    if (draft == null) {
        return
    }
    var dirtyStatus by remember { mutableStateOf(true) }
    val itemClicked = {
        dirtyStatus = !dirtyStatus
    }
    val link by draftViewModel.link.observeAsState(null)


    if (dirtyStatus) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(Modifier.weight(1f)) {
            val state = rememberScrollState()
            Column(
                Modifier.verticalScroll(state)
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
                IdentityBox(identAndAbout = identAndAbout)
                if (link != null) {
                    MessageItem(
                        navController = navController,
                        message = link!!.message.toMessageViewData(),
                        showToolbar = false,
                        hasDivider = true,
                        onClickCallBack = {}
                    )
                }
                val showInputField: Boolean
                ReplyHeader(link = link, draftMode = draftMode)
                val coroutineScope = rememberCoroutineScope()
                when (draftMode) {
                    "repost" -> showInputField = false
                    "vote" -> {
                        showInputField = false
                        Text(
                            text = draft!!.contentAsText,
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
                        EmojiPicker {  draftViewModel.updateDraftContentAsText(it.emoji)  }
//                        coroutineScope.launch {
//                            state.animateScrollTo(state.maxValue) //  - 230)
//                        }
                    }

                    "reply" -> {
                        showInputField = true
                    }

                    else -> showInputField = true
                }

                if (showInputField) {

                    val focusRequester = remember { FocusRequester() }
                    var tfv by remember {
                        mutableStateOf(
                            TextFieldValue(
                                text = draft!!.contentAsText,
                                selection = TextRange(draft!!.contentAsText.length)
                            )
                        )
                    }
                    OutlinedTextField(
                        value = tfv,
                        onValueChange = {
                            tfv = it
                            draftViewModel.updateDraftContentAsText(it.text)
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
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.ime))
    }


    } else {
        val obj: MessageViewData = draft!!.toMessageViewData()
        MessageItem(
            navController = navController,
            message = obj,
            showToolbar = false,
            hasDivider = link != null,
            onClickCallBack = itemClicked
        )
    }


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
            val context = LocalContext.current
            val toastText = stringResource(R.string.draft_saved_confirmation)
            Spacer(Modifier.weight(1f))
            SaveDraftFab {
                dirtyStatus = false
                draftViewModel.save(draft!!)
                Toast
                    .makeText(
                        context,
                        toastText,
                        Toast.LENGTH_LONG
                    )
                    .show()
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