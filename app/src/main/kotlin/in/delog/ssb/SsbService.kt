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
package `in`.delog.ssb

import android.util.Log
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import `in`.delog.db.model.About
import `in`.delog.db.model.Ident
import `in`.delog.db.model.Message
import `in`.delog.db.model.toJsonResponse
import `in`.delog.repository.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.scuttlebutt.Invite
import org.apache.tuweni.scuttlebutt.lib.ScuttlebuttClient
import org.apache.tuweni.scuttlebutt.lib.ScuttlebuttClientFactory
import org.apache.tuweni.scuttlebutt.lib.model.FeedMessage
import org.apache.tuweni.scuttlebutt.lib.model.toAbout
import org.apache.tuweni.scuttlebutt.lib.model.toMessage
import org.apache.tuweni.scuttlebutt.rpc.*
import org.apache.tuweni.scuttlebutt.rpc.mux.ScuttlebuttStreamHandler


class SsbService(
    messageRepository: MessageRepository,
    contactRepository: ContactRepository,
    aboutRepository: AboutRepository
) :
    BaseSsbService() {
    val messageRepository = messageRepository
    val contactRepository = contactRepository
    val aboutRepository = aboutRepository


    suspend fun reconnect(pFeed: Ident) {
        Log.i(TAG, "reconnecting to %s %s".format(pFeed.server, pFeed.publicKey))
        try {
            super.connect(pFeed, ::onConnected)
        } catch (e: Exception) {
            Log.e(
                TAG,
                "error connecting to %s %s : %s ".format(
                    pFeed.server,
                    pFeed.publicKey,
                    e.message.toString()
                )
            )
        }
    }

    private fun onConnected() {
        val myFeed = this.toCanonicalForm()
        GlobalScope.launch {
            try {
                // let's check our backup, moved device
                var ourSequence = messageRepository.getLastSequence(myFeed)
                createHistoryStream(myFeed, ourSequence)
                // let's call all of our friends
                contactRepository.geContacts(myFeed).forEach { // todo implement multiserver
                    ourSequence = messageRepository.getLastSequence(it.follow)
                    createHistoryStream(it.follow, ourSequence)
                }
            } catch (e: Exception) {
                println(e.message)
            }
        }

    }


    @Override
    override fun onRemoteProcedureCall(rpcMessage: RPCMessage) {
        try {
            var rpcStreamRequest =
                rpcMessage.asJSON(jacksonObjectMapper(), RPCRequestBody::class.java)
            when (rpcStreamRequest.name.first()) {
                "createHistoryStream" -> onCreateHistoryStream(rpcMessage)
                else -> Log.w(TAG, "unhandled function : " + rpcMessage.asString())
            }
        } catch (e: JsonProcessingException) {
            Log.e(TAG, "unhandled function : " + rpcMessage.asString(), e)
        }
    }

    private fun onCreateHistoryStream(rpcMessage: RPCMessage) {
        var rpcStreamRequest = rpcMessage.asJSON(objectMapper, RPCStreamRequest2::class.java)
        var id = rpcStreamRequest.id
        var sequence = rpcStreamRequest.seq
        var remoteLimit = rpcStreamRequest.limit


        if (sequence < 1) {
            Log.w(TAG, String.format("pub is requesting the whole history: %s", sequence))
        }
        var remoteSequence = sequence.toLong()
        val batchSize = Math.min(3, remoteLimit)
        var hasMoreResults = true
        var ct = 0
        while (hasMoreResults) {
            val messages = messageRepository.getMessagePage(id, remoteSequence, batchSize)
            if (messages.size < batchSize) {
                hasMoreResults = false
            }
            messages.forEach {
                Log.d(TAG, "sending:" + it.key)
                val response = RPCCodec.encodeResponse(
                    Bytes.wrap(it.toJsonResponse(format)),
                    rpcMessage.requestNumber(),
                    RPCFlag.Stream.STREAM,
                    RPCFlag.BodyType.JSON
                )
                rpcHandler?.sendBytes(response)
                // increment for next query
                remoteSequence = it.sequence
                // increment for remote limit & protection
                ct++
            }
            if (ct > remoteLimit) {
                Log.w(TAG, "max remote limit reached: " + ct)
                break // !!
            }
        }
        Log.d(TAG, "sending endstream for: " + rpcMessage.requestNumber())
        rpcHandler?.endStream(rpcMessage.requestNumber() * -1)

        val pk = this.toCanonicalForm()
        // server has more message than us, that mean that our device is late and can be upgraded
        val ourSequence = messageRepository.getLastSequence(pk)
        if (remoteSequence > ourSequence) {
            GlobalScope.launch {
                createHistoryStream(pk,ourSequence + 1)
            }
        }
    }

    fun createHistoryStream(pk: String, sequence: Long) {
        val params = HashMap<String, Any>()
        params["id"] = pk
        params["seq"] = sequence
        params["limit"] = 100
        params["keys"] = true
        createStream("createHistoryStream", params)
    }


    fun createStream(functionName: String, params: Map<String, Any>) {
        val streamEnded = AsyncResult.incomplete<Void>()
        val streamRequest = RPCStreamRequest(RPCFunction(functionName), listOf(params))
        rpcHandler!!.openStream(
            streamRequest
        ) {
            object : ScuttlebuttStreamHandler {
                override fun onMessage(message: RPCResponse) {
                    routeMessage(message)
                }

                override fun onStreamEnd() {
                    streamEnded.complete(null)
                }

                override fun onStreamError(ex: Exception) {
                    streamEnded.completeExceptionally(ex)
                }
            }
        }
        // Wait until the stream is complete
        try {
            streamEnded.get()
        } catch (ex: RuntimeException) {
            Log.e(TAG, "%s %s : %s".format(functionName, params, ex.message.toString()))
            streamEnded.completeExceptionally(ex)
        }
    }

    fun routeMessage(message: RPCResponse) {
        Log.i("routeMessage", message.asString())
        var m = message.asJSON(objectMapper, FeedMessage::class.java)
        if (m.type.isPresent) {
            when (m.type.get()) {
                "post" -> storePostMessage(m)
                "vote" -> storeVoteMessage(m)
                "contact" -> storeContactMessage(m)
                "about" -> storeAboutMessage(m)
                else -> println("not implemented:" + m.type.get())
            }
        } else {
            Log.w(TAG, "type not present: " + m);
        }
    }

    private fun storeVoteMessage(m: FeedMessage) {

    }

    private fun storeContactMessage(m: FeedMessage) {

    }

    private fun storeAboutMessage(m: FeedMessage) {
        var about: About? = m.toAbout()
        if (about != null) {
            aboutRepository.insertOrUpdate(about!!)
        } else {
            Log.w(TAG, "unable to decode %s %s".format(m.key, m.value.contentAsString))
        }
    }

    private fun storePostMessage(m: FeedMessage) {
        var message: Message = m.toMessage()
        GlobalScope.launch {
            messageRepository.maybeAddMessage(message)
        }
    }

}

