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
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import `in`.delog.db.model.Ident
import `in`.delog.db.model.asKeyPair
import `in`.delog.repository.ContactRepositoryImpl
import `in`.delog.repository.MessageRepositoryImpl
import io.vertx.core.Vertx
import kotlinx.serialization.json.Json
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.crypto.sodium.Signature.PublicKey
import org.apache.tuweni.io.Base64
import org.apache.tuweni.scuttlebutt.Identity
import org.apache.tuweni.scuttlebutt.handshake.vertx.SecureScuttlebuttVertxClient
import org.apache.tuweni.scuttlebutt.lib.FeedService
import org.apache.tuweni.scuttlebutt.rpc.*
import org.apache.tuweni.scuttlebutt.rpc.mux.RPCHandler
import java.net.ConnectException
import java.util.*

open class BaseSsbService(
    messageRepositoryImpl: MessageRepositoryImpl,
    contactRepositoryImpl: ContactRepositoryImpl
) {

    lateinit var callBack: () -> Unit
    var messageRepositoryImpl = messageRepositoryImpl
    var contactRepositoryImpl = contactRepositoryImpl
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


    open suspend fun connect(pFeed: Ident, terminationFn: () -> Unit): RPCHandler? {
        setIdentity(pFeed) // TODO to rework
        if (keyPair == null || host.isEmpty()) {
            Log.w(TAG, "attempting to connect but no identity")
            return null
        }
        callBack = terminationFn
        secureScuttlebuttVertxClient =
            SecureScuttlebuttVertxClient(vertx, keyPair!!, networkKeyBytes32)
        for (i in 0..1) {
            try {
                val remotePublicKey =
                    PublicKey.fromBytes(Base64.decode("YpSbE5/7oWuf7k6zhU/wwbm28EffUggYEwVpDkOAdIg="))
                rpcHandler = makeRPCHandler(remotePublicKey)
                feedService = FeedService(rpcHandler!!)
                return rpcHandler
            } catch (e: ConnectException) {
                println(e)
                Thread.sleep(1000 * i.toLong())
            }
        }
        return null
    }

    @Throws(Exception::class)
    suspend fun makeRPCHandler(remotePublicKey: PublicKey): RPCHandler? {
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
        assert(false) // todo extract interface
    }

    fun setIdentity(pFeed: Ident) {
        if (pFeed == null || pFeed.privateKey == null) {
            return
        }
        feedOid = pFeed.oid
        host = pFeed.server
        port = pFeed.port
        keyPair = pFeed.asKeyPair()
    }

    fun toCanonicalForm(): String {
        val identity = keyPair?.let { Identity.fromKeyPair(it) }
        return identity?.toCanonicalForm() ?: "";
    }

}

@ExperimentalUnsignedTypes // just to make it clear that the experimental unsigned types are used
fun ByteArray.toHexString() = asUByteArray().joinToString("") { it.toString(16).padStart(2, '0') }