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
package `in`.delog.service.ssb

import `in`.delog.db.model.Ident
import `in`.delog.db.model.asKeyPair
import `in`.delog.repository.BlobRepository
import `in`.delog.service.ssb.BaseSsbService.Companion.format
import `in`.delog.viewmodel.BlobItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.sodium.SHA256Hash
import org.apache.tuweni.crypto.sodium.Signature

@Serializable
data class Mention(
    val link: String,
    val name: String,
    val type: String?=null,
    val size: Long?=null
)


@Serializable
data class SsbMessageContent(
    var text: String? = null,
    var type: String,
    val contentWarning: String? = null,
    val about: String? = null,
    val image: String? = null,
    val name: String? = null,
    var root: String? = null,
    var branch: String? = null,
    val description: String? = null,
    var mentions: Array<Mention>? = null
)
@Serializable
data class SsbSignableMessage(
    var previous: String?,
    var sequence: Long,
    var author: String,
    val timestamp: Long,
    var hash: String,
    val content: SsbMessageContent
)


@Serializable
data class SsbSignedMessage(
    var previous: String?,
    var sequence: Long,
    var author: String,
    var timestamp: Long,
    var key: String,
    var content: SsbMessageContent,
    var signature: String,
) {
    constructor(signable: SsbSignableMessage, sequence: Bytes) : this(
        previous = signable.previous,
        sequence = signable.sequence,
        author = signable.author,
        timestamp = signable.timestamp,
        key = signable.hash,
        content = signable.content,
        signature = sequence.toBase64String() + ".sig.ed25519"
    )
}

fun SsbSignableMessage.signMessage(feed: Ident): Bytes {
    val kp = feed.asKeyPair()
    return Signature.signDetached(
        Bytes.wrap(getMessageString(this).encodeToByteArray()),
        kp!!.secretKey()
    );
}

fun getMessageContentString(ssbMessageContent: SsbMessageContent): String {
    return format.encodeToString(
        SsbMessageContent.serializer(),
        ssbMessageContent
    )
}

fun getMessageContent(str: String): SsbMessageContent {
    if (str.isNullOrBlank()) {
        return SsbMessageContent(type = "post")
    }
    return Json.decodeFromString<SsbMessageContent>(
        SsbMessageContent.serializer(),
        str
    )
}

fun getMessageString(message: SsbSignableMessage): String {
    var message = format.encodeToString(
        SsbSignableMessage.serializer(),
        message
    )
    return message;
}

fun SsbSignedMessage.makeHash(): SHA256Hash.Hash? {
    val message = format.encodeToString(
        SsbSignedMessage.serializer(),
        this
    )
    return SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes.wrap(message.toByteArray())))
}
