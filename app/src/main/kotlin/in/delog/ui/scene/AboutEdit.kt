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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import `in`.delog.ssb.*
import `in`.delog.ui.component.IdentityBox
import `in`.delog.ui.navigation.Scenes
import `in`.delog.ui.theme.keySmall
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.IdentViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutEdit(
    navHostController: NavHostController,
    pubKey: String
) {
    val vm = koinViewModel<IdentViewModel>(parameters = { parametersOf(pubKey) })
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()

    var dirty by remember { mutableStateOf(false) }
    val showExportDialogState: Boolean by vm.showExportDialog.collectAsState()
    val showPublishDialogState: Boolean by vm.showPublishDialog.collectAsState()
    val showDeleteDialogState: Boolean by vm.showDeleteDialog.collectAsState()

    val title = stringResource(R.string.about)
    LaunchedEffect(dirty) {
        vm.setCurrentIdentByPk(pubKey)
        bottomBarViewModel.setTitle(title)
    }
    if (vm.about == null) {
        return
    }
    val about = vm.about!!

    var name by remember { mutableStateOf(about.name) }
    var description by remember { mutableStateOf(about.description) }
    var image by remember { mutableStateOf(about.image) }

    if (showExportDialogState) {
        IdentDetailExportDialog(vm)
    }

    if (showPublishDialogState) {
        AboutEditPublishDialog(navHostController, vm, About(pubKey, name, description, image, true))
    }

    if (showDeleteDialogState) {
        IdentDetailConfirmDeleteDialog(navHostController, vm)
    }

    if (!dirty) {
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
                        value = if (name != null) name!! else "",
                        onValueChange = { value ->
                            name = value
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
            Spacer(modifier = Modifier.height(12.dp))
            // description
            OutlinedTextField(
                value = if (description != null) description!! else "",
                onValueChange = { value ->
                    description = value
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
            IdentDetailTopBarMenu(navHostController, vm)
            Spacer(modifier = Modifier.weight(1f))
            // save
            ExtendedFloatingActionButton(
                onClick = {
                    vm.onSavingAbout(About(about.about, name, description, image, true));
                    dirty = true
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
                    about = about,
                    short = false,
                    navController = navHostController,
                    mine = true,
                    following = false
                )
            }
        }
        // bottom menu bar for review mode
        bottomBarViewModel.setActions {
            IdentDetailTopBarMenu(navHostController, vm)
            IconButton(onClick = { dirty = false }) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "edit")
            }
            Spacer(modifier = Modifier.weight(1f))
            // save
            ExtendedFloatingActionButton(
                onClick = {
                    //TODO
                },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Send,
                        contentDescription = stringResource(id = R.string.save)
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
    viewModel: IdentViewModel,
    about: About
) {
    AlertDialog(onDismissRequest = { viewModel.onExportDialogDismiss() },
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
                    .clickable { viewModel.onExportDialogDismiss() }
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
                        navHostController.navigate("${Scenes.FeedList.route}")
                    }
            )
        }
    )
}
