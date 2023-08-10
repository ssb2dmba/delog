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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.util.regex.Pattern

val imageExtension = Pattern.compile("(.*/)*.+\\.(png|jpg|gif|bmp|jpeg|webp|svg)$")
val videoExtension = Pattern.compile("(.*/)*.+\\.(mp4|avi|wmv|mpg|amv|webm)$")
val noProtocolUrlValidator = Pattern.compile("^[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&//=]*)$")

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

@Composable
fun RichTextViewer(text: String, onClickCallBack: () -> Unit) {
    var text1 = text.trim()
    var text2 = ""
    var url = ""
    var urlLen = 0
    Column(modifier = Modifier.padding(top = 5.dp)) {
        var i = 0
        text?.split('\n')?.forEach {paragraph: String ->
            if (android.util.Patterns.WEB_URL.matcher(paragraph).matches()) {
                text1 = text.substring(0, i)
                urlLen = paragraph.length
                url = text.substring(i, i+ urlLen +1)
                return@forEach;
            }
            i = i + paragraph.length + 1
        }
        if (i<text.length) {
            text2 = text.substring(text1.length + urlLen, text.length)
        }
        MarkdownText(
            onClick = onClickCallBack,
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            markdown = text1,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Justify
        )
        if (url!="") {
            UrlPreview(url = url , urlText = url )
            MarkdownText(
                onClick = onClickCallBack,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                markdown = text2,
                textAlign = TextAlign.Justify
            )
        }
    }
}
