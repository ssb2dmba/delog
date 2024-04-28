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


import org.apache.tuweni.bytes.Bytes;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import in.delog.libsodium.NaCl;
import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class ConcatenateTest {

    @BeforeClass
    public static void checkAvailable() {
        NaCl.sodium();
    }

    @Test
    public void testConcatenateTwoValues() {
        Concatenate concatenate = new Concatenate();
        Bytes random = Bytes.random(32);

        concatenate.add(Signature.PublicKey.fromBytes(random));
        concatenate.add(Signature.PublicKey.fromBytes(random));

        Allocated result = concatenate.concatenate();

        assertEquals(Bytes.concatenate(random, random), result.bytes());
    }
}
