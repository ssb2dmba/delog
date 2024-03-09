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
import `in`.delog.viewmodel.FeedMainUIState
import io.vertx.core.Vertx
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.apache.tuweni.scuttlebutt.Invite
import org.apache.tuweni.scuttlebutt.handshake.vertx.SecureScuttlebuttVertxClient
import org.apache.tuweni.scuttlebutt.lib.BlobService
import org.apache.tuweni.scuttlebutt.lib.FeedService
import org.apache.tuweni.scuttlebutt.lib.ScuttlebuttClient
import org.apache.tuweni.scuttlebutt.lib.ScuttlebuttClientFactory
import org.apache.tuweni.scuttlebutt.rpc.RPCAsyncRequest
import org.apache.tuweni.scuttlebutt.rpc.RPCFunction
import org.apache.tuweni.scuttlebutt.rpc.RPCMessage
import org.apache.tuweni.scuttlebutt.rpc.RPCRequestBody
import org.apache.tuweni.scuttlebutt.rpc.RPCResponse
import org.apache.tuweni.scuttlebutt.rpc.mux.RPCHandler


class SsbService(
    private val messageRepository: MessageRepository,
    private val aboutRepository: AboutRepository,
    private val contactRepository: ContactRepository,
    private val blobRepository: BlobRepository,
    val torService: TorService,
    private var _uiState: MutableStateFlow<FeedMainUIState>?
) {


    private var blobService: BlobService? =null
    private var feedService: FeedService? = null
    private var connectedIdent: Ident? = null
    lateinit var callBack: () -> Unit
    lateinit var vertx: Vertx
    private var rpcHandler: RPCHandler? = null
    var secureScuttlebuttVertxClient: SecureScuttlebuttVertxClient? = null

    init {
        Log.i(TAG, "init ssb service")

    }

    companion object {
        const val MAX_RETRY = 5
        val objectMapper = jacksonObjectMapper()
        const val TAG: String = "dlog-ssb-service"

        @OptIn(ExperimentalSerializationApi::class)
        val format = Json {
            prettyPrint = true
            prettyPrintIndent = "  " // two spaces
            ignoreUnknownKeys = true
        }
    }

    suspend fun synchronize(pFeed: Ident, errorCb: ((Exception) -> Unit)?) {
        try {
            reconnect(pFeed)
        } catch (e: Exception) {
            MainApplication.toastify("${e.message}")
            if (errorCb != null) {
                errorCb(e)
            }
        }
    }

    private suspend fun reconnect(pFeed: Ident) {
        Log.i(TAG, "reconnecting to %s %s".format(pFeed.server, pFeed.publicKey))

        if (pFeed.isOnion()) {
            torService.start() // will do nothing if started
            for (i in 0..20) {
                if (torService.status.value != 1) {
                    Thread.sleep(500)
                    Log.d(TAG, "waiting for Tor service to be started ...")
                    if (i>=20) return
                } else {
                    break
                }
            }
        }
        if (connectedIdent != null && secureScuttlebuttVertxClient!=null) {
            Log.d(TAG, "disconnecting from %s@%s".format(connectedIdent!!.server, connectedIdent!!.publicKey))
            secureScuttlebuttVertxClient!!.stop().join()
            connectedIdent = null
            secureScuttlebuttVertxClient = null
        }
        connect(pFeed, ::onConnected)
    }


    private suspend fun connect(
        pFeed: Ident,
        terminationFn: () -> Unit
    ): RPCHandler? {
        val keyPair = pFeed.asKeyPair()
        if (keyPair == null || pFeed.server.isEmpty()) {
            Log.w(TAG, "attempting to connect but no identity")
            return null
        }
        callBack = terminationFn
        vertx  = Vertx.vertx()
        secureScuttlebuttVertxClient =
            SecureScuttlebuttVertxClient(vertx, keyPair, ScuttlebuttClientFactory.DEFAULT_NETWORK)
        for (i in 0..MAX_RETRY) {
            try {
                if (pFeed.invite == null) {
                    Log.e("ssb", "attempting to connect but no invite !")
                    return null
                }
                rpcHandler = makeRPCHandler(pFeed)
                if (_uiState==null) _uiState= MutableStateFlow(FeedMainUIState())
                blobService= BlobService(rpcHandler!!, blobRepository, _uiState!!)
                return rpcHandler
            } catch (e: Exception) {
                println(e)
                Thread.sleep(1000 * i.toLong())
                if (i >= MAX_RETRY) throw e
            }
        }

        return null
    }

    @Throws(Exception::class)
    private suspend fun makeRPCHandler(ident: Ident): RPCHandler {
        val invite: Invite = Invite.fromCanonicalForm(ident.invite!!)
        val remotePublicKey = invite.identity.ed25519PublicKey()
        rpcHandler = secureScuttlebuttVertxClient
            ?.connectTo(
                ident.port,
                ident.server,
                remotePublicKey,
                null
            ) { sender, terminationFn ->
                RPCHandler(
                    vertx,
                    sender,
                    terminationFn,
                    ::onRPC,
                )
            } as RPCHandler

        connectedIdent = ident
        callBack()
        return rpcHandler as RPCHandler
    }


    /**
     *  application logic upon connection
     */
    private fun onConnected() {
        feedService = FeedService(rpcHandler!!, blobRepository, aboutRepository, messageRepository)
        blobService = BlobService(rpcHandler!!, blobRepository, _uiState!!)
        val feedCannonicalForm = connectedIdent!!.toCanonicalForm()
        try {
            // let's check @self for a backup or moved device
            var ourSequence = messageRepository.getLastSequence(feedCannonicalForm)
            // createHistoryStream
            feedService!!.createHistoryStream(
                feedCannonicalForm,
                ourSequence
            )
            // createWantStream
            // let's call all of our friends
            contactRepository.geContacts(feedCannonicalForm).forEach {
                ourSequence = messageRepository.getLastSequence(it.follow)
                feedService?.createHistoryStream(
                    it.follow,
                    ourSequence
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Handle incoming RPC messages and route them accordingly
     */
    private fun onRPC(rpcMessage: RPCMessage) {
        try {
            val rpcStreamRequest =
                rpcMessage.asJSON(jacksonObjectMapper(), RPCRequestBody::class.java)
            when (rpcStreamRequest.name.first()) {
                "createHistoryStream" -> feedService?.onCreateHistoryStream(rpcMessage)
                "blobs" -> blobService?.onBlobsRPC(
                    connectedIdent!!.publicKey,
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
                ScuttlebuttClientFactory.DEFAULT_NETWORK
            )

            val params = HashMap<String, String>()
            params["feed"] = feed.publicKey
            val asyncRequest = RPCAsyncRequest(RPCFunction(listOf("invite"), "use"), listOf(params))
            val rpcMessageAsyncResult =
                ssbInviteClient.rawRequestService.makeAsyncRequest(asyncRequest)
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

