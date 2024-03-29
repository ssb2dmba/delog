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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp


@Composable
fun HashTag(word: HashTagSegment, nav: (String) -> Unit) {
    RenderHashtag(word, nav)
}

@Composable
private fun InlineIcon(hashtagIcon: HashtagIcon) =
    InlineTextContent(
        Placeholder(
            width = 17.sp,
            height = 17.sp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
        )
    ) {
        Icon(
            painter = painterResource(hashtagIcon.icon),
            contentDescription = hashtagIcon.description,
            tint = hashtagIcon.color,
            modifier = hashtagIcon.modifier
        )
    }

@Composable
private fun RenderHashtag(
    segment: HashTagSegment,
    nav: (String) -> Unit
) {
    val primary = MaterialTheme.colorScheme.onTertiary
    val background = MaterialTheme.colorScheme.surfaceVariant
    val hashtagIcon = remember(segment.hashtag) {
        checkForHashtagWithIcon(segment.hashtag, primary)
    }

    val regularText = remember { SpanStyle(color = background) }
    val clickableTextStyle = remember { SpanStyle(color = primary) }

    val annotatedTermsString = remember {
        buildAnnotatedString {
            withStyle(clickableTextStyle) {
                pushStringAnnotation("routeToHashtag", "")
                append("#${segment.hashtag}")
            }

            if (hashtagIcon != null) {
                withStyle(clickableTextStyle) {
                    pushStringAnnotation("routeToHashtag", "")
                    appendInlineContent("inlineContent", "[icon]")
                }
            }

            segment.extras?.ifBlank { "" }?.let {
                withStyle(regularText) {
                    append(it)
                }
            }
        }
    }

    val inlineContent = if (hashtagIcon != null) {
        mapOf("inlineContent" to InlineIcon(hashtagIcon))
    } else {
        emptyMap()
    }

    val pressIndicator = remember {
        Modifier.clickable {
            nav("Hashtag/${segment.hashtag}")
        }
    }

    Text(
        text = annotatedTermsString,
        modifier = pressIndicator,
        inlineContent = inlineContent,
        fontWeight = FontWeight.Bold
    )
}