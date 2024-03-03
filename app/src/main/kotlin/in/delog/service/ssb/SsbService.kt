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
import `in`.delog.db.model.Ident
import `in`.delog.db.model.asKeyPair
import `in`.delog.db.model.isOnion
import `in`.delog.db.model.toCanonicalForm
import `in`.delog.db.repository.AboutRepository
import `in`.delog.db.repository.BlobRepository
import `in`.delog.db.repository.ContactRepository
import `in`.delog.db.repository.MessageRepository
import io.vertx.core.Vertx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.apache.tuweni.scuttlebutt.Invite
import org.apache.tuweni.scuttlebutt.lib.ScuttlebuttClient
import org.apache.tuweni.scuttlebutt.lib.ScuttlebuttClientFactory
import org.apache.tuweni.scuttlebutt.lib.SsbRequiredRepositories
import org.apache.tuweni.scuttlebutt.rpc.RPCAsyncRequest
import org.apache.tuweni.scuttlebutt.rpc.RPCFunction
import org.apache.tuweni.scuttlebutt.rpc.RPCMessage
import org.apache.tuweni.scuttlebutt.rpc.RPCRequestBody
import org.apache.tuweni.scuttlebutt.rpc.RPCResponse


class SsbService(
    private val messageRepository: MessageRepository,
    aboutRepository: AboutRepository,
    private val contactRepository: ContactRepository,
    blobRepository: BlobRepository,
    private val torService: TorService
) {

    val vertx: Vertx = Vertx.vertx()
    private var connectedIdent: Ident? = null
    private var ssbClient: ScuttlebuttClient? = null

    private val ssbRequiredRepositories = SsbRequiredRepositories(
        this,
        messageRepository,
        aboutRepository,
        blobRepository
    )

    companion object {
        val objectMapper = jacksonObjectMapper()
        const val TAG: String = "dlog-ssb-service"

        @OptIn(ExperimentalSerializationApi::class)
        val format = Json {
            prettyPrint = true
            prettyPrintIndent = "  " // two spaces
            ignoreUnknownKeys = true
        }
    }
    fun disconnect() {
        Log.i(TAG,"disconnect")
        //if (connectedIdent == null) return
        ssbClient?.secureScuttlebuttVertxClient?.stop()?.join()
        ssbClient = null
        connectedIdent = null
    }

    suspend fun reconnect(ident: Ident) {
        if (ident.isOnion()) {
            torService.start()
            if (!torService.connected.value) return
        }
        disconnect()
        if (ident.publicKey != connectedIdent?.publicKey) {
            for (i in 0..3) {
                try {
                    if (ssbClient!=null) {
                        break
                    }
                    ssbClient = ScuttlebuttClientFactory.fromFeedWithVertx(
                        vertx,
                        ident,
                        ssbRequiredRepositories,
                        ::onRPC
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "error connecting to server ($i) ${e.message}")
                    runBlocking { delay(i.toLong() * 1000) }
                    if (i >= 3) {
                        MainApplication.toastify(e.message ?: "error connecting to server")
                        disconnect()
                        return
                    }
                }
            }
        }
        connectedIdent = ident
        onConnected()
    }


    /**
     *  application logic upon connection
     */
    private fun onConnected() {
        if (connectedIdent == null || ssbClient == null) {
            Log.d("ssb", "onConnected: feed $connectedIdent or client $ssbClient is null")
            return
        }
        val feedCannonicalForm = connectedIdent!!.toCanonicalForm()
        try {
            // let's check @self for a backup or moved device
            var ourSequence = messageRepository.getLastSequence(feedCannonicalForm)
            // createHistoryStream
            ssbClient!!.feedService.createHistoryStream(
                feedCannonicalForm,
                ourSequence
            )
            // createWantStream
            ssbClient!!.blobService.createWantStream(ssbClient!!.clientId)

            // let's call all of our friends
//            contactRepository.geContacts(feedCannonicalForm).forEach {
//                ourSequence = messageRepository.getLastSequence(it.follow)
//                ssbClient?.feedService?.createHistoryStream(
//                    it.follow,
//                    ourSequence
//                )
//            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Handle incoming RPC messages and route them accordingly
     */
    private  fun onRPC(rpcMessage: RPCMessage) {
        if (ssbClient == null) return
        try {
            val rpcStreamRequest =
                rpcMessage.asJSON(jacksonObjectMapper(), RPCRequestBody::class.java)
            when (rpcStreamRequest.name.first()) {
                "createHistoryStream" -> ssbClient!!.feedService.onCreateHistoryStream(rpcMessage)
                "blobs" -> ssbClient?.blobService?.onBlobsRPC(
                    ssbClient!!.clientId,
                    rpcStreamRequest,
                    rpcMessage
                )

                else -> Log.w(TAG, "unhandled function : " + rpcMessage.asString())
            }
        } catch (e: JsonProcessingException) {
            Log.e(TAG, "unhandled function : " + rpcMessage.asString(), e)
        }
    }


    /**
     * Connect to a server using an invite
     */
    suspend fun connectWithInvite(
        feed: Ident,
        callBack: (RPCResponse) -> Unit,
        errorCb: ((Exception) -> Unit)?
    ) {
        Log.d(TAG, "redeem invite %s %s".format(feed.server, feed.publicKey))
        if (feed.isOnion()) {

        }
        try {
            if (feed.invite == null) {
                throw Exception("attempting to connect with invite but no invite !")
            }
            val inviteString = feed.invite!!
            val invite: Invite = Invite.fromCanonicalForm(inviteString)
            val vertx: Vertx = Vertx.vertx()
            val ssbInviteClient: ScuttlebuttClient = ScuttlebuttClientFactory.withInvite(
                feed.toCanonicalForm(),
                vertx,
                feed.asKeyPair()!!,
                invite,
                ScuttlebuttClientFactory.DEFAULT_NETWORK,
                ssbRequiredRepositories
            )

            val params = HashMap<String, String>()
            params["feed"] = feed.publicKey
            val asyncRequest = RPCAsyncRequest(RPCFunction(listOf("invite"), "use"), listOf(params))
            val rpcMessageAsyncResult = ssbInviteClient.rawRequestService.makeAsyncRequest(asyncRequest)
            callBack(rpcMessageAsyncResult)
        } catch (ex: Exception) {
            if (errorCb != null) {
                errorCb(ex)
            } else {
                throw ex
            }
        }
    }

}

