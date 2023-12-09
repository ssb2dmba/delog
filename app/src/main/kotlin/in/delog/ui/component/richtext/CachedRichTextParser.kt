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
import android.util.LruCache
import android.util.Patterns
import androidx.compose.runtime.Immutable
import com.linkedin.urls.detection.UrlDetector
import com.linkedin.urls.detection.UrlDetectorOptions
import `in`.delog.ui.component.preview.images.ZoomableUrlContent
import `in`.delog.ui.component.preview.images.ZoomableUrlImage
import `in`.delog.ui.component.preview.images.ZoomableUrlVideo
import `in`.delog.ui.component.preview.images.imageExtensions
import `in`.delog.ui.component.preview.images.videoExtensions
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import java.util.regex.Pattern

@Immutable
data class RichTextViewerState(
    val urlSet: ImmutableSet<String>,
    val imagesForPager: ImmutableMap<String, ZoomableUrlContent>,
    val imageList: ImmutableList<ZoomableUrlContent>,
    val paragraphs: ImmutableList<ParagraphState>
)

data class ParagraphState(val words: ImmutableList<Segment>, val isRTL: Boolean)

object CachedRichTextParser {
    val richTextCache = LruCache<String, RichTextViewerState>(200)

    fun parseText(content: String): RichTextViewerState {
        return if (richTextCache[content] != null) {
            richTextCache[content]
        } else {
            val newUrls = RichTextParser().parseText(content)
            richTextCache.put(content, newUrls)
            newUrls
        }
    }
}

val HTTPRegex =
    "^((http|https)://)?([A-Za-z0-9-_]+(\\.[A-Za-z0-9-_]+)+)(:[0-9]+)?(/[^?#]*)?(\\?[^#]*)?(#.*)?".toRegex(
        RegexOption.IGNORE_CASE
    )

class RichTextParser() {
    fun parseText(
        content: String
    ): RichTextViewerState {
        val urls = UrlDetector(content, UrlDetectorOptions.Default).detect()

        val urlSet = urls.mapNotNullTo(LinkedHashSet(urls.size)) {
            // removes e-mails
            if (Patterns.EMAIL_ADDRESS.matcher(it.originalUrl).matches()) {
                null
            } else if (isNumber(it.originalUrl)) {
                null
            } else if (it.originalUrl.contains("。")) {
                null
            } else {
                if (HTTPRegex.matches(it.originalUrl)) {
                    it.originalUrl
                } else {
                    null
                }
            }
        }

        val imagesForPager = urlSet.mapNotNull { fullUrl ->
            val removedParamsFromUrl = fullUrl.split("?")[0].lowercase()
            if (imageExtensions.any { removedParamsFromUrl.endsWith(it) }) {
                ZoomableUrlImage(fullUrl)
            } else if (videoExtensions.any { removedParamsFromUrl.endsWith(it) }) {
                ZoomableUrlVideo(fullUrl)
            } else {
                null
            }
        }.associateBy { it.url }
        val imageList = imagesForPager.values.toList()


        val segments = findTextSegments(content, imagesForPager.keys, urlSet)

        return RichTextViewerState(
            urlSet.toImmutableSet(),
            imagesForPager.toImmutableMap(),
            imageList.toImmutableList(),
            segments
        )
    }

    private fun findTextSegments(
        content: String,
        images: Set<String>,
        urls: Set<String>
    ): ImmutableList<ParagraphState> {
        var paragraphSegments = persistentListOf<ParagraphState>()

        content.split('\n').forEach { paragraph ->
            var segments = persistentListOf<Segment>()
            var isDirty = false

            val isRTL = isArabic(paragraph)

            val wordList = paragraph.trimEnd().split(' ')
            wordList.forEach { word ->
                val wordSegment = wordIdentifier(word, images, urls)
                if (wordSegment !is RegularTextSegment) {
                    isDirty = true
                }
                segments = segments.add(wordSegment)
            }

            val newSegments = if (isDirty) {
                ParagraphState(segments, isRTL)
            } else {
                ParagraphState(persistentListOf<Segment>(RegularTextSegment(paragraph)), isRTL)
            }

            paragraphSegments = paragraphSegments.add(newSegments)
        }

        return paragraphSegments
    }

    fun isNumber(word: String): Boolean {
        return numberPattern.matcher(word).matches()
    }

    fun isDate(word: String): Boolean {
        return shortDatePattern.matcher(word).matches() || longDatePattern.matcher(word).matches()
    }

    private fun isArabic(text: String): Boolean {
        return text.any { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' }
    }

    private fun wordIdentifier(word: String, images: Set<String>, urls: Set<String>): Segment {
        val emailMatcher = Patterns.EMAIL_ADDRESS.matcher(word)
        return if (word.isEmpty()) {
            RegularTextSegment(word)
        } else if (images.contains(word)) {
            ImageSegment(word)
        } else if (urls.contains(word)) {
            LinkSegment(word)
        } else if (emailMatcher.matches()) {
            EmailSegment(word)
        } else if (word.startsWith("#")) {
            parseHash(word)
        } else {
            RegularTextSegment(word)
        }
    }

    private fun parseHash(word: String): Segment {
        val hashtagMatcher = hashTagsPattern.matcher(word)

        try {
            if (hashtagMatcher.find()) {
                val hashtag = hashtagMatcher.group(1)
                if (hashtag != null) {
                    return HashTagSegment(word, hashtag, hashtagMatcher.group(2))
                }
            }
        } catch (e: Exception) {
            Log.e("Hashtag Parser", "Couldn't link hashtag $word", e)
        }

        return RegularTextSegment(word)
    }

    companion object {
        val longDatePattern: Pattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$")
        val shortDatePattern: Pattern = Pattern.compile("^\\d{2}-\\d{2}-\\d{2}$")
        val numberPattern: Pattern = Pattern.compile("^(-?[\\d.]+)([a-zA-Z%]*)$")
    }
}

@Immutable
open class Segment(val segmentText: String)

@Immutable
class ImageSegment(segment: String) : Segment(segment)

@Immutable
class LinkSegment(segment: String) : Segment(segment)

@Immutable
class EmailSegment(segment: String) : Segment(segment)


@Immutable
open class HashIndexSegment(segment: String, val hex: String, val extras: String?) :
    Segment(segment)

@Immutable
class HashTagSegment(segment: String, val hashtag: String, val extras: String?) : Segment(segment)

@Immutable
class SchemelessUrlSegment(segment: String, val url: String, val extras: String?) : Segment(segment)

@Immutable
class RegularTextSegment(segment: String) : Segment(segment)
