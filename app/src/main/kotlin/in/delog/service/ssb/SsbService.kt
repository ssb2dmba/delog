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

import android.util.Log
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import `in`.delog.MainApplication
import `in`.delog.db.model.About
import `in`.delog.db.model.Ident
import `in`.delog.db.model.Message
import `in`.delog.db.model.isOnion
import `in`.delog.db.model.toJsonResponse
import `in`.delog.db.repository.AboutRepository
import `in`.delog.db.repository.ContactRepository
import `in`.delog.db.repository.MessageRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.scuttlebutt.lib.model.FeedMessage
import org.apache.tuweni.scuttlebutt.lib.model.toAbout
import org.apache.tuweni.scuttlebutt.lib.model.toMessage
import org.apache.tuweni.scuttlebutt.rpc.RPCCodec
import org.apache.tuweni.scuttlebutt.rpc.RPCFlag
import org.apache.tuweni.scuttlebutt.rpc.RPCFunction
import org.apache.tuweni.scuttlebutt.rpc.RPCMessage
import org.apache.tuweni.scuttlebutt.rpc.RPCRequestBody
import org.apache.tuweni.scuttlebutt.rpc.RPCResponse
import org.apache.tuweni.scuttlebutt.rpc.RPCStreamRequest
import org.apache.tuweni.scuttlebutt.rpc.RPCStreamRequest2
import org.apache.tuweni.scuttlebutt.rpc.mux.ScuttlebuttStreamHandler


class SsbService(
    messageRepository: MessageRepository,
    contactRepository: ContactRepository,
    aboutRepository: AboutRepository,
    torService: TorService
) :
    BaseSsbService() {
    val messageRepository = messageRepository
    val contactRepository = contactRepository
    val aboutRepository = aboutRepository

    var connected: Int = 0 // 0 disconnected, 1 connected, -1 errored

    val torService = torService


    suspend fun synchronize(pFeed: Ident, errorCb: ((Exception) -> Unit)?) {
        val applicationScope = MainApplication.getApplicationScope()
        applicationScope.launch {
            try {
                reconnect(pFeed)
            } catch (e: Exception) {
                e.printStackTrace()
                if (errorCb != null) {
                    errorCb(e)
                }
            }
        }.join()
    }

    suspend fun reconnect(pFeed: Ident) {
        Log.d(TAG, "reconnecting to %s %s".format(pFeed.server, pFeed.publicKey))
        if (pFeed.isOnion()) {
            torService.start();
        }
        super.connect(pFeed, ::onConnected, ::onError)
    }

    private fun onError(error: Exception) {

    }

    private fun onConnected() {
        connected = 1
        val myFeed = this.toCanonicalForm()
        GlobalScope.launch {
            try {
                // let's check our backup, moved device
                var ourSequence = messageRepository.getLastSequence(myFeed)
                createHistoryStream(myFeed, ourSequence)
                // let's call all of our friends
                contactRepository.geContacts(myFeed).forEach {
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
        val batchSize = Math.min(100, remoteLimit) // TODO put in config
        var hasMoreResults = true
        var ct = 0
        while (hasMoreResults) {
            val messages = messageRepository.getMessagePage(id, remoteSequence, batchSize)
            if (messages.size < batchSize) {

            }
            if (messages.size == 0) {
                Log.w(TAG, "db return empty: " + ct)
                hasMoreResults = false
            }
            for (m: Message in messages) {
                Log.d(TAG, "> [${rpcMessage.requestNumber()}] :" + m)
                val response = RPCCodec.encodeResponse(
                    Bytes.wrap(m.toJsonResponse(format)),
                    rpcMessage.requestNumber(),
                    RPCFlag.Stream.STREAM,
                    RPCFlag.BodyType.JSON
                )
                rpcHandler?.sendBytes(response)
                // increment for next query
                remoteSequence = m.sequence
                // increment for remote limit & protection
                ct++
                updateLastPush(m)
            }

        }
        Log.d(TAG, "sending endstream for: " + rpcMessage.requestNumber())
        rpcHandler?.endStream(rpcMessage.requestNumber() * -1)

        if (secureScuttlebuttVertxClient != null) {
            Thread.sleep(3000)
            Log.d(TAG, "closing connection")
            secureScuttlebuttVertxClient!!.stop()
        }
        //torService.stop();

    }

    private fun updateLastPush(it: Message) {
        // TODO
        // take it.author feed in database and set lastPush to it.sequence (if lower than)
    }

    fun createHistoryStream(pk: String, sequence: Long) {
        val params = HashMap<String, Any>()
        params["id"] = pk
        params["seq"] = sequence
        params["limit"] = 100
        params["keys"] = true
        Log.d("createHistoryStream", "$pk $sequence")
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
        Log.d("routeMessage", message.asString())
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

