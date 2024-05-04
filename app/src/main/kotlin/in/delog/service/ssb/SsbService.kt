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
import androidx.compose.runtime.Immutable
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import `in`.delog.db.model.Ident
import `in`.delog.db.model.IdentAndAboutWithBlob
import `in`.delog.db.model.Message
import `in`.delog.db.model.asKeyPair
import `in`.delog.db.model.isOnion
import `in`.delog.db.model.toCanonicalForm
import `in`.delog.db.repository.AboutRepository
import `in`.delog.db.repository.BlobRepository
import `in`.delog.db.repository.ContactRepository
import `in`.delog.db.repository.IdentRepository
import `in`.delog.db.repository.MessageRepository
import `in`.delog.model.Mention
import `in`.delog.model.SsbMessageContent
import `in`.delog.model.SsbSignableMessage
import `in`.delog.model.SsbSignedMessage
import `in`.delog.viewmodel.fromSsbSignedMessage
import io.netty.channel.ConnectTimeoutException
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.dns.AddressResolverOptions
import io.vertx.core.metrics.MetricsOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
import java.util.concurrent.CompletableFuture


@Immutable
data class SsbUIState(
    val identAndAbout: IdentAndAboutWithBlob? = null,
    val loaded: Boolean = false,
    val connecting: Boolean = false,
    val connected: Boolean = false,
    val error: Exception? = null,
    val blobSize: HashMap<String,Long> = HashMap(),
    val blobDown: HashMap<String,Long> = HashMap(),
    val blobUp: HashMap<String,Long> = HashMap()
)

