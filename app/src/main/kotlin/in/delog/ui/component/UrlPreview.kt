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

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import com.baha.url.preview.IUrlPreviewCallback
import com.baha.url.preview.UrlInfoItem
import `in`.delog.ui.component.ClickableUrl
import `in`.delog.ui.component.UrlCachedPreviewer


@Composable
fun UrlPreview(url: String, urlText: String, showUrlIfError: Boolean = true) {
    var urlPreviewState by remember { mutableStateOf<UrlPreviewState>(UrlPreviewState.Loading) }

    // Doesn't use a viewModel because of viewModel reusing issues (too many UrlPreview are created).
    LaunchedEffect(url) {
        UrlCachedPreviewer.previewInfo(url, object : IUrlPreviewCallback {
            override fun onComplete(urlInfo: UrlInfoItem) {
                if (urlInfo.allFetchComplete() && urlInfo.url == url)
                    urlPreviewState = UrlPreviewState.Loaded(urlInfo)
                else
                    urlPreviewState = UrlPreviewState.Empty
            }

            override fun onFailed(throwable: Throwable) {
                urlPreviewState =
                    UrlPreviewState.Error("Error parsing preview for ${url}: ${throwable.message}")
            }
        })
    }

    Crossfade(targetState = urlPreviewState, animationSpec = tween(durationMillis = 100)) { state ->
        when (state) {
            is UrlPreviewState.Loaded -> {
                UrlPreviewCard(url, state.previewInfo)
            }
            else -> {
                if (showUrlIfError) {
                    ClickableUrl(urlText, url)
                }
            }
        }
    }

}

