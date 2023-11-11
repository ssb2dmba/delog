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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import `in`.delog.R
import `in`.delog.db.model.About
import `in`.delog.ui.component.IdentityBox
import `in`.delog.ui.navigation.Scenes
import `in`.delog.ui.observeAsState
import `in`.delog.ui.theme.keySmall
import `in`.delog.viewmodel.AboutUIState
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.IdentAndAboutViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
//@OptIn(ExperimentalLifecycleComposeApi::class)
fun AboutEdit(
    navHostController: NavHostController,
    pubKey: String
) {
    val viewModel = koinViewModel<IdentAndAboutViewModel>(parameters = { parametersOf(pubKey) })
    val uiState by viewModel.uiState.observeAsState(AboutUIState())
    val aliasHasError by viewModel.aliasHasError.collectAsState()

    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
    val title = stringResource(R.string.about)
    LaunchedEffect(Unit) {
        bottomBarViewModel.setTitle(title)
    }
    if (uiState.identAndAbout == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val identAndAbout = uiState.identAndAbout!!
    val about = uiState.identAndAbout!!.about!!

    if (uiState.showExportDialogState) {
        ExportMnemonicDialog(identAndAbout = identAndAbout) { viewModel.closeExportDialog() }
    }

    if (uiState.showPublishDialogState) {
        AboutEditPublishDialog(
            navHostController,
            viewModel,
            about
        ) {
            viewModel.closePublishDialog()
        }
    }

    if (uiState.showDeleteDialogState) {
        IdentDetailConfirmDeleteDialog(navHostController, viewModel) { viewModel.closeDeteDialog() }
    }

    if (!uiState.dirty) {
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
                                Toast
                                    .makeText(
                                        context,
                                        String.format("%s copied!", about.about),
                                        Toast.LENGTH_LONG
                                    )
                                    .show()
                            },
                        text = about.about,
                        style = keySmall,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }

                Row(modifier = Modifier.padding(top = 16.dp)) {
                    AsyncImage(
                        model = "https://robohash.org/${about.about}.png",
                        placeholder = rememberAsyncImagePainter("https://robohash.org/${about.about}.png"),
                        contentDescription = "Profile Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .padding(top = 12.dp, end = 8.dp)
                            .size(size = 48.dp)
                            .clip(shape = CircleShape)
                            .background(MaterialTheme.colorScheme.outline),
                    )

                    // name
                    OutlinedTextField(
                        value = uiState.alias,
                        onValueChange = { newValue -> viewModel.updateAlias(newValue)
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

            if (uiState.didValid!=null) {
                var idStatusText = "${uiState.alias}@${uiState.identAndAbout!!.ident.server}"
                var idStatusIcon = Icons.Default.Done
                var didColor = MaterialTheme.colorScheme.primary
                if (!uiState.didValid!!) {
                    didColor = MaterialTheme.colorScheme.error
                    idStatusText = "${uiState.alias}@${uiState.identAndAbout!!.ident.server} is not not available"
                    idStatusIcon = Icons.Default.Close
                }
                Row(modifier = Modifier.fillMaxWidth().padding(start = 68.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(idStatusIcon, contentDescription = idStatusText, tint= didColor)
                    Text(idStatusText, modifier= Modifier.padding(start= 8.dp),color=didColor, style = MaterialTheme.typography.labelSmall)
                }
            }
            // description
            OutlinedTextField(
                value = uiState.description,
                onValueChange = { value -> viewModel.updateDescription(value)
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
        // bottom menu bar for edit mode
        bottomBarViewModel.setActions {
            IdentDetailTopBarMenu(
                navHostController,
                viewModel,
                { viewModel.openExportDialog()  },
                { viewModel.openDeleteDialog()  })
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
                    identAndAbout = identAndAbout,
                    short = false
                )
            }
        }
        // bottom menu bar for review mode
        bottomBarViewModel.setActions {
            IdentDetailTopBarMenu(
                navHostController,
                viewModel,
                { viewModel.openExportDialog()},
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
                text = stringResource(id = R.string.dismiss),
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
                        navHostController.navigate(Scenes.FeedList.route)
                    }
            )
        }
    )
}
