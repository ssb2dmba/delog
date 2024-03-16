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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import `in`.delog.MainApplication
import `in`.delog.R
import `in`.delog.db.model.About
import `in`.delog.db.model.Ident
import `in`.delog.db.model.IdentAndAbout
import `in`.delog.db.model.IdentAndAboutWithBlob
import `in`.delog.ui.component.IdentityBox
import `in`.delog.ui.component.ProfileImage
import `in`.delog.ui.component.UploadFromGallery
import `in`.delog.ui.navigation.Scenes
import `in`.delog.ui.observeAsState
import `in`.delog.ui.theme.MyTheme
import `in`.delog.ui.theme.keySmall
import `in`.delog.viewmodel.AboutUIState
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.IdentAndAboutViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AboutEdit(
    navHostController: NavHostController,
    pubKey: String
) {
    val viewModel = koinViewModel<IdentAndAboutViewModel>(parameters = { parametersOf(pubKey) })
    val uiState: AboutUIState? by viewModel.uiState.observeAsState(null)
    if (uiState == null) {
        return
    }
    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()


    val about = uiState!!.identAndAboutWithBlob.about


    if (uiState!!.showExportDialogState) {
        ExportMnemonicDialog(identAndAbout = uiState!!.identAndAboutWithBlob) { viewModel.closeExportDialog() }
    }

    if (uiState!!.showPublishDialogState) {
        AboutEditPublishDialog(
            navHostController,
            viewModel,
            about
        ) {
            viewModel.closePublishDialog()
        }
    }

    if (uiState!!.showDeleteDialogState) {
        IdentDetailConfirmDeleteDialog(navHostController, viewModel) { viewModel.closeDeteDialog() }
    }

    if (!uiState!!.dirty) {
        DirtyAboutEdit(uiState!!, null, viewModel::update)
        // bottom menu bar for edit mode
        bottomBarViewModel.setActions {
            IdentDetailTopBarMenu(
                navHostController,
                viewModel,
                { viewModel.openExportDialog() },
                { viewModel.openDeleteDialog() })
            Spacer(modifier = Modifier.weight(1f))
            // save
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.onSavingAbout(about)
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = stringResource(id = R.string.save)
                    )
                },
                text = { Text(text = stringResource(id = R.string.save)) }
            )
        }
    } else {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                elevation = CardDefaults.cardElevation(),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            ) {
                IdentityBox(
                    identAndAboutWithBlob = uiState!!.identAndAboutWithBlob,
                    short = false
                )
            }
        }
        // bottom menu bar for review mode
        bottomBarViewModel.setActions {
            IdentDetailTopBarMenu(
                navHostController,
                viewModel,
                { viewModel.openExportDialog() },
                { viewModel.openDeleteDialog() })
            IconButton(onClick = { viewModel.setDirty(false) }) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "edit")
            }
            Spacer(modifier = Modifier.weight(1f))
            // save
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.showPublishDialog()
                },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Send,
                        contentDescription = stringResource(id = R.string.publish_about)
                    )
                },
                text = { Text(text = stringResource(id = R.string.save)) }
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DirtyAboutEdit(
    uiState: AboutUIState,
    multiplePermissionsState: PermissionStatePreview? = null,
    callback: ((AboutUIState) -> Unit)
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val about = uiState.identAndAboutWithBlob.about
    var name by remember { mutableStateOf(about.name) }
    var description by remember { mutableStateOf(about.description) }

    Card(
        elevation = CardDefaults.cardElevation(),
        //shape = RoundedCornerShape(0.dp),
        modifier = Modifier
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp, 8.dp)
                .fillMaxWidth()
        ) {
            // public key
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboardManager.setText(buildAnnotatedString { append(about.about) })
                            MainApplication.toastify(String.format("%s copied!", about.about))
                        },
                    text = about.about,
                    style = keySmall,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }

            Row(modifier = Modifier.padding(top = 16.dp)) {
                Box {
                    ProfileImage(identAndAboutWithBlob = uiState.identAndAboutWithBlob)
                    UploadFromGallery(
                        isUploading = false,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp),
                        mockPermissionsState = multiplePermissionsState,
                        onImageChosen = {
                            callback(uiState.copy(imageToPick = it))
                        },
                        mimeFilter = "image/*",
                    )
                }

                // name
                OutlinedTextField(
                    value = name ?: "",
                    onValueChange = { newValue ->
                        name = newValue
                        val i = uiState.copy()
                        i.identAndAboutWithBlob.about.name = newValue
                        callback(i)
                    },
                    label = {
                        Text(
                            text = stringResource(id = R.string.alias),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    modifier = Modifier.weight(0.8f)
                )
            }
        }

        DidValidationRow(uiState)
        // description
        OutlinedTextField(
            value = description ?: "",
            onValueChange = { value ->
                description = value
                val i = uiState.copy()
                i.identAndAboutWithBlob.about.description = value
                callback(i)
            },
            label = {
                Text(
                    text = stringResource(id = R.string.description),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp, 8.dp)
        )
    }

}

