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

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.accompanist.web.LoadingState
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import `in`.delog.ui.observeAsState
import org.apache.tuweni.scuttlebutt.Identity
import org.apache.tuweni.scuttlebutt.Invite
import org.apache.tuweni.scuttlebutt.MalformedInviteCodeException
import `in`.delog.R
import `in`.delog.db.model.Ident
import `in`.delog.ssb.SsbService
import `in`.delog.ui.navigation.Scenes
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.IdentListViewModel
import `in`.delog.viewmodel.IdentViewModel
import org.koin.androidx.compose.get
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun IdentNew(navController: NavHostController) {
    val startUrl = "http://192.168.0.45:8000/invite/";
    val bottomBarViewModel = koinViewModel<BottomBarViewModel>()

    LaunchedEffect(Unit) {
        bottomBarViewModel.setTitle("New identity")
        bottomBarViewModel.setActions { }
    }

    var invite: String? by remember { mutableStateOf(null) }

    var identity: Identity? by remember { mutableStateOf(null) }

    fun setInvite(s: String) {
        invite = s
    }

    fun setIdentity(s: Identity) {
        identity = s
    }

    if (identity == null) {
        LoadIdentity(navController = navController, ::setIdentity)
    } else {
        if (invite == null) {
            InviteWebRequest(startUrl, ::setInvite)
        } else {
            IdentNewEdit(navController, identity!!, invite!!)
        }
    }

}


@Composable
fun InviteWebRequest(startUrl: String, callBack: (String) -> Unit) {
    val webViewState = rememberWebViewState(startUrl)
    LaunchedEffect(webViewState.lastLoadedUrl) {
        if (webViewState.lastLoadedUrl != null) {
            if (webViewState.lastLoadedUrl!!.contains("?")) {
                val b64 = webViewState.lastLoadedUrl!!.split('?')[1].split("=")[1]
                val inv = Base64.decode(b64, Base64.DEFAULT).toString(charset("UTF-8"))
                callBack(inv)
            }
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        val loading = remember { mutableStateOf(true) }
        if (!(webViewState.loadingState is LoadingState.Finished) || loading.value) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    "contacting dmba.info ...",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(12.dp)
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator()
                }

            }
        }
        val webError = remember { mutableStateOf("") }
        if (webError.value == "") {
            WebView(
                modifier = Modifier
                    .fillMaxSize(),
                state = webViewState,
                onCreated = {
                    it.settings.javaScriptEnabled = true

                    it.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    it.webViewClient = object : WebViewClient() {

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon);
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            loading.value = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            loading.value = false
                            Log.e("webview", error.toString())
                            //loadURL = "file:///android_asset/404.html"
                            if (error != null) {
                                webError.value = error.description as String
                            }
                        }
                    }
                }
            )
        } else {
            Text(webError.value)
        }
    }


}

@Composable
fun LoadIdentity(navController: NavHostController, callBack: (Identity) -> Unit) {

    var showMnemonicForm: Boolean by remember { mutableStateOf(false) }

    if (showMnemonicForm) {
        MnemonicForm()
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
            "load new identity",
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
                content = { Text("Generate random identity") }
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

@Composable
fun MnemonicForm() {
    Text(
        "Under construction",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(12.dp)
    )
}


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

    val responseState by identListViewModel.identToNavigate.observeAsState(null)

    if (responseState != null) {
        val identViewModel =
            koinViewModel<IdentViewModel>(parameters = { parametersOf(responseState!!.oid) })
        LaunchedEffect(responseState) {
            identViewModel.setCurrentIdent(responseState!!.oid.toString())
        }
        if (identViewModel.ident == null) {
            return
        }
        val ssbService: SsbService = get()
        identViewModel.ident!!.invite?.let {
            identViewModel.connectWithInvite(
                identViewModel.ident!!,
                it,
                ssbService
            )
        }
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text("setup ended ok", style = MaterialTheme.typography.titleLarge)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {

                FilledIconButton(onClick = {
                    identListViewModel.reset()
                    navController.navigate("${Scenes.MainFeed.route}/${identViewModel.ident!!.publicKey}")
                },
                    content = { Text("start") }
                )
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
                modifier = Modifier.fillMaxWidth()
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
                        0,
                        identity.toCanonicalForm(),
                        serverInput,
                        portInput.toInt(),
                        identity.privateKeyAsBase64String(),
                        defaultServer,
                        aliasInput,
                        1,
                        inviteString
                    );
                    identListViewModel.insertAndNavigate(ident = ident, navController)

                },
                content = { Text("save") }
            )
        }
    }
}
