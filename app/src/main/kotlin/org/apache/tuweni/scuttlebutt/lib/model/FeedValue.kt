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

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * The metadata and contents of a message
 *
 * @param previous the ID of the previous message on the append only feed, prior to this one
 * @param author the author of the posted message
 * @param sequence the sequence number of this message
 * @param timestamp the time the client asserts that the message was posted at
 * @param hash the hash type of the message
 * @param contentAsText the content of the message
 */
class FeedValue(
    val previous: String,
    val author: Author,
    val sequence: Long,
    val timestamp: Long,
    val hash: String,
    val content: JsonNode,
    val signature: String
) {

    companion object {
        private val mapper = ObjectMapper()
    }

    @get:Throws(JsonProcessingException::class)
    val contentAsString: String
        get() = mapper.writeValueAsString(this.content)

}
