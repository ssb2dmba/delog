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

import android.content.ContentResolver
import android.util.Log
import androidx.core.net.toUri
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import `in`.delog.MainApplication
import `in`.delog.db.repository.BlobRepository
import `in`.delog.service.ssb.SsbService
import `in`.delog.service.ssb.SsbService.Companion.TAG
import `in`.delog.service.ssb.SsbUIState
import `in`.delog.viewmodel.FeedMainUIState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.crypto.sodium.SHA256Hash
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
import java.util.concurrent.ConcurrentHashMap


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
    private val blobRepository: BlobRepository,
    private val _uiState: MutableStateFlow<SsbUIState>
)  {

    companion object {
        // We don't represent all the fields returned over RPC in our java classes, so we configure the mapper
        // to ignore JSON fields without a corresponding Java field
        private val mapper =
            ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        const val MAX_BLOB_SIZE = 25 * 1024 * 1024
        const val BUFFER_SIZE = 4 * 1024
    }

    private val runningFileHandler: MutableMap<String, File> = ConcurrentHashMap()

    private var wantRequestNumber: Int? = null

    val context = MainApplication.applicationContext()

    private val contentResolver: ContentResolver = this.context.contentResolver


    private var blobSize = ConcurrentHashMap<String,Long>()
    private var blobDownStatus = ConcurrentHashMap<String,Long>()
    private var blobUpStatus = ConcurrentHashMap<String,Long>()

    private val serverHas: HashMap<String, Long> = HashMap()

    /**
     * handle RPC messages for namespace 'blobs'
     */
    fun onBlobsRPC(clientId: String, rpcRequestBody: RPCRequestBody, rpcMessage: RPCMessage) {
        if (rpcRequestBody.name.size < 2) {
            Log.w(TAG, "unhandled blob function : " + rpcMessage.asString())
            return
        }
        when (rpcRequestBody.name[1]) {
            "get" -> onRPCBlobsGet(rpcMessage)
            "createWants" -> onRPCBlobsCreateWants(clientId, rpcMessage)
            else -> Log.w(TAG, "unhandled blob function : " + rpcMessage.asString())
        }
    }

    /**
     * Send blobs.createWants TPC to the server and setup receive stream
     */
    @Throws(JsonProcessingException::class, ConnectionClosedException::class)
    fun createWantStream(author: String) {
        val rpcFunction = RPCFunction(listOf("blobs"), "createWants")
        val request = RPCStreamRequest(rpcFunction, listOf())
        val streamEnded = AsyncResult.incomplete<Void>()
        multiplexer.openStream(
            request
        ) { _: Runnable ->
            object : ScuttlebuttStreamHandler {

                override fun onMessage(requestNumber: Int, message: RPCResponse) {
                        val str = message.body().toArrayUnsafe()
                        val map = mapper.readValue<HashMap<String, Long>>(str)
                        onHasMessage(multiplexer,  author, map)
                }

                override fun onStreamEnd() {
                    streamEnded.complete(null)
                }

                override fun onStreamError(ex: Exception) {
                    streamEnded.completeExceptionally(ex)
                }
            }
        }
    }


    private fun onHasMessage(
        multippx: RPCHandler,
        author: String,
        item: HashMap<String, Long>?
    ) {

        val serverWant: HashMap<String, Long> = HashMap()
        for (key in item!!.keys) {
            val blobItem = runBlocking(Dispatchers.IO) { blobRepository.getBlobItem(key) }
            if (item[key]!! > 0) {
                    serverHas[key] = item[key]!!
                    blobSize[key] = item[key]!!
            } else {
                if (blobItem != null) {
                    serverWant[key] = blobItem.size
                    blobSize[key] = blobItem.size
                }
            }
            _uiState.update { it.copy(blobSize = HashMap(blobSize)) }
        }


        val responseString: String = JSONObject((serverWant as Map<String, Long>?)!!).toString()
        val response = RPCCodec.encodeResponse(
            Bytes.wrap(responseString.toByteArray()),
             wantRequestNumber!!,
            RPCFlag.BodyType.JSON,
            RPCFlag.Stream.STREAM
        )
        multippx.sendBytes(response)

        val wants = runBlocking { blobRepository.getWants(author) }
        for (key in serverHas.keys) {
            if (wants.containsKey(key)) {
                createBlobGetStream(author, key, serverHas[key])
                break // limit to one blob at a time
            }
        }

    }

    /**
     * Send blobs.get RPC to the server and setup receive stream
     */
    @Throws(JsonProcessingException::class, ConnectionClosedException::class)
    fun createBlobGetStream(author: String, hash: String, size: Long? = null, max: Long?=null) {
        if (runningFileHandler.containsKey(hash)) {
            Log.w(TAG, "createBlobGetStream: already running for $hash")
            return
        }
        val params = java.util.HashMap<String, Any>()
        params["hash"] = hash
        if (size!=null) params["size"] = size
        if (max!=null) params["max"] = max
        val tmpFile = blobRepository.getTempFile(hash)

        var func = RPCFunction(listOf("blobs"),"get")
//        if (tmpFile.length()>0 && size!=null && size > 0L) {
//            // TODO copy our part into the tmp File
//            func = RPCFunction(listOf("blobs"),"getSlice")
//            params["start"]=tmpFile.length()
//            params["end"]=size
//        }
        Log.d(TAG, "${func.asList()} $params")
        val streamRequest = RPCStreamRequest(func, listOf(params))

        val streamEnded = AsyncResult.incomplete<Void>()

        multiplexer.openStream(
            streamRequest
        ) { _: Runnable ->
            object : ScuttlebuttStreamHandler {

                override fun onMessage(requestNumber: Int, message: RPCResponse) {
                    onRPCResponseForBlobGet( hash, message)
                }

                override fun onStreamEnd() {
                    onRPCResponseForBlobGetWithEnd(author, hash)
                    streamEnded.complete(null)
                }

                override fun onStreamError(ex: Exception) {
                    onRPCResponseForBlobGetWithError(author, hash)
                    streamEnded.completeExceptionally(ex)
                }
            }
        }
    }

    /**
     * end of blobs.get RPC with success
     * remove tmp file if error
     * @param hash the hash of the blob
     */
    private  fun onRPCResponseForBlobGetWithEnd(author: String, hash: String) {

        // TODO split in a separate DRY function
        serverHas.remove(hash)
        val wants = runBlocking { blobRepository.getWants(author) }
        for (key in serverHas.keys) {
            if (wants.containsKey(key)) {
                createBlobGetStream(author, key, serverHas[key])
                break // limit to one blob at a time
            }
        }

        if (runningFileHandler.containsKey(hash)) {
            val tmpFile: File = runningFileHandler[hash]!!
            val inputStream  = contentResolver.openInputStream(tmpFile.toUri())
            if (inputStream == null) {
                Log.w(TAG, "stream blobs.get end tmp file not found: $hash")
                return
            }
            val hashed = inputStream.use { SHA256Hash.hash(SHA256Hash.Input.fromBytes(it.readBytes())) }
            inputStream.close()
            val b64hash = hashed.bytes().toBase64String()
            val key = "&$b64hash.sha256"
            if (key == hash) {
                // copy into blob to repository
                runBlocking(Dispatchers.IO) { blobRepository.update(author, tmpFile.toUri()) }
                tmpFile.delete()
                runningFileHandler.remove(hash)
                return
            }
            val debug=Bytes.fromBase64String(hash.substring(1,hash.length - 7)).toHexString()
            Log.w(TAG, "$debug end hash mismatch: $key != $hash ${tmpFile.length()}")
            try {
                contentResolver.delete(tmpFile.toUri(), null, null)
            } catch (e: Exception) {
                Log.e(TAG, "unable to delete tmp file: $hash")
            }
        } else {
            // this can happen if we already have the file
            Log.d(TAG, "stream blobs.get end tmp file not found: $hash ${runningFileHandler.keys}")
        }



    }

    /**
     * end of blobs.get RPC with error
     * remove tmp file if error
     * @param hash the hash of the blob
     */
    private fun onRPCResponseForBlobGetWithError(author: String, hash: String) {
        Log.e(TAG, "stream blobs.get error: $hash")
        // TODO split in a separate DRY function
        serverHas.remove(hash)
        val wants = runBlocking { blobRepository.getWants(author) }
        for (key in serverHas.keys) {
            if (wants.containsKey(key)) {
                createBlobGetStream(author, key, serverHas[key])
                break // limit to one blob at a time
            }
        }


        if (runningFileHandler.containsKey(hash)) {
            val tmpFile: File = runningFileHandler[hash]!!
            tmpFile.delete()
            runningFileHandler.remove(hash)
            Log.e(TAG, "stream blobs.get error: $hash, tmp file removed")
        }

    }


    /**
     * store slice of the blob in the repository
     * @param hash the hash of the blob
     * @param message the RPC message containing binary body
     */
    private fun onRPCResponseForBlobGet(hash: String, message: RPCResponse) {
        val tmpFile:File
        if (runningFileHandler.containsKey(hash)) {
            tmpFile = runningFileHandler[hash]!!
        } else {
            tmpFile = blobRepository.getTempFile(hash)
            if (!tmpFile.exists()) {
                tmpFile.createNewFile()
            } else {
                tmpFile.delete()
                tmpFile.createNewFile()
            }
            runningFileHandler[hash] = tmpFile

        }
        contentResolver.openOutputStream(tmpFile.toUri(),"wa")?.use {
            it.write(message.body().toArrayUnsafe())

        }
        blobDownStatus[hash] = tmpFile.length()
        _uiState.update { it.copy(blobDown = HashMap(blobDownStatus)) }
        Log.d(TAG, "< $hash: ${tmpFile.length()}")
    }


    /**
     *  server asked us blob.createWants and we reply all blobs we need to get
     */
    private  fun onRPCBlobsCreateWants(clientId: String, rpcMessage: RPCMessage) {
        wantRequestNumber = rpcMessage.requestNumber()
        val wants = runBlocking(Dispatchers.IO) {  blobRepository.getWants(clientId) }
        val responseString: String = JSONObject((wants as Map<String, Long>?)!!).toString()
        val response = RPCCodec.encodeResponse(
            Bytes.wrap(responseString.toByteArray()),
            wantRequestNumber!!,
            RPCFlag.BodyType.JSON,
            RPCFlag.Stream.STREAM
        )
        multiplexer.sendBytes(response)
        createWantStream(clientId)
    }

    /**
     *  return file as stream of bytes if possible
     */
    private fun onRPCBlobsGet(rpcMessage: RPCMessage) {

        val rpcStreamRequest =
            rpcMessage.asJSON(SsbService.objectMapper, RPCBlobRequest::class.java)
        val key = rpcStreamRequest.key
        val blobItem = runBlocking(Dispatchers.IO){ blobRepository.getBlobItem(key) }

        if (blobItem != null) {
            var offset = 0
            try {
                var buff = ByteArray(BUFFER_SIZE)
                File(blobItem.uri.path!!).inputStream().buffered().use { input ->
                    while (true) {
                        val sizeRead = input.read(buff)
                        if (sizeRead <= 0) break
                        if (sizeRead < BUFFER_SIZE) {
                            buff = buff.copyOfRange(0, sizeRead)
                        }
                        offset += sizeRead
                        multiplexer.sendBlobSlice(rpcMessage.requestNumber(), Bytes.wrap(buff))
                        blobUpStatus[key] = offset.toLong()
                        _uiState.update { it.copy(blobUp = HashMap(blobUpStatus)) }
                        Log.d(TAG, "> $key: $offset")
                    }
                }
                Log.d(TAG, "blob ended $key ${blobItem.size}")
                multiplexer.sendEndBlob(rpcMessage.requestNumber())
            } catch (e: Exception) {
                MainApplication.toastify(e.toString())
            } finally {

            }
        }
    }

}
