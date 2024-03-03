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

import org.apache.tuweni.scuttlebutt.handshake.vertx.SecureScuttlebuttVertxClient
import org.apache.tuweni.scuttlebutt.rpc.mux.RPCHandler

/**
 * A client for making requests to a scuttlebutt instance with. This is the entry point for accessing service classes
 * which perform operations related to different logical areas.
 *
 * Should be constructed using the ScuttlebuttClientFactory factory class.
 *
 * @param multiplexer the multiplexer to make RPC requests with.
 */
class ScuttlebuttClient(
    val clientId: String,
    multiplexer: RPCHandler,
    ssbRequiredRepositories: SsbRequiredRepositories,
    secureScuttlebuttVertxClient: SecureScuttlebuttVertxClient
) {

    val secureScuttlebuttVertxClient = secureScuttlebuttVertxClient

    private val aboutRepository = ssbRequiredRepositories.aboutRepository

    private val feedRepository = ssbRequiredRepositories.feedRepository

    private val blobRepository = ssbRequiredRepositories.blobRepository


    /**
     * Provides a service for operations that concern scuttlebutt feeds.
     *
     * @return a service for operations that concern scuttlebutt feeds
     */
    val feedService = FeedService(multiplexer, blobRepository, aboutRepository, feedRepository)

    /**
     * Provides a service for operations that connect nodes together.
     *
     * @return a service for operations that connect nodes together
     */
    val blobService = BlobService(ssbRequiredRepositories.ssbService, multiplexer, blobRepository)

    /**
     * Provides a service for making lower level requests that are not supported by higher level services.
     *
     * @return a service for making lower level requests that are not supported by higher level services
     */
    val rawRequestService = RawRequestService(multiplexer)
}
