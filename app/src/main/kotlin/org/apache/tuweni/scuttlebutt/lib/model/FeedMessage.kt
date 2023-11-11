/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tuweni.scuttlebutt.lib.model

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import `in`.delog.db.model.About
import `in`.delog.db.model.Message
import `in`.delog.ssb.SsbMessageContent
import kotlinx.serialization.json.Json
import java.util.*

/**
 * A scuttlebutt feed message
 *
 * @param key the ID of the message
 * @param type the type of the content (is Empty if unknown because the message is private and not decryptable.)
 * @param value the metadata and contents of the message.
 */
@JsonDeserialize(using = FeedMessageDeserializer::class)
data class FeedMessage(val key: String, val type: Optional<String>, val value: FeedValue)

fun FeedMessage.toMessage(): Message {
    return Message(
        key = this.key,
        timestamp = this.value.timestamp,
        author = this.value.author.id,
        sequence = this.value.sequence,
        contentAsText = this.value.contentAsString,
        type = if (this.type.isPresent()) this.type.get() else null,
        previous = this.value.previous,
        signature = this.value.signature,
        root = getContentStringValue("root", this.value.content),
        branch = getContentStringValue("branch", this.value.content)
    )
}


fun FeedMessage.toAbout(): About? {
    val ssbMe: SsbMessageContent = Json.decodeFromString<SsbMessageContent>(
        SsbMessageContent.serializer(),
        this.value.contentAsString
    )
    return if (ssbMe.about == null) {
        null
    } else {
        About(
            about = ssbMe.about,
            description = ssbMe.description,
            image = ssbMe.image,
            name = ssbMe.name,
            dirty = false
        )
    }

}

fun getContentStringValue(key: String, content: JsonNode): String? {
    return if (content.has(key)) content.get(key).asText() else null
}
