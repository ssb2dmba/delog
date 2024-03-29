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

import org.apache.tuweni.scuttlebutt.rpc.RPCResponse

/**
 * Handles incoming items from a result stream
 */
interface ScuttlebuttStreamHandler {

    /**
     * Handles a new message from the result stream.
     *
     * @param message a new message appearing.
     */
    fun onMessage(requestNumber: Int, message: RPCResponse)

    /**
     * Invoked when the stream has been closed.
     */
    fun onStreamEnd()

    /**
     * Invoked when there is an error in the stream.
     *
     * @param ex the underlying error
     */
    fun onStreamError(ex: Exception)
}
