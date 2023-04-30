package `in`.delog.ui.scene.identitifiers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import `in`.delog.R
import org.apache.tuweni.scuttlebutt.Identity

@Composable
fun LoadIdentity(navController: NavHostController, callBack: (Identity?) -> Unit) {

    var showMnemonicForm: Boolean by remember { mutableStateOf(false) }
    if (showMnemonicForm) {
        MnemonicForm {
            if (it == null) {
                showMnemonicForm = false;
            } else {
                callBack(it)
            }
        }
        return
    }

    Card(
        elevation = CardDefaults.cardElevation(),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier
            .padding(8.dp)
            .fillMaxSize()
    ) {
        Text(
            stringResource(R.string.load_new_identifier),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(12.dp)
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.size(24.dp))
            Button(
                onClick = {
                    callBack(Identity.random())
                },
                content = { Text(stringResource(R.string.gen_rand_identifier)) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                modifier = Modifier.wrapContentWidth(),
                onClick = {
                    showMnemonicForm = true
                },
                content = { Text("Restore from mnemonic") }
            )
        }
    }
}
