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

import `in`.delog.db.model.Ident
import `in`.delog.db.model.asKeyPair
import `in`.delog.service.ssb.SsbService.Companion.format
import kotlinx.serialization.Serializable
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.sodium.Signature


@Serializable
class SsbSignableMessage(
    var previous: String?,
    var sequence: Long,
    var author: String,
    val timestamp: Long,
    var hash: String,
    val content: SsbMessageContent
) {
    private fun deserialize(): String {
        return format.encodeToString(
            serializer(),
            this
        )
    }

    fun signMessage(feed: Ident): Bytes {
        val kp = feed.asKeyPair()
        return Signature.signDetached(
            Bytes.wrap(this.deserialize().encodeToByteArray()),
            kp!!.secretKey()
        )
    }
}



