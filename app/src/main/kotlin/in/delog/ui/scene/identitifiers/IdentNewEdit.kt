package `in`.delog.ui.scene.identitifiers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import `in`.delog.R
import `in`.delog.db.model.Ident
import `in`.delog.ui.navigation.Scenes
import `in`.delog.ui.observeAsState
import `in`.delog.viewmodel.IdentAndAboutViewModel
import `in`.delog.viewmodel.IdentListViewModel
import org.apache.tuweni.scuttlebutt.Identity
import org.apache.tuweni.scuttlebutt.Invite
import org.apache.tuweni.scuttlebutt.MalformedInviteCodeException
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentNewEdit(navController: NavHostController, identity: Identity, inviteString: String) {
    val identity = identity
    var invite: Invite? = null
    try {
        invite = Invite.fromCanonicalForm(inviteString)
    } catch (e: MalformedInviteCodeException) {
        e.printStackTrace();
        return
    }

    val identListViewModel = koinViewModel<IdentListViewModel>()
    var aliasInput by remember { mutableStateOf("") }
    var serverInput by remember { mutableStateOf(invite.host) }
    var portInput by remember { mutableStateOf(invite.port.toString()) }
    var isValid = (aliasInput.length > 1) && (serverInput.length > 1) && (portInput.length == 4)
    var defaultServer by remember { mutableStateOf(true) }

    val responseState by identListViewModel.insertedIdent.observeAsState(null)

    if (responseState != null) {
        val identAndAboutViewModel =
            koinViewModel<IdentAndAboutViewModel>(parameters = { parametersOf(responseState!!.oid) })
        LaunchedEffect(responseState) {
            identAndAboutViewModel.setCurrentIdent(responseState!!.oid.toString())
        }
        if (identAndAboutViewModel.identAndAbout == null) {
            return
        }
        identAndAboutViewModel.identAndAbout!!.ident.invite?.let {
            identAndAboutViewModel.connectWithInvite(
                identAndAboutViewModel.identAndAbout!!.ident
            ) {
                navController.navigate("${Scenes.FeedList.route}")
            }
        }
        return
    }

    Card(
        elevation = CardDefaults.cardElevation(),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
    ) {

        Column(
            modifier = Modifier
                .padding(12.dp, 8.dp)
                .fillMaxWidth()
        ) {
            OutlinedTextField(
                value = aliasInput,
                onValueChange = {
                    aliasInput = it
                },
                label = {
                    Text(
                        text = stringResource(id = R.string.please_alias),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alias")
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                // server
                OutlinedTextField(
                    value = serverInput,
                    onValueChange = {
                        serverInput = it
                    },
                    label = {
                        Text(
                            text = stringResource(id = R.string.server),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    modifier = Modifier.weight(0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = portInput,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    onValueChange = {
                        portInput = it
                    },
                    label = {
                        Text(
                            text = stringResource(id = R.string.port),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    modifier = Modifier.weight(0.3f)
                )
            }
            Row(modifier = Modifier.padding(start = 8.dp, end = 8.dp)) {
                Checkbox(
                    checked = defaultServer,
                    onCheckedChange = { defaultServer = !defaultServer },
                    colors = CheckboxDefaults.colors()
                )
                Text(
                    text = stringResource(R.string.default_ident),
                    //color= MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            Button(
                enabled = isValid,
                onClick = {
                    val ident = Ident(
                        oid =0,
                        publicKey = identity.toCanonicalForm(),
                        server = serverInput,
                        port = portInput.toInt(),
                        privateKey= identity.privateKeyAsBase64String(),
                        invite = inviteString,
                        sortOrder = 1,
                        defaultIdent = defaultServer
                    );
                    identListViewModel.insert(ident = ident)
                },
                content = { Text(stringResource(id = R.string.save)) }
            )
        }
    }
}
