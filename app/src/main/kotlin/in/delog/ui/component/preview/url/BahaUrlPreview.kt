package `in`.delog.ui.component.preview.url

import android.net.Uri
import android.os.Looper
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

interface IUrlPreviewCallback {
    fun onComplete(urlInfo: UrlInfoItem)
    fun onFailed(throwable: Throwable)
}

fun checkNotInMainThread() {
    if (isMainThread()) throw Exception("It should not be in the MainThread")
}

fun isMainThread() = Looper.myLooper() == Looper.getMainLooper()
class BahaUrlPreview(val url: String, var callback: IUrlPreviewCallback?) {
    val scope = CoroutineScope(Job() + Dispatchers.IO)
    private val imageExtensionArray = arrayOf(".gif", ".png", ".jpg", ".jpeg", ".bmp", ".webp")

    fun fetchUrlPreview(timeOut: Int = 30000) {
        val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            callback?.onFailed(throwable)
        }
        scope.launch(exceptionHandler) {
            fetch(timeOut)
        }
    }

    private suspend fun fetch(timeOut: Int = 30000) {
        checkNotInMainThread()
        lateinit var urlInfoItem: UrlInfoItem
        if (checkIsImageUrl()) {
            urlInfoItem = UrlInfoItem(url = url, image = url)
        } else {
            val document = getDocument(url, timeOut)
            urlInfoItem = parseHtml(url, document)
        }
        callback?.onComplete(urlInfoItem)
    }

    private fun checkIsImageUrl(): Boolean {
        val uri = Uri.parse(url)
        var isImage = false
        for (imageExtension in imageExtensionArray) {
            if (uri.path != null && uri.path!!.toLowerCase(Locale.getDefault())
                    .endsWith(imageExtension)
            ) {
                isImage = true
                break
            }
        }
        return isImage
    }

    fun cleanUp() {
        scope.cancel()
        callback = null
    }
}