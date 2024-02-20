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

import `in`.delog.service.ssb.BaseSsbService.Companion.format
import kotlinx.serialization.Serializable
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.sodium.SHA256Hash


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

    fun makeHash(): SHA256Hash.Hash? {
        val message = format.encodeToString(
            serializer(),
            this
        )
        return SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes.wrap(message.toByteArray())))
    }
}


