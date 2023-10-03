package `in`.delog.ui.scene.identitifiers

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.delog.R
import `in`.delog.db.SettingStore
import `in`.delog.db.SettingStore.Companion.INVITE_URL
import `in`.delog.ui.CameraQrCodeScanner
import `in`.delog.ui.component.EditDialog
import `in`.delog.viewmodel.IdentListViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.io.Base64
import org.apache.tuweni.scuttlebutt.Identity
import org.koin.androidx.compose.koinViewModel
import kotlin.reflect.KFunction2

@Preview
@Composable
fun LoadIdentityPreview() {
    @SuppressWarnings
    val identListViewModel = koinViewModel<IdentListViewModel>()
    fun mockReturn(pIdentity: Identity?, pInviteUrl: String?) {}
    LoadIdentity(
        identListViewModel = identListViewModel,
        callBack = ::mockReturn
    )
}

@Composable
fun LoadIdentity(
    identListViewModel: IdentListViewModel,
    callBack: KFunction2<Identity?, String?, Unit>
) {

    val context = LocalContext.current
    val store = SettingStore(context)
    val inviteUrl = store.getData(INVITE_URL).collectAsState(initial = null)

    var showInviteUrlDialog by remember { mutableStateOf(false) }

    var getInvite: Boolean by remember { mutableStateOf(true) }

    var showMnemonicForm: Boolean by remember { mutableStateOf(false) }

    var showCameraScanner: Boolean by remember { mutableStateOf(false) }

    fun toastify(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    fun setPk(input: String) {
        if (input.length < 85) return;
        showCameraScanner = false
        val privateKey = input.split("@")[0]

        var identity: Identity?
        try {
            var keyPair = Signature.KeyPair.forSecretKey(
                Signature.SecretKey.fromBytes(
                    Base64.decode(privateKey)
                )
            )
            identity = Identity.fromKeyPair(keyPair)

        } catch (e: Exception) {
            toastify("${e.localizedMessage}: $input")
            return
        }
        // check if exists ...
        var pk = identity.toCanonicalForm()
        if (identListViewModel.idents.value!!.any { it.ident.publicKey == pk }) {
            var preexist =
                identListViewModel.idents.value!!.filter { it.ident.publicKey == pk }.first()
            toastify("This identity ${pk} already exists !")
            identListViewModel.setFeedAsDefaultFeed(preexist.ident)
        }
        callBack(identity, null)
    }

    if (showMnemonicForm) {
        MnemonicForm {
            if (it == null) {
                showMnemonicForm = false;
            } else {
                callBack(it, if (getInvite) inviteUrl.value else null)
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
                        inviteUrl.value
                    ),
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(16.dp))

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

                ElevatedButton(
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    onClick = {
                        callBack(Identity.random(), if (getInvite) inviteUrl.value else null)
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

                Spacer(modifier = Modifier.height(16.dp))
            }

        }
    }

    if (showInviteUrlDialog) {
        EditDialog(
            title = R.string.edit_default_invite_url,
            value = if (inviteUrl.value == null) "" else inviteUrl.value!!,
            closeDialog = { showInviteUrlDialog = false },
            setValue = {
                CoroutineScope(Dispatchers.IO).launch {
                    store.saveData(INVITE_URL, it)
                    showInviteUrlDialog = false
                }
            }
        )
    }
}
