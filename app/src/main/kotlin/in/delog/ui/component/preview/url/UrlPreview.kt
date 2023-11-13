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

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import `in`.delog.ui.component.richtext.ClickableUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
fun UrlPreview(url: String, urlText: String) {
    var urlPreviewState by remember(url) {
        mutableStateOf(
            UrlCachedPreviewer.cache.get(url)?.let { it } ?: UrlPreviewState.Loading
        )
    }

    // Doesn't use a viewModel because of viewModel reusing issues (too many UrlPreview are created).
    if (urlPreviewState == UrlPreviewState.Loading) {
        LaunchedEffect(url) {
            launch(Dispatchers.IO) {
                UrlCachedPreviewer.previewInfo(url) {
                    launch(Dispatchers.Main) {
                        urlPreviewState = it
                    }
                }
            }
        }
    }

    Crossfade(targetState = urlPreviewState, animationSpec = tween(durationMillis = 100)) { state ->
        when (state) {
            is UrlPreviewState.Loaded -> {
                UrlPreviewCard(url, state.previewInfo)
            }
            else -> {
                ClickableUrl(urlText, url)
            }
        }
    }
}

