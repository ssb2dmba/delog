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

import `in`.delog.db.model.Draft
import `in`.delog.db.model.Message
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

// @See https://scuttlebot.io/docs/message-types/post.html
@kotlinx.serialization.Serializable
data class MessageContent(
    val text: String? = null,
    val type: String? = null,
    val root: String? = null,
    val branch: String? = null,
    val mentions: List<String>? = emptyList()
)

data class MessageViewData(
    val key: String,
    val timestamp: Long,
    val author: String,
    val contentAsText: String,
    var authorName: String? = null,
    var authorImage: String? = null,
    var root: String? = null,
    var branch: String? = null,
    var pName: String? = null,
)

fun Message.toMessageViewData() = MessageViewData(
    key = key,
    timestamp = timestamp,
    author = author,
    contentAsText = contentAsText,
    root= root,
    branch = branch
)


fun Draft.toMessageViewData() = MessageViewData(
    key = "",
    timestamp = timestamp,
    author = author,
    contentAsText = contentAsText
)

fun MessageViewData.content(format: Json): MessageContent {
    try {
        return format.decodeFromString<MessageContent>(
            MessageContent.serializer(),
            this.contentAsText
        )
    } catch (e: SerializationException) {
        // TODO incomming .box message here
        // at insert in db with type text that should not happen
        return MessageContent(this.contentAsText, "error");
    }
}