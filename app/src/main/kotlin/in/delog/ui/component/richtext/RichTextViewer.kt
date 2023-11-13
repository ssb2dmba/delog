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
package `in`.delog.ui.component.richtext

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.util.regex.Pattern
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import `in`.delog.ssb.BaseSsbService.Companion.TAG
import `in`.delog.ui.component.preview.url.LoadUrlPreview


val imageExtension: Pattern = Pattern.compile("(.*/)*.+\\.(png|jpg|gif|bmp|jpeg|webp|svg)$")
val videoExtension: Pattern = Pattern.compile("(.*/)*.+\\.(mp4|avi|wmv|mpg|amv|webm)$")
val tagIndex = Pattern.compile("\\#\\[([0-9]+)\\](.*)")
val hashTagsPattern: Pattern = Pattern.compile("#([^\\s!@#\$%^&*()=+./,\\[{\\]};:'\"?><]+)(.*)", Pattern.CASE_INSENSITIVE)
//val noProtocolUrlValidator: Pattern = Pattern.compile("^[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&//=]*)$")
@Stable
data class ImmutableListOfLists<T>(val lists: List<List<T>> = emptyList())

fun isValidURL(url: String?): Boolean {
    return try {
        URL(url).toURI()
        true
    } catch (e: MalformedURLException) {
        false
    } catch (e: URISyntaxException) {
        false
    }
}

fun isMarkdown(content: String): Boolean {
    return content.startsWith("> ") ||
            content.startsWith("# ") ||
            content.contains("##") ||
            content.contains("__") ||
            content.contains("```") ||
            content.contains("](")
}

@Composable
fun MeasureSpaceWidth(
    content: @Composable (measuredWidth: Dp) -> Unit
) {
    SubcomposeLayout { constraints ->
        val measuredWidth =
            subcompose("viewToMeasure", { Text(" ") })[0].measure(Constraints()).width.toDp()

        val contentPlaceable = subcompose("content") {
            content(measuredWidth)
        }[0].measure(constraints)
        layout(contentPlaceable.width, contentPlaceable.height) {
            contentPlaceable.place(0, 0)
        }
    }
}

@Composable
private fun NoProtocolUrlRenderer(word: SchemelessUrlSegment) {
    RenderUrl(word)
}

@Composable
private fun RenderUrl(segment: SchemelessUrlSegment) {
    ClickableUrl(segment.url, "https://${segment.url}")
    segment.extras?.let { it1 -> Text(it1) }
}

@Composable
private fun NormalWord(word: String, style: TextStyle) {
    BasicText(
        text = word,
        style = style
    )
}

@Composable
private fun ZoomableContentView(
    word: String,
    state: RichTextViewerState
) {
    state.imagesForPager[word]?.let {
        Box(modifier = Modifier.padding(vertical = 5.dp)) {
            `in`.delog.ui.component.preview.images.ZoomableContentView(
                it,
                state.imageList,
                roundedCorner = true
            )
        }
    }
}

@Composable
private fun RenderWordWithPreview(
    word: Segment,
    state: RichTextViewerState,
    style: TextStyle,
    nav: (String) -> Unit
) {
    when (word) {
        is ImageSegment -> ZoomableContentView(word.segmentText, state)
        is LinkSegment -> LoadUrlPreview(word.segmentText, word.segmentText)
        //is EmojiSegment -> RenderCustomEmoji(word.segmentText, state)
        //is EmailSegment -> ClickableEmail(word.segmentText)
        //is PhoneSegment -> ClickablePhone(word.segmentText)
        is HashTagSegment -> HashTag(word, nav)
        is SchemelessUrlSegment -> NoProtocolUrlRenderer(word)
        is RegularTextSegment -> NormalWord(word.segmentText, style)
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RenderRegular(
    content: String
) {
    val state by remember(content) {
        mutableStateOf(CachedRichTextParser.parseText(content))
    }

    val currentTextStyle = LocalTextStyle.current
    val currentTextColor = LocalContentColor.current

    val textStyle = remember(currentTextStyle, currentTextColor) {
        currentTextStyle.copy(
            lineHeight = 1.4.em,
            color = currentTextStyle.color.takeOrElse {
                currentTextColor
            }
        )
    }

    MeasureSpaceWidth() { spaceWidth ->
        Column() {

            // FlowRow doesn't work well with paragraphs. So we need to split them
            state.paragraphs.forEach { paragraph ->
                val direction = if (paragraph.isRTL) {
                    LayoutDirection.Rtl
                } else {
                    LayoutDirection.Ltr
                }

                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    FlowRow(
                        modifier = Modifier.align(if (paragraph.isRTL) Alignment.End else Alignment.Start),
                        horizontalArrangement = Arrangement.spacedBy(spaceWidth)
                    ) {
                        paragraph.words.forEach { word ->
                            RenderWordWithPreview(
                                word,
                                state,
                                textStyle,
                                {
                                    Log.d(TAG, "$it clicked!")
                                }

                            )
                        }
                    }
                }
            }
        }
    }


}

@Composable
fun RichTextViewer(
    content: String,
    onClickCallBack: () -> Unit,
    maxLines: Int = Int.MAX_VALUE
) {
    val uri = LocalUriHandler.current
    if (remember(content) { isMarkdown(content) }) {
        MarkdownText(
            maxLines = maxLines,
            linkColor = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            markdown = content,
            style = MaterialTheme.typography.bodyMedium,
            onClick = { onClickCallBack.invoke() },
            onLinkClicked = {
                runCatching {
                    uri.openUri(it)
                }
            }
        )
    } else {
        RenderRegular(content)
    }


}