@Composable
fun DidValidationRow(uiState: AboutUIState) {
    if (uiState.didValid != null) {
        val identifier = uiState.didValidationErrorMessage
        var idStatusText = ""
        var idStatusIcon = Icons.Default.QuestionMark
        var didColor = MaterialTheme.colorScheme.error
        if (uiState.didValid == true) {
            didColor = MaterialTheme.colorScheme.primary
            idStatusIcon = Icons.Default.Done
        } else if (uiState.didValid == false) {
            if (uiState.didValidationErrorMessage.isEmpty()) {
                didColor = MaterialTheme.colorScheme.error
                idStatusIcon = Icons.Default.QuestionMark
                idStatusText = "$identifier is not available"
            } else {
                didColor = MaterialTheme.colorScheme.error
                idStatusText = uiState.didValidationErrorMessage
                idStatusIcon = Icons.Default.Close
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 68.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(idStatusIcon, contentDescription = idStatusText, tint = didColor)
            Text(
                idStatusText,
                modifier = Modifier.padding(start = 8.dp),
                color = didColor,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
@Preview
fun PreviewAboutEdit() {

    val ident = Ident(
        oid = 0,
        publicKey = "publicKey",
        server = "server",
        port = 8080,
        sortOrder = 0,
        defaultIdent = true,
        privateKey = "",
        invite = "",
        lastPush = 0
    )
    val about = About(
        ident.publicKey,
        name = "someone",
        description = "awesome",
        dirty = true
    )
    val identAbout = IdentAndAbout(ident, about)
    val uiState = AboutUIState(
        identAndAboutWithBlob = IdentAndAboutWithBlob(
            identAbout.ident,
            identAbout.about!!,
            null
        )
    )


    CompositionLocalProvider {
        MyTheme(
            darkTheme = false,
            dynamicColor = false
        ) {

            DirtyAboutEdit(
                uiState,
                PermissionStatePreview(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    PermissionStatus.Granted
                )
            ) {}
        }
    }


}

@ExperimentalPermissionsApi
class PermissionStatePreview(
    override val permission: String,
    override val status: PermissionStatus
) : PermissionState {
    override fun launchPermissionRequest() {
        //
    }


}


@Composable
fun AboutEditPublishDialog(
    navHostController: NavHostController,
    viewModel: IdentAndAboutViewModel,
    about: About,
    onDismissRequest: () -> Unit
) {
    AlertDialog(onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                color = MaterialTheme.colorScheme.onSurface,
                text = stringResource(id = R.string.publish_about),
                style = MaterialTheme.typography.titleSmall
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.publish),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        dismissButton = {
            Text(
                text = stringResource(id = R.string.later),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(15.dp)
                    .clickable { onDismissRequest.invoke() }
            )
        },
        confirmButton = {
            Text(
                text = stringResource(R.string.publish),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(15.dp)
                    .clickable {
                        viewModel.onDoPublishClicked(about)
                        navHostController.navigate(Scenes.FeedList.route) {
                            popUpTo(navHostController.graph.startDestinationId)
                        }
                    }
            )
        }
    )
}
