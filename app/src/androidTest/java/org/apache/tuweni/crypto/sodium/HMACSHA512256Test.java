/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.crypto.sodium;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import in.delog.libsodium.NaCl;

class HMACSHA512256Test {
    @BeforeAll
    static void checkAvailable() {
        NaCl.sodium();
    }

    @Test
    void testHmacsha512256() {
        HMACSHA512256.Key key = HMACSHA512256.Key.random();
        Bytes authenticator = HMACSHA512256.authenticate(Bytes.fromHexString("deadbeef"), key);
        assertTrue(HMACSHA512256.verify(authenticator, Bytes.fromHexString("deadbeef"), key));
    }

    @Test
    void testHmacsha512256InvalidAuthenticator() {
        HMACSHA512256.Key key = HMACSHA512256.Key.random();
        Bytes authenticator = HMACSHA512256.authenticate(Bytes.fromHexString("deadbeef"), key);
        assertThrows(
                IllegalArgumentException.class,
                () -> HMACSHA512256
                        .verify(Bytes.concatenate(authenticator, Bytes.of(1, 2, 3)), Bytes.fromHexString("deadbeef"), key));
    }

    @Test
    void testHmacsha512256NoMatch() {
        HMACSHA512256.Key key = HMACSHA512256.Key.random();
        Bytes authenticator = HMACSHA512256.authenticate(Bytes.fromHexString("deadbeef"), key);
        assertFalse(HMACSHA512256.verify(authenticator.reverse(), Bytes.fromHexString("deadbeef"), key));
    }
}
