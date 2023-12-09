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
package `in`.delog.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import `in`.delog.service.ssb.SsbMessageContent
import kotlinx.serialization.json.Json

/**
This model adapted for room database represent an ssb message as described in the ssb protocol guide

Exemple message
{
"key": "%fH6ZETSgkMAvxbMO8aAz1h8rNLO4lKoWMTtmxZZag/A=.sha256",
"value": {
"previous": "%11J4JcYTzJy6a5Tlk9ZKxiCMQEupNuNs747Ktemo2d0=.sha256",
"sequence": 3,
"author": "@YpSbE5/7oWuf7k6zhU/wwbm28EffUggYEwVpDkOAdIg=.ed25519",
"timestamp": 1673170497023,
"hash": "sha256",
"content": {
"text": "NEWTEST 2",
"type": "post"
},
"signature": "KlEVtD4E221mJibhXuZCQ15BrsnNNHruepucHqvYnJVvw8UJgl5sL1QPGMATnP7KlkzM3SirUf4/19DkT+4sDQ==.sig.ed25519"
},
"timestamp": 1673170497024
}

 */
@Entity(primaryKeys = ["key"])
data class Message(

    @ColumnInfo(name = "key")
    var key: String,

    @ColumnInfo(name = "previous")
    var previous: String,

    @ColumnInfo(name = "sequence")
    var sequence: Long,

    @ColumnInfo(name = "author")
    var author: String,

    @ColumnInfo(name = "timestamp")
    var timestamp: Long,

    @ColumnInfo(name = "contentAsText")
    var contentAsText: String,

    @ColumnInfo(name = "signature")
    var signature: String,

    // in content but as column for better indexing
    @ColumnInfo(name = "type")
    var type: String?,

    @ColumnInfo(name = "root")
    var root: String?,

    @ColumnInfo(name = "branch")
    var branch: String?,

    ) {

}


@kotlinx.serialization.Serializable
class MessageBodyValueJSonResponse(
    val previous: String?,
    val sequence: Long,
    val author: String,
    val timestamp: Long,
    val hash: String,
    val content: SsbMessageContent,
    val signature: String
)

@kotlinx.serialization.Serializable
data class BodyMessageJsonContent(
    val key: String,
    val value: MessageBodyValueJSonResponse,
    val timestamp: Long
)

fun Message.toBodyMessageResponse(): BodyMessageJsonContent {
    var content: SsbMessageContent
    if (this.contentAsText.endsWith("box")) {
        content = SsbMessageContent(type = "box", text = contentAsText)
    } else {
        content = Json.decodeFromString<SsbMessageContent>(
            SsbMessageContent.serializer(),
            this.contentAsText
        )
    }

    val bodyMessageValueResponse = MessageBodyValueJSonResponse(
        previous = if (this.previous.equals("null")) null else this.previous,
        author = this.author,
        sequence = this.sequence,
        timestamp = this.timestamp,
        hash = "sha256",
        content = content,
        signature = this.signature
    )
    return BodyMessageJsonContent(
        key = this.key,
        value = bodyMessageValueResponse,
        timestamp = this.timestamp
    )
}

fun Message.toJsonResponse(format: Json): ByteArray {
    return format.encodeToString(
        BodyMessageJsonContent.serializer(),
        this.toBodyMessageResponse()
    ).encodeToByteArray()
}