class SsbService(
    private val identRepository: IdentRepository,
    private val messageRepository: MessageRepository,
    private val aboutRepository: AboutRepository,
    private val contactRepository: ContactRepository,
    private val blobRepository: BlobRepository,
    private val torService: TorService
) {

    private var blobService: BlobService? =null
    private var feedService: FeedService? = null
    private var connectedIdent: Ident? = null

    private var rpcHandler: RPCHandler? = null
    private var secureScuttlebuttVertxClient: SecureScuttlebuttVertxClient? = null
    private val _uiState = MutableStateFlow(SsbUIState())
    val uiState: StateFlow<SsbUIState> = _uiState.asStateFlow()
    private lateinit var promise: CompletableFuture<Boolean>
    lateinit var callBack: () -> Unit
    var vertx: Vertx


    init {
        val vertxOptions = VertxOptions()
        vertxOptions.preferNativeTransport = true
        vertxOptions.setMetricsOptions(MetricsOptions().setEnabled(false))
        vertxOptions.setAddressResolverOptions(AddressResolverOptions().setServers(listOf("8.8.8.8")))
        vertx  = Vertx.vertx(vertxOptions)
    }

    companion object {
        const val MAX_RETRY = 10
        val objectMapper = jacksonObjectMapper()
        const val TAG: String = "dlog-ssb-service"


        @OptIn(ExperimentalSerializationApi::class)
        val format = Json {
            prettyPrint = true
            prettyPrintIndent = "  " // two spaces
            ignoreUnknownKeys = true
        }
    }


    suspend fun replicateSync():CompletableFuture<Boolean> {

        promise= CompletableFuture<Boolean>()
        identRepository.default.take(1).collect {
            if (it != null) {
                replicate(it.ident)
            }
        }
        return promise
    }


    private suspend fun replicate(pFeed: Ident) {
        try {
            val keyPair = pFeed.asKeyPair()
            if (keyPair == null || pFeed.server.isEmpty()) {
                Log.d(TAG, "attempting to connect but no identity")
                throw Exception("no identity")
            }
            if (pFeed.invite == null) {
                Log.d("ssb", "attempting to connect but no invite !")
                throw Exception("no invite")
            }
            if (_uiState.value.connecting || _uiState.value.connected) {
                Log.d("ssb", "already connected")
                throw Exception("already connected")
            }
            reconnect(pFeed)
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e, connecting = false, connected=false) }
            e.message?.let { Log.e(TAG, it) }
        }
    }

    private suspend fun reconnect(pFeed: Ident) {
        if (_uiState.value.connecting) {
            Log.d(TAG,"skipping: already connecting ...")
            return
        }
        Log.i(TAG, "reconnecting to %s %s".format(pFeed.server, pFeed.publicKey))
        _uiState.update { it.copy(connecting = true, error=null) }
        maybeWaitForTor(pFeed)
        connect(pFeed, ::onConnected)
        _uiState.update { it.copy(connecting = false) }
    }

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    private fun monitorStreamForCallBack() {
        scope.launch(Dispatchers.IO) {
            var ct = 0
            var gotStream = false
            while (true) {
                if (rpcHandler!!.streams.isNotEmpty()){
                    //Log.d(TAG, "got streams ${rpcHandler!!.streams}")
                    gotStream = true
                }
                runBlocking { delay(1000) }
                if (rpcHandler!= null && gotStream && rpcHandler!!.streams.isEmpty()) {
                    ct += 1
                    if (ct > 10) {
                        Log.d(TAG, "no streams, replication complete")
                        disconnect()
                        //torService.torOperationManager.stop()
                        promise.complete(true)
                        break
                    }
                } else {
                    ct=0
                }
            }
        }
    }

    private fun maybeWaitForTor(pFeed: Ident) {
        if (pFeed.isOnion()) {
            torService.start()
            for (i in 0..20) {
                if (torService.status.value != 1) {
                    runBlocking { delay(500) }
                    if (i>=20) break
                } else {
                    break
                }
            }
        }
    }

    private suspend fun connect(
        pFeed: Ident,
        terminationFn: () -> Unit
    ): RPCHandler? {
        val keyPair = pFeed.asKeyPair()

        callBack = terminationFn

        for (i in 0..MAX_RETRY) {

            secureScuttlebuttVertxClient =
                SecureScuttlebuttVertxClient(vertx, keyPair!!, ScuttlebuttClientFactory.DEFAULT_NETWORK)
            try {
                rpcHandler = makeRPCHandler(pFeed)
                monitorStreamForCallBack()
                return rpcHandler
            } catch (e: ConnectTimeoutException) {
                e.message?.let { Log.e(TAG, it) }
                if (i >= 2) throw e
            } catch (e: Exception) {
                e.message?.let { Log.d(TAG, it) }
                //torService.torOperationManager.restart()
                runBlocking { delay(500L * i) }
                if (i >= MAX_RETRY) throw e
            }
        }
        return null
    }

    private fun disconnect() {
        _uiState.update { it.copy(connected = false, connecting = false) }
        if (secureScuttlebuttVertxClient!=null) {
            secureScuttlebuttVertxClient!!.stop().join()
            connectedIdent = null
            secureScuttlebuttVertxClient = null
        }
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     *  application logic upon connection
     */
    private fun onConnected() {
        _uiState.update { it.copy(connected = true) }
        blobService = BlobService(rpcHandler!!, blobRepository, _uiState)
        feedService = FeedService(rpcHandler!!, blobRepository, aboutRepository, messageRepository,
            blobService!!
        )

        val feedCannonicalForm = connectedIdent!!.toCanonicalForm()
        try {
            // let's check @self for a backup or moved device
            var ourSequence = messageRepository.getLastSequence(feedCannonicalForm)
            // createHistoryStream
            feedService!!.createHistoryStream(
                feedCannonicalForm,
                ourSequence
            )

            // let's call all of our friends
            contactRepository.geContacts(feedCannonicalForm).forEach {
        Log.i(TAG, "calling friend ${it.follow}")
                ourSequence = messageRepository.getLastSequence(it.follow)
                feedService?.createHistoryStream(
                    it.follow,
                    ourSequence
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(connecting = false, error=e) }
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
        _uiState.update { it.copy(error = null, connecting = true) }
        try {
            if (feed.invite == null) {
                throw Exception("attempting to connect with invite but no invite !")
            }
            val inviteString = feed.invite!!
            val invite: Invite = Invite.fromCanonicalForm(inviteString)

            for (i in 0..MAX_RETRY) {
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
                val asyncRequest =
                    RPCAsyncRequest(RPCFunction(listOf("invite"), "use"), listOf(params))
                val rpcMessageAsyncResult =
                    ssbInviteClient.rawRequestService.makeAsyncRequest(asyncRequest)
                _uiState.update { it.copy(error = null, connecting = false) }
                callBack(rpcMessageAsyncResult)
                break
            }
        } catch (ex: Exception) {
            _uiState.update { it.copy(error = ex, connecting = false) }
            if (errorCb != null) {
                errorCb(ex)
            } else {
                throw ex
            }
        }
    }



    suspend fun deletePost(ident: Ident, messageEntity:Message?) {
        if (messageEntity== null) {
            Log.w(TAG, "deletePost: messageEntity is null")
            return
        }
        val last: Message? = messageRepository.getLastMessage(messageEntity.author)
        var sequence = 1L
        if (last != null) {
            sequence = last.sequence + 1
        }
        val mentions = arrayOf(
            Mention(
                link = messageEntity.key
            )
        )
        val ssbMessageContent = SsbMessageContent(
            type = "post-delete",
            mentions = mentions,
        )
        val ssbSignableMessage = SsbSignableMessage(
            previous = last?.key,
            sequence=sequence,
            author = messageEntity.author,
            timestamp = System.currentTimeMillis(),
            content = ssbMessageContent,
            hash = "sha256"
        )
        // precise some blockchain info
        val sig = ssbSignableMessage.signMessage(ident)
        // add sig & hash info
        val ssbSignedMessage = SsbSignedMessage(ssbSignableMessage, sig)
        val hash = ssbSignedMessage.makeHash()
        ssbSignedMessage.hash = "%" + hash!!.bytes().toBase64String() + ".sha256"
        // translate to db model
        val message = fromSsbSignedMessage(ssbSignedMessage)
        feedService?.executeDeleteMessage(message)
        messageRepository.addMessage(message)
    }


}
