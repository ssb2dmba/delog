package `in`.delog.ui.scene.identitifiers

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import `in`.delog.service.ssb.TorService
import org.koin.androidx.compose.get

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
        if (webViewState.loadingState !is LoadingState.Finished || loading.value) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    "contacting $startUrl",
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
                                webError.value = error.description as String
                            }
                        }
                    }
                }
            )

    }


}
