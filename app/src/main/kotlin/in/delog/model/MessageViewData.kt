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
package `in`.delog.model

import android.util.Log
import `in`.delog.db.AppDatabaseView
import `in`.delog.db.model.Draft
import `in`.delog.db.model.Message
import `in`.delog.db.repository.BlobRepository
import `in`.delog.viewmodel.BlobItem
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

// @See https://scuttlebot.io/docs/message-types/post.html
@kotlinx.serialization.Serializable
data class MessageContent(
    val text: String? = null,
    val type: String? = null,
    val root: String? = null,
    val branch: String? = null,
    val mentions: Array<Mention>? = emptyArray()
)

data class MessageViewData(
    var oid: Long = -1L,
    val key: String,
    val timestamp: Long,
    val author: String,
    val contentAsText: String,
    var authorName: String? = null,
    var authorImage: String? = null,
    var pName: String? = null,
    var type: String? = null,
    var root: String? = null,
    var branch: String? = null,
    var blobs: Array<BlobItem> = arrayOf(),
    var links: Array<Mention> = arrayOf(),
    var replies:Long = 0,
    var level: Long = 0
    ) {
    companion object

}

fun MessageViewData.Companion.empty(author: String): MessageViewData {
    return MessageViewData(
        oid = 0L,
        key = "",
        timestamp = System.currentTimeMillis(),
        author = author,
        contentAsText = "",
        authorName = "",
        authorImage = "",
        pName = "",
        type = "",
        root = "",
        branch = "",
        blobs = arrayOf(),
        links = arrayOf()
    )
}

fun MessageViewData.serializeMessageContent(format: Json): MessageContent {
    return try {
        format.decodeFromString(
            MessageContent.serializer(),
            this.contentAsText
        )
    } catch (e: SerializationException) {
        Log.e("MessageViewData.toMessageContent", this.contentAsText  )
        e.printStackTrace()
        MessageContent(
            "error with: '${this.contentAsText}'",
            "post",
            "",
            "",
            emptyArray()
        )
    }
}

fun Message.toMessageViewData() = MessageViewData(
    key = key,
    timestamp = timestamp,
    author = author,
    contentAsText = contentAsText,
    root = root,
    branch = branch
)


suspend fun Draft.toMessageViewData(format: Json, blobRepository: BlobRepository): MessageViewData {
    val mvd = MessageViewData(
        oid = oid ?: 0L,
        key = "",
        timestamp = timestamp,
        author = author,
        type = type,
        contentAsText = contentAsText,
        root = root,
        branch = branch,
        links = arrayOf(),
        blobs = arrayOf()
    )
    val mc = mvd.serializeMessageContent(format)
    mvd.type = mc.type
    mvd.root = mc.root
    mvd.branch = mc.branch
    mvd.links = mc.mentions?.filter { it.link.startsWith("%") }?.toTypedArray() ?: arrayOf()
    mvd.blobs = mc.mentions?.filter { it.link.startsWith("&") }
        ?.map { blobRepository.getAsBlobItem(it.link) }?.toTypedArray() ?: arrayOf()
    return mvd
}

suspend fun AppDatabaseView.MessageInTree.toMessageViewData(format: Json, blobRepository: BlobRepository): MessageViewData {
    val mvd = MessageViewData(
        key = key,
        timestamp = timestamp,
        author = author,
        contentAsText = contentAsText,
        root = root,
        branch = branch,
        authorName = name,
        authorImage = image?.let { blobRepository.getAsBlobItem(it).uri.toString() },
        pName = pName,
        replies = replies,
        level = level,
        links = arrayOf(),
        blobs = arrayOf()
    )
    val mc = mvd.serializeMessageContent(format)
    mvd.type=mc.type
    mvd.root=mc.root
    mvd.branch=mc.branch
    mvd.links = mc.mentions?.filter { it.link.startsWith("%") }?.toTypedArray() ?: arrayOf()
    mvd.blobs = mc.mentions?.filter { it.link.startsWith("&") }
        ?.map { blobRepository.getAsBlobItem(it.link) }?.toTypedArray() ?: arrayOf()
    return mvd
}


fun MessageViewData.toDraft(): Draft {
    val mc = serializeMessageContent(Json)
    return Draft(
        oid = oid,
        author = author,
        timestamp = timestamp,
        type = mc.type ?: "post",
        contentAsText = contentAsText,
        root = mc.root,
        branch = mc.branch
    )
}