package `in`.delog.ui.scene.identitifiers

import android.util.Log
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
import `in`.delog.ui.component.EditDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.tuweni.scuttlebutt.Identity
import kotlin.reflect.KFunction2

@Preview
@Composable
fun LoadIdentityPreview() {
    @SuppressWarnings
    fun mockReturn(pIdentity: Identity?, pInviteUrl: String?) {}
    LoadIdentity(callBack = ::mockReturn)
}

@Composable
fun LoadIdentity(callBack: KFunction2<Identity?, String?, Unit>) {

    val context = LocalContext.current
    val store = SettingStore(context)
    val inviteUrl = store.getData(INVITE_URL).collectAsState(initial = null)

    var showInviteUrlDialog by remember { mutableStateOf(false) }

    var getInvite: Boolean by remember { mutableStateOf(true) }

    var showMnemonicForm: Boolean by remember { mutableStateOf(false) }

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


    ElevatedCard(
        //elevation = CardDefaults.cardElevation(),
        //shape = RoundedCornerShape(0.dp),
        modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth()
    ) {

        Text(
            stringResource(R.string.load_new_identifier),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(12.dp)
        )

        Row(verticalAlignment = Alignment.Top,
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
            Column(
                //verticalArrangement = Arrangement.Center,
                //modifier = Modifier
                //    .wrapContentHeight()
                //    .background(Color.Red)
            ) {

                Text(
                    String.format(stringResource(R.string.get_invite_and_redeem_it),
                        inviteUrl.value),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(modifier = Modifier.size(24.dp))
        Row(
            modifier = Modifier.padding(16.dp)
        ) {

            ElevatedButton(
                modifier = Modifier.padding( end = 8.dp, top = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                onClick = {
                    showMnemonicForm = true
                },
                content = { Text(stringResource(R.string.restore_from_mnemonic)) }
            )

            ElevatedButton(
                modifier = Modifier.padding(start = 8.dp,  top = 8.dp),
                colors = ButtonDefaults.buttonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                onClick = {
                    callBack(Identity.random(), if (getInvite) inviteUrl.value else null)
                },
                content = { Text(stringResource(R.string.gen_rand_identifier)) }
            )


        }
    }

    if (showInviteUrlDialog) {
        EditDialog(
            title = R.string.edit_default_invite_url,
            value = if (inviteUrl.value==null) "" else inviteUrl.value!!,
            closeDialog = { showInviteUrlDialog = false },
            setValue = {
                CoroutineScope(Dispatchers.IO).launch {
                    Log.w("TEST","sabving !!!!!!! " + it)
                    store.saveData(INVITE_URL, it)
                    showInviteUrlDialog = false
                }
            }
        )
    }
}
