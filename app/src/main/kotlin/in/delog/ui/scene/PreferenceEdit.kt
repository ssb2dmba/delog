package `in`.delog.ui.scene

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.navigation.NavHostController
import `in`.delog.R
import `in`.delog.db.SettingStore
import `in`.delog.db.SettingStore.Companion.ALWAYS_TOR_PROXY
import `in`.delog.db.SettingStore.Companion.SERVER_URL
import `in`.delog.db.SettingStore.Companion.TOR_SOCK_PROXY_PORT
import `in`.delog.ui.component.BottomBarButton
import `in`.delog.ui.component.BottomBarMainButton
import `in`.delog.ui.navigation.Scenes
import `in`.delog.viewmodel.BottomBarViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun PreferencesEdit(navController: NavHostController) {

    val context = LocalContext.current
    val store = SettingStore(context)
    val openDialog = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        PreferencesItemHeader(text = stringResource(R.string.network))
        PreferencesTextField(
            store,
            "default server",
            SERVER_URL
        )
        PreferencesItemHeader(text = stringResource(R.string.proxy_tor))
        PreferencesTextField(
            store,
            stringResource(R.string.tor_sock_proxy_port),
            TOR_SOCK_PROXY_PORT,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
        )
        PreferencesCheckBox(
            store = store,
            title = stringResource(R.string.always_uses_tor_proxy),
            key = ALWAYS_TOR_PROXY
        )
    }

    // bottom bar setup
    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()
    bottomBarViewModel.setActions {
        BottomBarButton(
            imageVector = Icons.Default.HomeRepairService,
            onClick = {
                openDialog.value = true
            },
            contentDescription = "reset"
        )
        Spacer(modifier = Modifier.weight(1f))
        BottomBarMainButton(
            onClick = {
                navController.navigate(Scenes.MainFeed.route){
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }

                      },
            text = "quit"
        )
    }

    // confirm dialog setup
    if (openDialog.value) {
        AlertDialog(
            title = {
                Text(text = "Reset preferences")
            },
            text = {
                Text("Reset preference to application defaults")
            },
            onDismissRequest = { openDialog.value = false },
            confirmButton = {
                Button(
                    onClick = {
                        GlobalScope.launch {
                            store.reset()
                            openDialog.value = false
                        }

                    }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        openDialog.value = false
                    }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@Composable
private fun PreferencesCheckBox(
    store: SettingStore,
    title: String,
    key: Preferences.Key<String>
) {
    val storeState = store.getData(key).collectAsState(initial = null)
    if (storeState.value.isNullOrEmpty()) {
        return
    }
    var dirty by remember { mutableStateOf(false) }
    var innerValue = remember { mutableStateOf(storeState.value == "1") }
    Row(
        Modifier
            .padding(horizontal = 28.dp)
            .fillMaxWidth()
            .clickable {
                if (!dirty) dirty = true
            }, verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = innerValue.value,
            onCheckedChange = { innerValue.value = it },
            enabled = dirty,
            modifier = Modifier.clickable {
                if (!dirty) dirty = true
            }
        )
        Text(title, modifier = Modifier.clickable {
            dirty = true
        })
        if (dirty) {
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        innerValue?.let {
                            store.saveData(
                                key, if (innerValue.value) {
                                    "1"
                                } else {
                                    "0"
                                }
                            )
                            dirty = false
                        }

                    }
                }
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = stringResource(id = R.string.save),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun PreferencesTextField(
    store: SettingStore,
    title: String,
    key: Preferences.Key<String>,
    keyboardOptions: KeyboardOptions? = null
) {
    val storeState = store.getData(key).collectAsState(initial = null)
    if (storeState.value == null) {
        return
    }
    var dirty by remember { mutableStateOf(false) }
    var innerValue = remember { mutableStateOf(storeState.value) }
    if (dirty) {
        val kOpts = keyboardOptions ?: KeyboardOptions.Default
        TextField(
            modifier = Modifier.padding(horizontal = 28.dp),
            label = { Text(title) },
            value = innerValue.value!!,
            keyboardOptions = kOpts,
            onValueChange = {
                innerValue.value = it
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            innerValue?.let { store.saveData(key, innerValue.value!!) }
                            dirty = false
                        }
                    }
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = stringResource(id = R.string.save),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        )
    } else {
        Column(modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 28.dp),
            )
            Text(
                text = storeState.value!!,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .clickable {
                        dirty = true
                    }
            )
        }

    }
}

@Composable
private fun PreferencesItemHeader(text: String) {
    Box(
        modifier = Modifier
            .heightIn(min = 52.dp)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

