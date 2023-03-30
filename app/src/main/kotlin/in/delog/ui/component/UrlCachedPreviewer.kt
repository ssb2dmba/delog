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
package `in`.delog.ui.component

import com.baha.url.preview.BahaUrlPreview
import com.baha.url.preview.IUrlPreviewCallback
import com.baha.url.preview.UrlInfoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import `in`.delog.ssb.BaseSsbService.Companion.format

object UrlCachedPreviewer {
    var cache = mapOf<String, UrlInfoItem>()
        private set
    var failures = mapOf<String, Throwable>()
        private set

    fun previewInfo(url: String, callback: IUrlPreviewCallback? = null) {
        cache[url]?.let {
            callback?.onComplete(it)
            return
        }

        failures[url]?.let {
            callback?.onFailed(it)
            return
        }

        val scope = CoroutineScope(Job() + Dispatchers.IO)
        scope.launch {
            BahaUrlPreview(url, object : IUrlPreviewCallback {
                override fun onComplete(urlInfo: UrlInfoItem) {
                    cache = cache + Pair(url, urlInfo)
                    callback?.onComplete(urlInfo)
                }

                override fun onFailed(throwable: Throwable) {
                    failures = failures + Pair(url, throwable)
                    callback?.onFailed(throwable)
                }
            }).fetchUrlPreview()
        }
    }

    fun findUrlsInMessage(message: String): List<String> {
        return message.split('\n').map { paragraph ->
            paragraph.split(' ').filter { word: String ->
                isValidURL(word) || noProtocolUrlValidator.matcher(word).matches()
            }
        }.flatten()
    }

    fun preloadPreviewsFor(messageViewData: MessageViewData) {
        messageViewData.content(format).text?.let { it0 ->
            findUrlsInMessage(it0).forEach {
                val removedParamsFromUrl = it.split("?")[0].toLowerCase()
                if (imageExtension.matcher(removedParamsFromUrl).matches()) {
                    // Preload Images? Isn't this too heavy?
                } else if (videoExtension.matcher(removedParamsFromUrl).matches()) {
                    // Do nothing for now.
                } else if (isValidURL(removedParamsFromUrl)) {
                    previewInfo(it)
                } else {
                    previewInfo("https://${it}")
                }
            }

        }

    }
}