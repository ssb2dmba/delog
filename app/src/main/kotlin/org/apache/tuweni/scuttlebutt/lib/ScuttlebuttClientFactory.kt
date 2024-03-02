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

import `in`.delog.db.model.Ident
import `in`.delog.db.model.asKeyPair
import `in`.delog.db.model.toCanonicalForm
import `in`.delog.db.repository.AboutRepository
import `in`.delog.db.repository.BlobRepository
import `in`.delog.db.repository.MessageRepository
import io.vertx.core.Vertx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.io.Base64
import org.apache.tuweni.scuttlebutt.Invite
import org.apache.tuweni.scuttlebutt.handshake.vertx.SecureScuttlebuttVertxClient
import org.apache.tuweni.scuttlebutt.rpc.RPCMessage
import org.apache.tuweni.scuttlebutt.rpc.mux.RPCHandler


class SsbRequiredRepositories(
    val feedRepository: MessageRepository,
    val aboutRepository: AboutRepository,
    val blobRepository: BlobRepository
) {
    lateinit var scope: CoroutineScope
}

/**
 * A factory for constructing a new instance of ScuttlebuttClient with the given configuration parameters
 */
object ScuttlebuttClientFactory {
    @JvmStatic
    val DEFAULT_NETWORK: Bytes32 =
        Bytes32.wrap(Base64.decode("1KHLiKZvAvjbY1ziZEHMXawbCEIM6qwjCDm3VYRan/s="))

    @JvmStatic
    fun fromFeedWithVertx(
        vertx: Vertx,
        pFeed: Ident,
        ssbRequiredRepositories: SsbRequiredRepositories,
        onRPCRequest:  (RPCMessage) -> Unit
    ) : ScuttlebuttClient {
        val invite: Invite = Invite.fromCanonicalForm(pFeed.invite!!)
        val remotePublicKey = invite.identity.ed25519PublicKey()
        val clientId = pFeed.toCanonicalForm()
        return fromNetWithNetworkKey(
            clientId,
            vertx,
            pFeed.server,
            pFeed.port,
            pFeed.asKeyPair(),
            remotePublicKey,
            DEFAULT_NETWORK,
            ssbRequiredRepositories,
            onRPCRequest
        )


    }

    /**
     * Creates a scuttlebutt client by connecting with the given host, port and keypair using the given vertx instance.
     *
     * @param vertx the vertx instance to use for network IO
     * @param host The host to connect to as a scuttlebutt client
     * @param port The port to connect on
     * @param keyPair The keys to use for the secret handshake
     * @param serverPublicKey the public key of the server we connect to
     * @return the scuttlebutt client
     */
    @JvmStatic
    fun fromNetWithVertx(
        clientId: String,
        vertx: Vertx,
        host: String,
        port: Int,
        keyPair: Signature.KeyPair,
        serverPublicKey: Signature.PublicKey,
        ssbRequiredRepositories: SsbRequiredRepositories,
        onRPCRequest: (rpcM: RPCMessage) -> Unit
    ): ScuttlebuttClient {
        return fromNetWithNetworkKey(clientId, vertx, host, port, keyPair, serverPublicKey, DEFAULT_NETWORK, ssbRequiredRepositories,onRPCRequest)
    }

    /**
     * Creates a SSB client with a network key
     *
     * @param vertx the vertx instance to use for network IO
     * @param host The host to connect to as a scuttlebutt client
     * @param port The port to connect on
     * @param keyPair The keys to use for the secret handshake
     * @param serverPublicKey the public key of the server we connect to
     * @param networkIdentifier The scuttlebutt network key to use.
     * @return the scuttlebutt client
     */
    @JvmStatic
    fun fromNetWithNetworkKey(
        clientId: String,
        vertx: Vertx,
        host: String,
        port: Int,
        keyPair: Signature.KeyPair?,
        serverPublicKey: Signature.PublicKey?,
        networkIdentifier: Bytes32?,
        ssbRequiredRepositories: SsbRequiredRepositories,
        onRPCRequest: (RPCMessage) -> Unit
    ): ScuttlebuttClient {
        val secureScuttlebuttVertxClient = SecureScuttlebuttVertxClient(
            vertx,
            keyPair!!,
            networkIdentifier!!
        )
        return runBlocking {
            val client = secureScuttlebuttVertxClient.connectTo(
                port,
                host,
                serverPublicKey,
                null
            ) { sender, terminationFn ->
                RPCHandler(
                    vertx,
                    sender,
                    terminationFn,
                    onRPCRequest
                )
            } as RPCHandler

            return@runBlocking ScuttlebuttClient(clientId, client, ssbRequiredRepositories)
        }
    }

    /**
     * Creates a SSB client with an invite
     *
     * @param vertx the vertx instance to use for network IO
     * @param keyPair The keys to use for the secret handshake
     * @param invite the invitation to the remote server
     * @param networkIdentifier The scuttlebutt network key to use.
     * @return the scuttlebutt client
     */
    @JvmStatic
    fun withInvite(
        clientId: String,
        vertx: Vertx,
        keyPair: Signature.KeyPair,
        invite: Invite,
        networkIdentifier: Bytes32,
        ssbRequiredRepositories: SsbRequiredRepositories
    ): ScuttlebuttClient {
        val secureScuttlebuttVertxClient = SecureScuttlebuttVertxClient(
            vertx,
            keyPair,
            networkIdentifier
        )
        return runBlocking {
            val multiplexer: RPCHandler = secureScuttlebuttVertxClient
                .connectTo(
                    invite.port,
                    invite.host,
                    null,
                    invite
                ) { sender, terminationFn ->
                    RPCHandler(
                        vertx,
                        sender,
                        terminationFn,
                        { rpcMessage -> }
                    )
                } as RPCHandler
            return@runBlocking ScuttlebuttClient(clientId, multiplexer,ssbRequiredRepositories)
        }
    }
}
