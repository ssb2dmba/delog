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
package `in`.delog.ui.component.preview.url


import android.util.LruCache
import androidx.compose.runtime.Stable

@Stable
object UrlCachedPreviewer {
    var cache = LruCache<String, UrlPreviewState>(100)
        private set

    fun previewInfo(url: String, onReady: (UrlPreviewState) -> Unit) {
        checkNotInMainThread()

        cache[url]?.let {
            onReady(it)
            return
        }

        BahaUrlPreview(
            url,
            object : IUrlPreviewCallback {
                override fun onComplete(urlInfo: UrlInfoItem) {
                    cache[url]?.let {
                        if (it is UrlPreviewState.Loaded || it is UrlPreviewState.Empty) {
                            onReady(it)
                            return
                        }
                    }

                    val state = if (urlInfo.allFetchComplete() && urlInfo.url == url) {
                        UrlPreviewState.Loaded(urlInfo)
                    } else {
                        UrlPreviewState.Empty
                    }

                    cache.put(url, state)
                    onReady(state)
                }

                override fun onFailed(throwable: Throwable) {
                    cache[url]?.let {
                        onReady(it)
                        return
                    }

                    val state = UrlPreviewState.Error(throwable.message ?: "Error Loading url preview")
                    cache.put(url, state)
                    onReady(state)
                }
            }
        ).fetchUrlPreview()
    }
}