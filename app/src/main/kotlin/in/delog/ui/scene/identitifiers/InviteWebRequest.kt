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
package `in`.delog.ui.scene.identitifiers

import android.graphics.Bitmap
import android.os.Build
import android.util.Base64
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.google.accompanist.web.LoadingState
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState

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


    val loading = remember { mutableStateOf(true) }
    if (loading.value && (webViewState.loadingState is LoadingState.Loading
                || webViewState.loadingState is LoadingState.Initializing)
    ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        "contacting $startUrl " + webViewState.loadingState.toString(),
                        style = MaterialTheme.typography.titleMedium,
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

    if (""".*\.onion(/.*)?$""".toRegex().matches(startUrl)) {

        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            val proxyConfig: ProxyConfig = ProxyConfig.Builder()
                .addProxyRule("socks5://127.0.0.1:9050")
                .build()
            ProxyController.getInstance()
                .setProxyOverride(proxyConfig, { Runnable { } }, { })
        }
    }


    WebView(
        modifier = Modifier.fillMaxSize(),
        state = webViewState,
        onCreated = {
            it.settings.javaScriptEnabled = true

            it.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            it.webViewClient = object : WebViewClient() {

                override fun onPageStarted(
                    view: WebView?,
                    url: String?,
                    favicon: Bitmap?
                ) {
                    super.onPageStarted(view, url, favicon)
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
                    if (error != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            webError.value = error.description as String
                        } else {
                            webError.value = error.toString()
                        }
                    }
                }
            }
        }
    )
}






