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
package org.apache.tuweni.scuttlebutt.lib

import android.util.Log
import androidx.lifecycle.LifecycleService
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import `in`.delog.db.model.About
import `in`.delog.db.model.Message
import `in`.delog.db.model.toJsonResponse
import `in`.delog.db.repository.AboutRepository
import `in`.delog.db.repository.MessageRepository
import `in`.delog.service.ssb.SsbService
import `in`.delog.service.ssb.SsbService.Companion.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.scuttlebutt.lib.model.FeedMessage
import org.apache.tuweni.scuttlebutt.lib.model.ScuttlebuttMessageContent
import org.apache.tuweni.scuttlebutt.lib.model.toAbout
import org.apache.tuweni.scuttlebutt.lib.model.toMessage
import org.apache.tuweni.scuttlebutt.rpc.RPCAsyncRequest
import org.apache.tuweni.scuttlebutt.rpc.RPCCodec
import org.apache.tuweni.scuttlebutt.rpc.RPCFlag
import org.apache.tuweni.scuttlebutt.rpc.RPCFunction
import org.apache.tuweni.scuttlebutt.rpc.RPCMessage
import org.apache.tuweni.scuttlebutt.rpc.RPCResponse
import org.apache.tuweni.scuttlebutt.rpc.RPCStreamRequest
import org.apache.tuweni.scuttlebutt.rpc.RPCStreamRequest2
import org.apache.tuweni.scuttlebutt.rpc.mux.ConnectionClosedException
import org.apache.tuweni.scuttlebutt.rpc.mux.RPCHandler
import org.apache.tuweni.scuttlebutt.rpc.mux.ScuttlebuttStreamHandler
import java.util.*

/**
 * A service for operations that concern scuttlebutt feeds.
 *
 * Should be accessed via a ScuttlebuttClient instance.
 *
 * @param multiplexer the RPC request multiplexer to make requests with.
 */
class FeedService(
    private val multiplexer: RPCHandler,
    private val aboutRepository: AboutRepository,
    private val messageRepository: MessageRepository) : LifecycleService() {
    companion object {
        private val objectMapper = ObjectMapper()
    }
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)
    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    /**
     * Publishes a message to the instance's own scuttlebutt feed, assuming the client established the connection using
     * keys authorising it to perform this operation.
     *
     * @param content the message to publish to the feed
     * @param <T> the content published should extend ScuttlebuttMessageContent to ensure the 'type' field is a String
     * @return the newly published message, asynchronously
     *
     * @throws JsonProcessingException if 'content' could not be marshalled to JSON.
    </T> */
    @Throws(JsonProcessingException::class)
    suspend fun <T : ScuttlebuttMessageContent?> publish(content: T): FeedMessage {
        val jsonNode = objectMapper.valueToTree<JsonNode>(content)
        val asyncRequest = RPCAsyncRequest(RPCFunction("publish"), listOf<Any>(jsonNode))
        val response = multiplexer.makeAsyncRequest(asyncRequest)
        return response.asJSON(
            objectMapper,
            FeedMessage::class.java
        )
    }


    @Throws(JsonProcessingException::class, ConnectionClosedException::class)
    fun createHistoryStream(pk: String, sequence: Long) {

        val params = HashMap<String, Any>()
        params["id"] = pk
        params["seq"] = sequence
        params["limit"] = 100
        params["keys"] = true
        params["live"] = true
        Log.i(TAG, "createHistoryStream: $params")
        val streamRequest = RPCStreamRequest(RPCFunction("createHistoryStream"), listOf(params))

        val streamEnded = AsyncResult.incomplete<Void>()
        multiplexer.openStream(
            streamRequest
        ) { _: Runnable ->
            object : ScuttlebuttStreamHandler {

                override fun onMessage(requestNumber: Int, message: RPCResponse) {
                    routeFeedMessage(message)
                }

                override fun onStreamEnd() {
                    streamEnded.complete(null)
                }

                override fun onStreamError(ex: Exception) {
                    streamEnded.completeExceptionally(ex)
                }
            }
        }
    }

    fun routeFeedMessage(message: RPCResponse) {
        val m = message.asJSON(objectMapper, FeedMessage::class.java)
        if (m.type.isPresent) {
            when (m.type.get()) {
                "post" -> storePostMessage(m)
                "vote" -> storeVoteMessage(m)
                "contact" -> storeContactMessage(m)
                "about" -> storeAboutMessage(m)
                else -> println("not implemented:" + m.type.get())
            }
        } else {
            Log.w(TAG, "type not present: $m")
        }
    }

    private fun storeVoteMessage(m: FeedMessage) {

    }

    private fun storeContactMessage(m: FeedMessage) {

    }

    private fun storeAboutMessage(m: FeedMessage) {
        val about: About? = m.toAbout()
        if (about != null) {
            aboutRepository.insertOrUpdate(about)
        } else {
            Log.w(TAG, "unable to decode %s %s".format(m.key, m.value.contentAsString))
        }
    }

    private fun storePostMessage(m: FeedMessage) {
        val message: Message = m.toMessage()
        scope.launch {
            messageRepository.maybeAddMessage(message)
        }
    }

    fun onCreateHistoryStream(rpcMessage: RPCMessage) {
        val rpcStreamRequest = rpcMessage.asJSON(SsbService.objectMapper, RPCStreamRequest2::class.java)
        val id = rpcStreamRequest.id
        val sequence = 0 // rpcStreamRequest.seq
        val remoteLimit = rpcStreamRequest.limit

        if (sequence < 1) {
            Log.w(TAG, String.format("pub is requesting the whole history: %s", sequence))
        }
        var remoteSequence = sequence.toLong()
        val batchSize = 100.coerceAtMost(remoteLimit) // TODO put in config
        var hasMoreResults = true
        var ct = 0
        while (hasMoreResults) {
            val messages = messageRepository.getMessagePage(id, remoteSequence, batchSize)
            if (messages.isEmpty()) {
                Log.w(TAG, "db return empty: $ct")
                hasMoreResults = false
            }
            for (m: Message in messages) {
                Log.d(TAG, "> [${rpcMessage.requestNumber()}] :" + m)
                val response = RPCCodec.encodeResponse(
                    Bytes.wrap(m.toJsonResponse(SsbService.format)),
                    rpcMessage.requestNumber(),
                    RPCFlag.Stream.STREAM,
                    RPCFlag.BodyType.JSON
                )
                multiplexer.sendBytes(response)
                // increment for next query
                remoteSequence = m.sequence
                // increment for remote limit & protection
                ct++
                updateLastPush(m)
            }

        }
        //Log.d(TAG, "sending endstream for: " + rpcMessage.requestNumber())
        //multiplexer.endStream(rpcMessage.requestNumber() * -1)
    }

    private fun updateLastPush(it: Message) {
        // TODO
        // take it.author feed in database and set lastPush to it.sequence (if lower than)
    }


}