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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.io.Base64
import org.apache.tuweni.scuttlebutt.Invite
import org.apache.tuweni.scuttlebutt.lib.ScuttlebuttClient
import org.apache.tuweni.scuttlebutt.lib.ScuttlebuttClientFactory
import org.apache.tuweni.scuttlebutt.lib.model.FeedMessage
import org.apache.tuweni.scuttlebutt.lib.model.toMessage
import org.apache.tuweni.scuttlebutt.rpc.*
import org.apache.tuweni.scuttlebutt.rpc.mux.ScuttlebuttStreamHandler
import `in`.delog.db.model.About
import `in`.delog.db.model.Ident
import `in`.delog.db.model.Message
import `in`.delog.db.model.toJsonResponse
import `in`.delog.repository.AboutRepository
import `in`.delog.repository.ContactRepositoryImpl
import `in`.delog.repository.MessageRepositoryImpl


class SsbService(
    messageRepositoryImpl: MessageRepositoryImpl,
    contactRepositoryImpl: ContactRepositoryImpl,
    aboutRepository: AboutRepository
) :
    BaseSsbService(messageRepositoryImpl, contactRepositoryImpl) {


    var aboutRepository = aboutRepository


    suspend fun reconnect(pFeed: Ident) {
        Log.i(TAG, "connecting to %s %s".format(pFeed.server, pFeed.publicKey))
        try {
            super.connect(pFeed, ::onConnected)
        } catch (e: Exception) {
            Log.e(
                TAG,
                "connecting to %s %s : %s ".format(
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
                // let's call all of our friends
                contactRepositoryImpl.geContacts(myFeed).forEach {
                    val ourSequence = messageRepositoryImpl.getLastSequence(it.follow)
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
            val messages = messageRepositoryImpl.getMessagePage(id, remoteSequence, batchSize)
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

        // server has more message than us, that mean that our device is late and can be upgraded
        val ourSequence = messageRepositoryImpl.getLastSequence(this.toCanonicalForm())
        Log.i(
            TAG,
            "%s our: %s ,theirs: %s".format(this.toCanonicalForm(), ourSequence, remoteSequence)
        )
        if (remoteSequence > ourSequence) {
            Log.i(TAG, "trying to retrieve messages from other device")
            GlobalScope.launch {
                createHistoryStream(ourSequence + 1)
            }
        }
    }


    fun createHistoryStream(sequence: Long) {
        createHistoryStream(this.toCanonicalForm(), sequence)
    }

    fun createHistoryStream(id: String, sequence: Long) {
        Log.i(TAG, "calling remote createHistoryStream(%s, %s)".format(id, sequence))
        val params = HashMap<String, Any>()
        params["id"] = id
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
            messageRepositoryImpl.maybeAddMessage(message)
        }
    }


    open suspend fun connectWithInvite(s: String, feed: Ident, callBack: (RPCResponse) -> Unit) {

        setIdentity(feed) // TODO to rework
        val invite: Invite = Invite.fromCanonicalForm(s);
        // hack replace invite by our current feed so we can bypass server name for dev/test
        invite.port = feed.port
        invite.host = feed.server

        var ssbClient: ScuttlebuttClient = ScuttlebuttClientFactory.withInvite(
            vertx,
            keyPair!!, invite, networkKeyBytes32
        )

        val params = HashMap<String, String>()
        params["feed"] = feed.publicKey
        val asyncRequest = RPCAsyncRequest(RPCFunction(listOf("invite"), "use"), listOf(params))
        val rpcMessageAsyncResult = ssbClient.rawRequestService.makeAsyncRequest(asyncRequest)
        println(rpcMessageAsyncResult.body().toString())
        callBack(rpcMessageAsyncResult)
    }

}

private fun FeedMessage.toAbout(): About? {
    val ssbMe: SsbMessageContent = Json.decodeFromString<SsbMessageContent>(
        SsbMessageContent.serializer(),
        this.value.contentAsString
    )
    return if (ssbMe.about == null) {
        null
    } else {
        About(
            about = ssbMe.about!!,
            description = ssbMe.description,
            image = ssbMe.image,
            name = ssbMe.name,
            dirty = false
        )
    }

}
