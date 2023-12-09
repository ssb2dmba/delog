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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import in.delog.libsodium.NaCl;

class ConcatenateTest {

    @BeforeAll
    static void checkAvailable() {
        NaCl.sodium();
    }

    @Test
    void testConcatenateTwoValues() {
        Concatenate concatenate = new Concatenate();
        Bytes random = Bytes.random(32);

        concatenate.add(Signature.PublicKey.fromBytes(random));
        concatenate.add(Signature.PublicKey.fromBytes(random));

        Allocated result = concatenate.concatenate();

        assertEquals(Bytes.concatenate(random, random), result.bytes());
    }
}
