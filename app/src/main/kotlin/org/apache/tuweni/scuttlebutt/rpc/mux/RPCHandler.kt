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
package org.apache.tuweni.scuttlebutt.rpc.mux

import android.util.Log
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.Handler
import io.vertx.core.Vertx
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.CompletableAsyncResult
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.scuttlebutt.handshake.vertx.ClientHandler
import org.apache.tuweni.scuttlebutt.rpc.*
import org.apache.tuweni.scuttlebutt.rpc.RPCCodec.encodeBlobEnd
import org.apache.tuweni.scuttlebutt.rpc.RPCCodec.encodeBlobSlice
import org.apache.tuweni.scuttlebutt.rpc.RPCCodec.encodeRequest
import org.apache.tuweni.scuttlebutt.rpc.RPCCodec.encodeStreamEndRequest
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.function.Function

/**
 * Handles RPC requests and responses from an active connection to a scuttlebutt node.
 *
 * @param vertx The vertx instance to queue requests with. We run each update on the vertx event loop to update the request state synchronously, and to handle the
 * underlying connection closing by failing the in progress requests and not accepting future requests
 * @param messageSender sends the request to the node
 * @param terminationFn closes the connection
 */
open class RPCHandler(
    private val vertx: Vertx,
    private val messageSender: Consumer<Bytes>,
    private val terminationFn: Runnable,
    private val onRPCRequest: (RPCMessage) -> Unit
) :
    Multiplexer, ClientHandler {
    private val awaitingAsyncResponse: MutableMap<Int, CompletableAsyncResult<RPCResponse>> =
        ConcurrentHashMap()
    val streams: MutableMap<Int, ScuttlebuttStreamHandler> = ConcurrentHashMap()
    private var closed = false
    init {
        streams.clear()
        closed  = false
    }



    @Throws(JsonProcessingException::class)
    override suspend fun makeAsyncRequest(request: RPCAsyncRequest): RPCResponse {
        val bodyBytes = request.toEncodedRpcMessage(objectMapper)
        val result = AsyncResult.incomplete<RPCResponse>()
        vertx.runOnContext {
            if (closed) {
                result.completeExceptionally(ConnectionClosedException())
            } else {
                val message = RPCMessage(bodyBytes)
                val requestNumber: Int = message.requestNumber()
                awaitingAsyncResponse[requestNumber] = result
                val bytes: Bytes =
                    encodeRequest(message.body(), requestNumber, *request.rPCFlags)
                logOutgoingRequest(message)
                sendBytes(bytes)
            }
        }
        return result.await()
    }

    @Throws(JsonProcessingException::class)
    override fun openStream(
        request: RPCStreamRequest,
        streamFactory: Function<Runnable, ScuttlebuttStreamHandler>
    ) {
        val bodyBytes = request.toEncodedRpcMessage(objectMapper)
        val synchronizedRequest = Handler { _: Void? ->
            val rpcFlags: Array<RPCFlag> = request.rPCFlags
            val message = RPCMessage(bodyBytes)
            val requestNumber: Int = message.requestNumber()
            val requestBytes: Bytes =
                encodeRequest(message.body(), requestNumber, *rpcFlags)
            val closeStreamHandler = {
                // Run on vertx context because this callback may be called from a different
                // thread by the caller
                vertx.runOnContext {
                        endStream(requestNumber)
                }
            }
            val scuttlebuttStreamHandler: ScuttlebuttStreamHandler =
                streamFactory.apply(closeStreamHandler)

            if (closed) {
                Log.e(TAG, "Connection $requestNumber closed, cannot open stream.")
                scuttlebuttStreamHandler.onStreamError(ConnectionClosedException())
            } else {
                streams[requestNumber] = scuttlebuttStreamHandler
                logOutgoingRequest(message)
                sendBytes(requestBytes)
            }
        }
        vertx.runOnContext(synchronizedRequest)
    }

    private fun logOutgoingRequest(rpcMessage: RPCMessage) {
        val requestString = rpcMessage.asString()
        val logMessage = String.format(
            "> [%d]: %s",
            rpcMessage.requestNumber(),
            requestString
        )
        Log.d(TAG, logMessage)
    }

    override fun close() {
        vertx.runOnContext { terminationFn.run() }
    }

    override fun receivedMessage(message: Bytes) {
        val synchronizedHandleMessage = Handler { _: Void? ->
            val rpcMessage = RPCMessage(message)
            // A negative request number indicates that this is a response, rather than a request that this node
            // should service
            if (rpcMessage.requestNumber() < 0) {
                    handleResponse(rpcMessage)

            } else {
                handleRequest(rpcMessage)
            }
        }
        vertx.runOnContext(synchronizedHandleMessage)
    }

    override fun streamClosed() {
        val synchronizedCloseStream = Handler { _: Void? ->
            closed = true
            streams.forEach { (_: Int, streamHandler: ScuttlebuttStreamHandler) ->
                streamHandler.onStreamError(
                    ConnectionClosedException()
                )
            }
            streams.clear()
            awaitingAsyncResponse.forEach { (_: Int, value: CompletableAsyncResult<RPCResponse>) ->
                if (!value.isDone) {
                    value.completeExceptionally(ConnectionClosedException())
                }
            }
            awaitingAsyncResponse.clear()
        }
        vertx.runOnContext(synchronizedCloseStream)
    }

    private fun handleRequest(rpcMessage: RPCMessage) {
        val logMessage =
            String.format("< [%s] %s", rpcMessage.requestNumber(), rpcMessage.asString())
        Log.d(TAG, logMessage)
        val rpcFlags = rpcMessage.rpcFlags()
        val isStream = RPCFlag.Stream.STREAM.isApplied(rpcFlags)
        if (!isStream) {
            Log.w(
                TAG,
                String.format("incoming RPC request not implemented: %s", rpcMessage.asString())
            )

        } else {
            val isEnd = RPCFlag.EndOrError.END.isApplied(rpcFlags)
            if (isEnd) {
                Log.d(TAG, rpcMessage.requestNumber().toString() + " ends ack OK")
            } else {
                onRPCRequest(rpcMessage)
            }
        }
    }


    private fun handleResponse(response: RPCMessage) {
        val requestNumber = response.requestNumber() * -1
        if (response.bodyType() == RPCFlag.BodyType.BINARY) {
            //Log.d(TAG, "[%d] incoming response: binary data".format(requestNumber))
        } else {
            val logMessage =
                String.format("[%d] incoming response: %s", requestNumber, response.asString())
            Log.d(TAG, logMessage)
        }


        val rpcFlags = response.rpcFlags()
        val isStream = RPCFlag.Stream.STREAM.isApplied(rpcFlags)

        val exception = response.getException(objectMapper)
        if (isStream) {
            val scuttlebuttStreamHandler = streams[requestNumber]
            if (scuttlebuttStreamHandler != null) {
                if (response.isSuccessfulLastMessage) {
                    // Confirm our end of the stream close and inform the consumer of the stream that it is closed
                    endStream(requestNumber)
                } else if (exception.isPresent) {
                    scuttlebuttStreamHandler.onStreamError(exception.get())
                } else {
                    val successfulResponse = RPCResponse(response.body(), response.bodyType())
                    scuttlebuttStreamHandler.onMessage(response.requestNumber(), successfulResponse)
                }
            } else {
                Log.w(
                    TAG,
                    "Couldn't find stream handler for RPC response with request number " +
                            requestNumber +
                            " " +
                            response.asString()
                )
            }
        } else {
            val rpcMessageFuture = awaitingAsyncResponse.remove(requestNumber)
            if (rpcMessageFuture != null) {
                if (exception.isPresent) {
                    rpcMessageFuture.completeExceptionally(exception.get())
                } else {
                    val successfulResponse = RPCResponse(response.body(), (response.bodyType()))
                    rpcMessageFuture.complete(successfulResponse)
                }
            } else {
                Log.w(
                    TAG,
                    "Couldn't find async handler for RPC response with request number " +
                            requestNumber +
                            " " +
                            response.asString()
                )
            }
        }
    }

    fun sendBytes(bytes: Bytes) {
        messageSender.accept(bytes)
    }

    /**
     * Sends an stream close message over the RPC channel to for the given request number if we have not already closed
     * our end of the stream.
     *
     * Removes the stream handler from the state, so any newly incoming messages until the other side of the stream has
     * closed its end will be ignored.
     *
     * @param requestNumber the request number of the stream to send a close message over RPC for
     */
     fun endStream(requestNumber: Int) {
        try {
            val streamHandler = streams.remove(requestNumber)
            // Only send the message if the stream hasn't already been closed at our end
            if (streamHandler != null) {
                val streamEnd = encodeStreamEndRequest(requestNumber)
                streamHandler.onStreamEnd()
                val logMessage = String.format("[%d] Sending close stream message.", requestNumber)
                Log.d(TAG, logMessage)
                sendBytes(streamEnd)
            } else {
                Log.d("RCPHandler","stream $requestNumber have been closed at other end")
            }
        } catch (e: JsonProcessingException) {
            Log.e(
                TAG,
                "Unexpectedly could not encode stream end message to JSON. %s".format(e.message)
            )
        }
    }

    /**
     * Sends an end blob message over the RPC channel to for the given request number
     * @param requestNumber the request number of the stream to send a close message over RPC for
     */
    fun sendEndBlob(requestNumber: Int) {
        val streamBlobEnd = encodeBlobEnd(requestNumber)
        sendBytes(streamBlobEnd)
    }

    /**
     * Sends an blob slice message over the RPC channel to for the given request number
     * @param  requestNumber the request number of the stream to send a close message over RPC for
     * @param  buff the blob slice to send
     */
    fun sendBlobSlice(requestNumber: Int, buff: Bytes) {
        val streamBlobSlice = encodeBlobSlice(requestNumber, buff)
        sendBytes(streamBlobSlice)
    }

    companion object {
        private val TAG = RPCHandler::class.java.canonicalName
        private val objectMapper = ObjectMapper()
    }
}
