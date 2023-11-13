package `in`.delog.ui.component.preview.url

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import `in`.delog.ui.component.richtext.ClickableUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
fun LoadUrlPreview(url: String, urlText: String) {


    var urlPreviewState by remember(url) {
        mutableStateOf(
            UrlCachedPreviewer.cache.get(url) ?: UrlPreviewState.Loading
        )
    }

    // Doesn't use a viewModel because of viewModel reusing issues (too many UrlPreview are created).
    if (urlPreviewState == UrlPreviewState.Loading) {
        LaunchedEffect(url) {
            launch(Dispatchers.IO) {
                UrlCachedPreviewer.previewInfo(url) {
                    urlPreviewState = it
                }
            }
        }
    }

    Crossfade(
        targetState = urlPreviewState,
        animationSpec = tween(durationMillis = 100),
        label = "UrlPreview"
    ) { state ->
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
