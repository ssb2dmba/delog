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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Plumbing
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import `in`.delog.MainApplication
import `in`.delog.R
import `in`.delog.db.model.Ident
import `in`.delog.db.model.getInviteURl
import `in`.delog.service.ssb.SsbService
import `in`.delog.ui.component.IdentityBox
import `in`.delog.ui.navigation.Scenes
import `in`.delog.ui.observeAsState
import `in`.delog.ui.scene.identitifiers.InviteWebRequest
import `in`.delog.ui.theme.keySmall
import `in`.delog.viewmodel.AboutUIState
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.IdentAndAboutViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.androidx.compose.get
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun IdentDetailTopBarMenu(
    navHostController: NavHostController,
    vm: IdentAndAboutViewModel,
    onOpenExportDialogClicked: () -> Unit,
    onOpenDeleteDialogClicked: () -> Unit

) {
    val uiState by vm.uiState.observeAsState(AboutUIState())
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
                        navHostController.navigate("${Scenes.FeedDetail.route}/${uiState.identAndAbout!!.ident.oid}")
                    },
                    text = { Text(text = stringResource(R.string.network)) }
                )
            } else {

                DropdownMenuItem(
                    enabled = true,
                    onClick = {
                        showMenu = false
                        navHostController.navigate("${Scenes.AboutEdit.route}/${uiState.identAndAbout!!.ident.oid}")
                    },
                    text = { Text(text = stringResource(R.string.about)) })
            }
        }
        DropdownMenuItem(
            enabled = true,
            onClick = {
                onOpenExportDialogClicked.invoke()
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
            onOpenDeleteDialogClicked.invoke()
            showMenu = false
        }, text = { Text(text = stringResource(R.string.delete)) })
    }
}


@Composable
fun IdentDetail(
    navHostController: NavHostController,
    id: String
) {
    val vm = koinViewModel<IdentAndAboutViewModel>(parameters = { parametersOf(id) })
    val uiState by vm.uiState.observeAsState(AboutUIState())

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


    if (uiState.showDeleteDialogState) {
        IdentDetailConfirmDeleteDialog(navHostController, vm) { vm.closeDeteDialog() }
    }
    if (uiState.showExportDialogState) {
        ExportMnemonicDialog(identAndAbout = uiState.identAndAbout!!) { vm.closeExportDialog() }
    }

    IdentEdit(ident = uiState.identAndAbout!!.ident, navHostController, vm)

}


@Composable
fun IdentDetailConfirmDeleteDialog(
    navHostController: NavHostController,
    viewModel: IdentAndAboutViewModel,
    onDismissRequest: () -> Unit
) {
    val uiState by viewModel.uiState.observeAsState(AboutUIState())
    AlertDialog(onDismissRequest = onDismissRequest,
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
                    .clickable { onDismissRequest.invoke() }
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
                        viewModel.delete(uiState.identAndAbout!!.ident)
                        onDismissRequest.invoke()
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


@Composable
fun IdentEdit(ident: Ident, navHostController: NavHostController, vm: IdentAndAboutViewModel) {
    var server by remember { mutableStateOf(ident.server) }
    var port by remember { mutableStateOf(ident.port) }
    var defaultIdent by remember { mutableStateOf(ident.defaultIdent) }
    var showExportDialogState by remember { mutableStateOf(false) }
    var showDeleteDialogState by remember { mutableStateOf(false) }
    var showInviteRequest by remember { mutableStateOf(false) }

    val uiState by vm.uiState.observeAsState(AboutUIState())
    val identAndAbout = uiState.identAndAbout

    if (identAndAbout == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()

    bottomBarViewModel.setActions {
        IdentDetailTopBarMenu(
            navHostController,
            vm,
            { showExportDialogState = true },
            { showDeleteDialogState = true }
        )
        Spacer(modifier = Modifier.weight(1f))
        ExtendedFloatingActionButton(
            onClick = {
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
    val ssbService: SsbService = get()


    fun setUpInvite(invite: String) {
        ident.invite = invite
        vm.onSavingIdent(ident)
        GlobalScope.launch {
            ssbService.connectWithInvite(ident,
                {
                    // everything is going according to the plan
                },
                {
                    MainApplication.toastify(it.message.toString())
                })
        }
        navHostController.navigate("${Scenes.FeedList.route}")
    }

    if (showInviteRequest) {
        val inviteUrl = ident.getInviteURl()
        InviteWebRequest(inviteUrl, ::setUpInvite)
        return
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

            IdentityBox(identAndAbout = identAndAbout!!)

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
                modifier = Modifier.weight(0.8f)
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
                    .padding(start = 8.dp)
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
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        }


        if (identAndAbout!!.ident.invite != null) {
            Row {
                val ssbService: SsbService = get()
                TextButton(
                    onClick = {
                        GlobalScope.launch {
                            ssbService.connectWithInvite(
                                identAndAbout.ident,
                                {
                                    MainApplication.toastify("ok" + it.asString())
                                },
                                {
                                    MainApplication.toastify(it.message.toString())
                                }
                            )
                        }
                        navHostController.navigate("${Scenes.FeedList.route}")
                    }
                ) {
                    Text(text = "redeem invite")
                }
                TextButton(
                    onClick = {
                        vm.cleanInvite(identAndAbout!!.ident)
                        navHostController.navigate("${Scenes.FeedDetail.route}/${identAndAbout!!.ident.oid}")
                    }
                ) {
                    Text(text = "delete invite")
                }

            }
        } else if (identAndAbout!!.ident.server != null) {
            Row {
                val ssbService: SsbService = get()
                TextButton(
                    onClick = {
                        showInviteRequest = true
                    }
                ) {
                    Text(
                        text = String.format(
                            stringResource(R.string.get_invite_and_redeem_it),
                            identAndAbout!!.ident.server
                        )
                    )
                }
            }
        }
    }
}
