package `in`.delog.ui.scene.identitifiers

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
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
