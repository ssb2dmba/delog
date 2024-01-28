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
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import `in`.delog.db.model.Ident
import `in`.delog.db.model.asKeyPair
import io.vertx.core.Vertx
import kotlinx.serialization.json.Json
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.crypto.sodium.Signature.PublicKey
import org.apache.tuweni.io.Base64
import org.apache.tuweni.scuttlebutt.Identity
import org.apache.tuweni.scuttlebutt.Invite
import org.apache.tuweni.scuttlebutt.handshake.vertx.SecureScuttlebuttVertxClient
import org.apache.tuweni.scuttlebutt.lib.FeedService
import org.apache.tuweni.scuttlebutt.lib.ScuttlebuttClient
import org.apache.tuweni.scuttlebutt.lib.ScuttlebuttClientFactory
import org.apache.tuweni.scuttlebutt.rpc.*
import org.apache.tuweni.scuttlebutt.rpc.mux.RPCHandler
import java.util.*

open class BaseSsbService {

    lateinit var callBack: () -> Unit

    var rpcHandler: RPCHandler? = null

    lateinit var feedService: FeedService
    var secureScuttlebuttVertxClient: SecureScuttlebuttVertxClient? = null
    val vertx: Vertx = Vertx.vertx()
    private val networkKeyBase64 = "1KHLiKZvAvjbY1ziZEHMXawbCEIM6qwjCDm3VYRan/s="
    val networkKeyBytes32 = Bytes32.wrap(Base64.decode(networkKeyBase64))
    var keyPair: Signature.KeyPair? = null
    private var host: String = ""
    private var port: Int = 8008
    var feedOid: Long = -1

    companion object {
        val objectMapper = jacksonObjectMapper()
        val TAG = this.javaClass.canonicalName
        val format = Json {
            prettyPrint = true
            prettyPrintIndent = "  " // two spaces
            ignoreUnknownKeys = true
        }
    }

    fun toCanonicalForm(): String {
        val identity = keyPair?.let { Identity.fromKeyPair(it) }
        return identity?.toCanonicalForm() ?: "";
    }

    open suspend fun connectWithInvite(
        feed: Ident,
        callBack: (RPCResponse) -> Unit,
        errorCb: ((Exception) -> Unit)?
    ) {
        try {
            setIdentity(feed)
            if (feed.invite == null) {
                Log.e("ssb", "attempting to connect with invite but no invite !")
                return
            }
            val inviteString = feed.invite!!
            val invite: Invite = Invite.fromCanonicalForm(inviteString);


            var ssbClient: ScuttlebuttClient = ScuttlebuttClientFactory.withInvite(
                vertx,
                keyPair!!, invite, networkKeyBytes32
            )

            val params = HashMap<String, String>()
            params["feed"] = feed.publicKey
            val asyncRequest = RPCAsyncRequest(RPCFunction(listOf("invite"), "use"), listOf(params))
            val rpcMessageAsyncResult = ssbClient.rawRequestService.makeAsyncRequest(asyncRequest)
            Log.i(TAG, rpcMessageAsyncResult.asString())
            callBack(rpcMessageAsyncResult)
        } catch (ex: Exception) {
            if (errorCb != null) {
                errorCb(ex)
            }
        }
    }


    open suspend fun connect(
        pFeed: Ident,
        terminationFn: () -> Unit,
        errorFn: (Exception) -> Unit
    ): RPCHandler? {
        setIdentity(pFeed)
        if (keyPair == null || host.isEmpty()) {
            Log.w(TAG, "attempting to connect but no identity")
            return null
        }
        callBack = terminationFn
        secureScuttlebuttVertxClient =
            SecureScuttlebuttVertxClient(vertx, keyPair!!, networkKeyBytes32)
        for (i in 0..3) {
            try {
                if (pFeed.invite == null) {
                    Log.e("ssb", "attempting to connect but no invite !")
                    return null
                }
                val invite: Invite = Invite.fromCanonicalForm(pFeed.invite!!);
                val remotePublicKey = invite.identity.ed25519PublicKey();
                rpcHandler = makeRPCHandler(remotePublicKey!!)
                feedService = FeedService(rpcHandler!!)
                return rpcHandler
            } catch (e: Exception) {
                println(e)
                Thread.sleep(1000 * i.toLong())
                if (i >= 3) throw e
            }
        }

        return null
    }

    @Throws(Exception::class)
    private suspend fun makeRPCHandler(remotePublicKey: PublicKey): RPCHandler? {
        rpcHandler = secureScuttlebuttVertxClient
            ?.connectTo(
                port,
                host,
                remotePublicKey,
                null
            ) { sender, terminationFn ->
                RPCHandler(
                    vertx,
                    sender,
                    terminationFn,
                    ::onRemoteProcedureCall,
                )
            } as RPCHandler
        vertx.eventBus().publish("app_state", "connected")
        callBack()
        return rpcHandler as RPCHandler
    }

    internal open fun onRemoteProcedureCall(rpcMessage: RPCMessage) {
        // overwritten ...
        assert(false)
    }

    private fun setIdentity(pFeed: Ident) {
        if (pFeed == null || pFeed.privateKey == null) {
            return
        }
        feedOid = pFeed.oid
        host = pFeed.server
        port = pFeed.port
        keyPair = pFeed.asKeyPair()
    }


}
