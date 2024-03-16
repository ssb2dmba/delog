package `in`.delog.ui.scene.identitifiers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import `in`.delog.R
import `in`.delog.db.model.Ident
import `in`.delog.ui.component.TextError
import `in`.delog.ui.navigation.Scenes
import `in`.delog.ui.observeAsState
import `in`.delog.viewmodel.IdentListViewModel
import org.apache.tuweni.scuttlebutt.Identity
import org.apache.tuweni.scuttlebutt.Invite
import org.apache.tuweni.scuttlebutt.MalformedInviteCodeException
import org.koin.androidx.compose.koinViewModel

@Preview
@Composable
fun previewIdentNewEdit() {
    val inviteString ="udwhjyymzan454unyeirqbicgarsg4w3q664iadehiss4ek5gystc4ad.onion:8008:@u64QhYoJN4EKUtXi/T1hVvVYf+Rqm/t50rvNUFsXVK8=.ed25519~86Dn5SUBAuTbzJARsgV99LBu/qb4dxMoJZYSR5HcRzk="
    val ident = Ident(
        oid = 0,
        publicKey = "@8bKCopZL2rilN7rPgd7/IyKj0RYPcilWsqaezvkGFRU=.ed25519",
        server = "udwhjyymzan454unyeirqbicgarsg4w3q664iadehiss4ek5gystc4ad.onion",
        port = 8008,
        privateKey = "8CcQUI27IE+Rjj7sZ4Q9njjqcB0vizqstNYGVux/ehJilJsTn/uha5/uTrOFT/DBubbwR99SCBgTBWkOQ4B0iA==",
        invite = inviteString,
        sortOrder = 1,
        defaultIdent = true,
        lastPush = null
    )
    InnerNewIdentNewEdit(ident) { ident, alias -> }
}


@Composable
fun InnerNewIdentNewEdit(ident: Ident,
                         callback: (ident: Ident, alias: String) -> Unit
) {

    var aliasInput by remember { mutableStateOf("") }
    var serverInput by remember { mutableStateOf(ident.server) }
    var portInput by remember { mutableStateOf(ident.port.toString()) }
    val isValid = (aliasInput.length > 1) && (serverInput.length > 1) && (portInput.length == 4)
    var defaultServer by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    if (loading) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator()
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
                    loading = true
                    ident.defaultIdent = defaultServer
                    ident.server = serverInput
                    ident.port = portInput.toInt()
                    callback( ident,  aliasInput)
                },
                content = { Text(stringResource(id = R.string.save)) }
            )
        }
    }
}

@Composable
fun IdentNewEdit(navController: NavHostController, identity: Identity, inviteString: String) {
    val identListViewModel = koinViewModel<IdentListViewModel>()
    val newIdent by identListViewModel.insertedIdent.observeAsState(null)
    if (newIdent != null) {
        LaunchedEffect(key1 = Unit) {
            newIdent!!.invite?.let {
                navController.navigate(Scenes.FeedList.route) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            }
        }
        return
    }
    val invite: Invite?
    try {
        invite = Invite.fromCanonicalForm(inviteString)
    } catch (e: MalformedInviteCodeException) {
        Card{
            TextError("invite is malformed ! ${e.message}, $inviteString")
        }
        return
    }
    val ident = Ident(
        oid = 0,
        publicKey = identity.toCanonicalForm(),
        server = invite.host,
        port = invite.port,
        privateKey = identity.privateKeyAsBase64String(),
        invite = inviteString,
        sortOrder = 1,
        defaultIdent = true,
        lastPush = null
    )

    InnerNewIdentNewEdit(ident) { ident, alias ->
        identListViewModel.insert(ident = ident, alias)
    }

}
