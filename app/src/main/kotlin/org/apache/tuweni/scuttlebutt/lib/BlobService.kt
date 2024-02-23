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

import android.util.Log
import androidx.core.net.toFile
import androidx.lifecycle.LifecycleService
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import `in`.delog.MainApplication
import `in`.delog.db.repository.BlobRepository
import `in`.delog.service.ssb.SsbService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.scuttlebutt.lib.model.StreamHandler
import org.apache.tuweni.scuttlebutt.rpc.RPCBlobRequest
import org.apache.tuweni.scuttlebutt.rpc.RPCCodec
import org.apache.tuweni.scuttlebutt.rpc.RPCFlag
import org.apache.tuweni.scuttlebutt.rpc.RPCFunction
import org.apache.tuweni.scuttlebutt.rpc.RPCMessage
import org.apache.tuweni.scuttlebutt.rpc.RPCRequestBody
import org.apache.tuweni.scuttlebutt.rpc.RPCResponse
import org.apache.tuweni.scuttlebutt.rpc.RPCStreamRequest
import org.apache.tuweni.scuttlebutt.rpc.mux.ConnectionClosedException
import org.apache.tuweni.scuttlebutt.rpc.mux.RPCHandler
import org.apache.tuweni.scuttlebutt.rpc.mux.ScuttlebuttStreamHandler
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.function.Function


/**
 * A service for operations that connect nodes together and other network related operations
 *
 *
 * Assumes the standard 'ssb-gossip' plugin is installed and enabled on the node that we're connected to (or that RPC
 * functions meeting its manifest's contract are available.).
 *
 *
 * Should not be constructed directly, should be used via an ScuttlebuttClient instance.
 */
class BlobService(
    private val multiplexer: RPCHandler,
    private val blobRepository: BlobRepository
): LifecycleService() {
    companion object {
        // We don't represent all the fields returned over RPC in our java classes, so we configure the mapper
        // to ignore JSON fields without a corresponding Java field
        private val mapper =
            ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)
    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    @Throws(JsonProcessingException::class, ConnectionClosedException::class)
    fun createWantStream(streamHandler: Function<Runnable?, StreamHandler<HashMap<String, Long>?>>) {
        val function = RPCFunction(listOf("blobs"), "createWants")
        val request = RPCStreamRequest(function, listOf())
        multiplexer.openStream(
            request
        ) { closer: Runnable ->
            object : ScuttlebuttStreamHandler {

                var wantStream: StreamHandler<HashMap<String, Long>?> =
                    streamHandler.apply(closer)

                override fun onMessage(requestNumber: Int, message: RPCResponse) {
                    try {
                        val str = message.body().toArrayUnsafe()
                        val map = mapper.readValue<HashMap<String, Long>>(str)
                        wantStream.onMessage(map)
                    } catch (e: IOException) {
                        wantStream.onStreamError(e)
                        closer.run()
                    }
                }

                override fun onStreamEnd() {
                    wantStream.onStreamEnd()
                }

                override fun onStreamError(ex: Exception) {
                    wantStream.onStreamError(ex)
                }
            }
        }
    }

    /**
     * we asked server a createWantStream RPC and we got a response
     * we reply with the matching list of blob we have
     * (this shall be enhanced for further replication as for ex. storing the want for someone else)
     */
    fun wantStreamHandler(runnable: Runnable?): StreamHandler<HashMap<String, Long>?> {
        return object : StreamHandler<HashMap<String, Long>?> {

            override fun onMessage(item: HashMap<String, Long>?) {
                val wants:HashMap<String, Long> = HashMap()
                scope.launch {
                    for (key in item!!.keys) {
                        val blobItem = blobRepository.getBlobItem(key)
                        if (blobItem != null) {
                            wants[key] = blobItem.size
                        }
                    }
                    val responseString: String = JSONObject((wants as Map<String, Long>?)!!).toString()
                    val response = RPCCodec.encodeResponse(
                        Bytes.wrap(responseString.toByteArray()),
                        1,
                        RPCFlag.Stream.STREAM,
                        RPCFlag.BodyType.JSON
                    )
                    multiplexer.sendBytes(response)
                    multiplexer.endStream(1 * -1)
                }
            }

            override fun onStreamEnd() {

            }

            override fun onStreamError(ex: Exception?) {
                ex?.printStackTrace()
                MainApplication.toastify(ex?.message ?: "error on wantStreamHandler")
            }
        }
    }

    fun onBlobsRPC(clientId: String, rpcRequestBody: RPCRequestBody, rpcMessage: RPCMessage) {
        if (rpcRequestBody.name.size < 2) {
            Log.w(SsbService.TAG, "unhandled blob function : " + rpcMessage.asString())
            return
        }
        when (rpcRequestBody.name[1]) {
            "get" -> onRPCBlobsGet(rpcMessage)
            "createWants" -> onRPCBlobsCreateWants(clientId, rpcMessage)
            else -> Log.w(SsbService.TAG, "unhandled blob function : " + rpcMessage.asString())
        }
    }


    /**
     *  server asked us blob.createWants and we reply all blobs we need to get
     */
    private fun onRPCBlobsCreateWants(clientId: String, rpcMessage: RPCMessage) {
        scope.launch {
            val wants = blobRepository.getWants(clientId)
            val responseString: String = JSONObject((wants as Map<String, Long>?)!!).toString()
            val response = RPCCodec.encodeResponse(
                Bytes.wrap(responseString.toByteArray()),
                rpcMessage.requestNumber(),
                rpcMessage.rpcFlags()
            )
            val requestNumber = rpcMessage.requestNumber() * -1
            Log.d(" onRPCBlobsCreateWants", "$requestNumber > $wants")
            multiplexer.sendBytes(response)
            multiplexer.endStream(requestNumber)
        }

    }

    /**
     *  return file as stream of bytes if possible
     */
    private fun onRPCBlobsGet(rpcMessage: RPCMessage) {
        val rpcStreamRequest = rpcMessage.asJSON(SsbService.objectMapper, RPCBlobRequest::class.java)
        val key = rpcStreamRequest.key
        scope.launch {
            val blobItem = blobRepository.getBlobItem(key)
            if (blobItem!=null) {
                val file = blobItem.uri.toFile()
                val size = file.length().toInt()
                val bytes = ByteArray(size)
                var offset = 0
                var oldRead =0
                try {
                    val buff = ByteArray(1230)
                    File(blobItem.uri.path).inputStream().buffered().use { input ->
                        while(true) {
                            val sz = input.read(buff)
                            if (sz <= 0) break
                            offset += sz
                            val pct: Int = offset * 100 / size
                            if (oldRead!=pct) {
                                oldRead = pct
                                println("read $pct $offset $size")
                            }
                            ///at that point we have a sz bytes in the buff to process
                            multiplexer.sendBytes(Bytes.wrap(bytes))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    MainApplication.toastify(e.toString())
                } finally {
                    Log.d("onRPCBlobsGet", "file sent")
                    multiplexer.endStream(rpcMessage.requestNumber() * -1)
                }
            }
        }
    }



}
