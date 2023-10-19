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

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import dev.jeziellago.compose.markdowntext.MarkdownText
import `in`.delog.ssb.BaseSsbService.Companion.TAG
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.util.regex.Pattern

val imageExtension: Pattern = Pattern.compile("(.*/)*.+\\.(png|jpg|gif|bmp|jpeg|webp|svg)$")
val videoExtension: Pattern = Pattern.compile("(.*/)*.+\\.(mp4|avi|wmv|mpg|amv|webm)$")
val noProtocolUrlValidator: Pattern = Pattern.compile("^[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&//=]*)$")

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
    val uri = LocalUriHandler.current
        MarkdownText(
            linkColor = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            markdown = text,
            style = MaterialTheme.typography.bodyMedium,
            onClick = { onClickCallBack.invoke() },
            onLinkClicked = {
                runCatching {
                    uri.openUri(it)
                }
            }
        )

}
