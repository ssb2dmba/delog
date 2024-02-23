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
import androidx.lifecycle.LifecycleService
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
) : LifecycleService() {

    val vertx: Vertx = Vertx.vertx()
    private var feed: Ident? = null
    private var ssbClient: ScuttlebuttClient? = null

    private val ssbRequiredRepositories = SsbRequiredRepositories(
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

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)
    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        if (ssbClient!=null) {
            ssbClient!!.rpcHandler.close()
        }
    }

    suspend fun reconnect(ident: Ident) {
        feed = ident
        if (ident.isOnion()) {
            torService.start()
        }
        try {
            ssbClient = ScuttlebuttClientFactory.fromFeedWithVertx(
                vertx,
                ident,
                ssbRequiredRepositories,
                ::onRPC
            )
        } catch (e: Exception) {
            MainApplication.toastify(e.message ?: "error connecting to server")
            return
        }
        onConnected()
    }


    /**
     *  application logic upon connection
     */
    private fun onConnected() {
        if (feed == null || ssbClient == null) {
            Log.d("ssb", "onConnected: feed $feed or client $ssbClient is null")
            return
        }
        val feedCannonicalForm = feed!!.toCanonicalForm()
        scope.launch {
            try {
                // let's check @self for a backup or moved device
                var ourSequence = messageRepository.getLastSequence(feedCannonicalForm)
                Log.d("ssb", "createHistoryStream")
                ssbClient!!.feedService.createHistoryStream(
                    feedCannonicalForm,
                    ourSequence
                )
                Log.d("ssb", "createWantStream")
                ssbClient!!.blobService.createWantStream(ssbClient!!.blobService::wantStreamHandler)
                // let's call all of our friends
                contactRepository.geContacts(feedCannonicalForm).forEach {
                    ourSequence = messageRepository.getLastSequence(it.follow)
                    ssbClient?.feedService?.createHistoryStream(
                        it.follow,
                        ourSequence
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    /**
     * Handle incoming RPC messages and route them accordingly
     */
    private fun onRPC(rpcMessage: RPCMessage) {
        if (ssbClient==null) return
        try {
            val rpcStreamRequest =
                rpcMessage.asJSON(jacksonObjectMapper(), RPCRequestBody::class.java)
            when (rpcStreamRequest.name.first()) {
                "createHistoryStream" -> ssbClient!!.feedService.onCreateHistoryStream(rpcMessage)
                "blobs" -> ssbClient?.blobService?.onBlobsRPC(ssbClient!!.clientId, rpcStreamRequest, rpcMessage)
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
            torService.start()
        }
        try {
            if (feed.invite == null) {
                throw Exception("attempting to connect with invite but no invite !")
            }
            val inviteString = feed.invite!!
            val invite: Invite = Invite.fromCanonicalForm(inviteString)

            val ssbClient: ScuttlebuttClient = ScuttlebuttClientFactory.withInvite(
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
            val rpcMessageAsyncResult = ssbClient.rawRequestService.makeAsyncRequest(asyncRequest)
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

