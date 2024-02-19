package `in`.delog.ui.scene.identitifiers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.delog.R
import `in`.delog.db.model.Ident
import `in`.delog.ui.CameraQrCodeScanner
import `in`.delog.ui.component.EditDialog
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.io.Base64
import org.apache.tuweni.scuttlebutt.Identity
import kotlin.reflect.KFunction2

@Preview
@Composable
fun LoadIdentityPreview() {
    @SuppressWarnings
    fun mockReturn(pIdentity: Identity?, pInviteUrl: String?) {}
    LoadIdentity(
        serverUrl = "https://delog.in:8008",
        callBack = ::mockReturn
    )
}

@Composable
fun LoadIdentity(
    serverUrl: String,
    callBack: KFunction2<Identity?, String?, Unit>
) {


    if (serverUrl == null) return
    val serverName = serverUrl.split(":")[0]

    var showInviteUrlDialog by remember { mutableStateOf(false) }

    var getInvite: Boolean by remember { mutableStateOf(true) }

    var showMnemonicForm: Boolean by remember { mutableStateOf(false) }

    var showCameraScanner: Boolean by remember { mutableStateOf(false) }


    fun setPk(input: String) {
        if (input.length < 85) return
        showCameraScanner = false
        val privateKey = input.split("@")[0]

        val identity: Identity?
        try {
            val keyPair = Signature.KeyPair.forSecretKey(
                Signature.SecretKey.fromBytes(
                    Base64.decode(privateKey)
                )
            )
            identity = Identity.fromKeyPair(keyPair)

        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        callBack(identity, null)
    }

    if (showMnemonicForm) {
        MnemonicForm {
            if (it == null) {
                showMnemonicForm = false
            } else {
                val inviteUrl = Ident.getInviteUrl(serverName)
                callBack(it, if (getInvite) inviteUrl else null)
            }
        }
        return
    }

    if (showCameraScanner) {
        CameraQrCodeScanner(::setPk)
        return
    }

    ElevatedCard(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth()
    ) {

        Text(
            stringResource(R.string.load_new_identifier),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(12.dp)
        )

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(96.dp)
            ) {
                Checkbox(
                    modifier = Modifier.align(Alignment.TopCenter),
                    checked = getInvite,
                    onCheckedChange = { getInvite = !getInvite }
                )
                FilledTonalIconButton(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onClick = {
                        showInviteUrlDialog = true
                    }
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                }
            }
            Column {

                Text(
                    String.format(
                        stringResource(R.string.get_invite_and_redeem_it),
                        serverName
                    ),
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                ElevatedButton(
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    onClick = {
                        val inviteUrl = Ident.getInviteUrl(serverName)
                        callBack(Identity.random(), if (getInvite) inviteUrl else null)
                    },
                    content = { Text(stringResource(R.string.gen_rand_identifier)) }
                )


                ElevatedButton(
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    onClick = {

                        showCameraScanner = true
                    },
                    content = { Text(stringResource(R.string.fromQRCode)) }
                )

                ElevatedButton(
                    modifier = Modifier.padding(end = 8.dp, top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    onClick = {
                        showMnemonicForm = true
                    },
                    content = { Text(stringResource(R.string.restore_from_mnemonic)) }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

        }
    }

    if (showInviteUrlDialog) {
        EditDialog(
            title = R.string.edit_default_invite_url,
            value = serverName,
            closeDialog = { showInviteUrlDialog = false },
            setValue = {
//                CoroutineScope(Dispatchers.IO).launch {
//                    store.saveData(SERVER_URL, it)
//                    showInviteUrlDialog = false
//                }
            }
        )
    }
}
