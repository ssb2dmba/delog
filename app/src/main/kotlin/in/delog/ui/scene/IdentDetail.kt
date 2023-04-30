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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Plumbing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import `in`.delog.R
import `in`.delog.db.model.Ident
import `in`.delog.db.model.asKeyPair
import `in`.delog.ssb.*
import `in`.delog.ssb.BaseSsbService.Companion.TAG
import `in`.delog.ui.navigation.Scenes
import `in`.delog.ui.theme.keySmall
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.IdentViewModel
import io.vertx.core.http.impl.HttpClientConnection.log
import org.apache.tuweni.io.Base64
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.*


@Composable
fun IdentDetailTopBarMenu(navHostController: NavHostController, vm: IdentViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    IconButton(onClick = { showMenu = !showMenu }) {
        Icon(imageVector = Icons.Outlined.Plumbing, contentDescription = null)
    }
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        val currentRoute = navHostController.currentBackStackEntry?.destination?.route
        if (currentRoute != null) {
            if (currentRoute.contains(Scenes.AboutEdit.route)) {
                DropdownMenuItem(
                    enabled = true,
                    onClick = {
                        showMenu = false
                        navHostController.navigate("${Scenes.FeedDetail.route}/${vm.ident!!.oid}")
                    },
                    text = { Text(text = stringResource(R.string.network)) }
                )
            } else {

                DropdownMenuItem(
                    enabled = true,
                    onClick = {
                        showMenu = false
                        var argUri = URLEncoder.encode(
                            vm.ident!!.publicKey,
                            Charset.defaultCharset().toString()
                        )
                        navHostController.navigate("${Scenes.AboutEdit.route}/${argUri}")
                    },
                    text = { Text(text = stringResource(R.string.about)) })
            }
        }
        DropdownMenuItem(
            enabled = true,
            onClick = {
                vm.onOpenExportDialogClicked()
                showMenu = false
            },
            text = { Text(text = stringResource(R.string.export_mnemonic)) })
        DropdownMenuItem(
            enabled = false,
            onClick = {
                showMenu = false
            },
            text = { Text(text = stringResource(R.string.export_pub_key)) })
        DropdownMenuItem(onClick = { //
            vm.onOpenDeleteDialogClicked()
            showMenu = false
        }, text = { Text(text = stringResource(R.string.delete)) })
    }
}


@Composable
fun IdentDetail(
    navHostController: NavHostController,
    id: String
) {
    val vm = koinViewModel<IdentViewModel>(parameters = { parametersOf(id) })


    val showDeleteDialogState: Boolean by vm.showDeleteDialog.collectAsState()
    val showExportDialogState: Boolean by vm.showExportDialog.collectAsState()


    LaunchedEffect(id) {
        vm.setCurrentIdent(id)
    }
    if (vm.ident == null) {
        return
    }

    if (showDeleteDialogState) {
        IdentDetailConfirmDeleteDialog(navHostController, vm)
    }
    if (showExportDialogState) {
        IdentDetailExportDialog(vm)
    }

    IdentEdit(ident = vm.ident!!, navHostController, vm)

}


@Composable
fun IdentDetailConfirmDeleteDialog(
    navHostController: NavHostController,
    viewModel: IdentViewModel
) {
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
                        viewModel.delete(viewModel.ident!!)
                        navHostController.navigate(Scenes.FeedList.route) {
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
fun IdentEdit(ident: Ident, navHostController: NavHostController, vm: IdentViewModel) {

    var alias by remember { mutableStateOf(ident.alias) }
    var aliasHasError by remember { mutableStateOf(false) }
    var server by remember { mutableStateOf(ident.server) }
    var serverHasError by remember { mutableStateOf(false) } // TODO improve form
    var port by remember { mutableStateOf(ident.port) }
    var defaultIdent by remember { mutableStateOf(ident.defaultIdent) }


    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
    val title = stringResource(id = R.string.edit)
    bottomBarViewModel.setTitle(title)

    bottomBarViewModel.setActions {
        IdentDetailTopBarMenu(navHostController, vm)
        Spacer(modifier = Modifier.weight(1f))
        ExtendedFloatingActionButton(
            onClick = {
                ident.alias = alias
                ident.port = port
                ident.server = server
                ident.defaultIdent = defaultIdent
                vm.onSavingIdent(ident)
            },
            text = { Text(text = stringResource(id = R.string.save)) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = stringResource(id = R.string.save)
                )
            }
        )

    }

    Card(
        elevation = CardDefaults.cardElevation(),
        shape = RoundedCornerShape(0.dp),
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
            Row {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = ident.publicKey,
                    style = keySmall,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row {
                AsyncImage(
                    model = "https://robohash.org/${ident.publicKey}.png",
                    placeholder = rememberAsyncImagePainter("https://robohash.org/${ident.publicKey}.png"),
                    contentDescription = "Profile Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(size = 48.dp)
                        .clip(shape = CircleShape)
                        .background(MaterialTheme.colorScheme.outline),

                    )
                // server
                OutlinedTextField(
                    value = alias,
                    onValueChange = { value ->
                        alias = value
                    },
                    label = {
                        Text(
                            text = stringResource(id = R.string.alias),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    modifier = Modifier.weight(0.8f),
                    isError = aliasHasError,
                )

            }

        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.padding(8.dp)) {
            // server
            OutlinedTextField(
                value = server,
                onValueChange = { value ->
                    server = value
                },
                label = {
                    Text(
                        text = stringResource(id = R.string.server),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                modifier = Modifier.weight(0.8f),
                isError = aliasHasError, // TODO fixme
            )

            OutlinedTextField(
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                value = port.toString(),
                onValueChange = { value ->
                    port = value.toInt()
                },
                label = {
                    Text(
                        text = stringResource(R.string.port),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                modifier = Modifier
                    .width(80.dp)
                    .padding(start = 8.dp),
                isError = aliasHasError,
            )
        }


        Row(modifier = Modifier.padding(start = 8.dp, end = 8.dp)) {
            Checkbox(
                checked = defaultIdent,
                onCheckedChange = { defaultIdent = !defaultIdent },
                colors = CheckboxDefaults.colors()
            )
            Text(
                text = stringResource(R.string.default_ident),
                //color= MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun IdentDetailExportDialog(
    viewModel: IdentViewModel
) {
    AlertDialog(onDismissRequest = { viewModel.onExportDialogDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                color = MaterialTheme.colorScheme.onSurface,
                text = stringResource(id = R.string.export_mnemonic),
                style = MaterialTheme.typography.titleSmall
            )
        },
        text = {
            val entropy: ByteArray = Base64.decode(viewModel.ident!!.privateKey).toArray()
            val arr: List<String> = WordList(Locale.ENGLISH).words
            val dict = Dict(arr.toTypedArray())
            var mnemonicCode = secretKeyToMnemonic(entropy, dict)
            var s = mnemonicCode.joinToString(" ")
            Text(
                text = s,
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
        confirmButton = {}
    )
}
