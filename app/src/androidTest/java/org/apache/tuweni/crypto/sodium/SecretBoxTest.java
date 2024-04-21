
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


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static java.nio.charset.StandardCharsets.UTF_8;


import org.junit.BeforeClass;
import org.junit.Test;

import in.delog.libsodium.NaCl;


class SecretBoxTest {


    @BeforeClass
    public void checkAvailable() {
        NaCl.sodium();
        ///System.loadLibrary("libsodiumjni");
        //Sodium.sodium_init();
    }

    @Test
    public void checkCombinedEncryptDecrypt() {
        SecretBox.Key key = SecretBox.Key.random();
        SecretBox.Nonce nonce = SecretBox.Nonce.random().increment();

        byte[] message = "This is a test message".getBytes(UTF_8);

        byte[] cipherText = SecretBox.encrypt(message, key, nonce);
        byte[] clearText = SecretBox.decrypt(cipherText, key, nonce);
        assertNotNull(clearText);
        assertArrayEquals(message, clearText);

        assertNull(SecretBox.decrypt(cipherText, key, nonce.increment()));
        SecretBox.Key otherKey = SecretBox.Key.random();
        assertNull(SecretBox.decrypt(cipherText, otherKey, nonce));
    }

    @Test
    public void checkCombinedEncryptDecryptEmptyMessage() {
        SecretBox.Key key = SecretBox.Key.random();
        SecretBox.Nonce nonce = SecretBox.Nonce.random().increment();

        byte[] cipherText = SecretBox.encrypt(new byte[0], key, nonce);
        byte[] clearText = SecretBox.decrypt(cipherText, key, nonce);

        assertNotNull(clearText);
        assertEquals(0, clearText.length);
    }

    @Test
    public void checkDetachedEncryptDecrypt() {
        SecretBox.Key key = SecretBox.Key.random();
        SecretBox.Nonce nonce = SecretBox.Nonce.random().increment();

        byte[] message = "This is a test message".getBytes(UTF_8);

        DetachedEncryptionResult result = SecretBox.encryptDetached(message, key, nonce);
        byte[] clearText = SecretBox.decryptDetached(result.cipherTextArray(), result.macArray(), key, nonce);

        assertNotNull(clearText);
        assertArrayEquals(message, clearText);

        assertNull(SecretBox.decryptDetached(result.cipherTextArray(), result.macArray(), key, nonce.increment()));
        SecretBox.Key otherKey = SecretBox.Key.random();
        assertNull(SecretBox.decryptDetached(result.cipherTextArray(), result.macArray(), otherKey, nonce));
    }

    @Test
    public void checkDetachedEncryptDecryptEmptyMessage() {
        SecretBox.Key key = SecretBox.Key.random();
        SecretBox.Nonce nonce = SecretBox.Nonce.random().increment();

        DetachedEncryptionResult result = SecretBox.encryptDetached(new byte[0], key, nonce);
        byte[] clearText = SecretBox.decryptDetached(result.cipherTextArray(), result.macArray(), key, nonce);

        assertNotNull(clearText);
        assertEquals(0, clearText.length);
    }

    @Test
    public void checkCombinedEncryptDecryptWithPassword() {
        String password = "a random password";

        byte[] message = "This is a test message".getBytes(UTF_8);

        byte[] cipherText = SecretBox
                .encrypt(
                        message,
                        password,
                        PasswordHash.interactiveOpsLimit(),
                        PasswordHash.interactiveMemLimit(),
                        PasswordHash.Algorithm.recommended());
        byte[] clearText = SecretBox
                .decrypt(
                        cipherText,
                        password,
                        PasswordHash.interactiveOpsLimit(),
                        PasswordHash.interactiveMemLimit(),
                        PasswordHash.Algorithm.recommended());

        assertNotNull(clearText);
        assertArrayEquals(message, clearText);

        String otherPassword = "a different password";
        assertNull(
                SecretBox
                        .decrypt(
                                cipherText,
                                otherPassword,
                                PasswordHash.interactiveOpsLimit(),
                                PasswordHash.interactiveMemLimit(),
                                PasswordHash.Algorithm.recommended()));
    }

    @Test
    public void checkCombinedEncryptDecryptEmptyMessageWithPassword() {
        String password = "a random password";

        byte[] cipherText = SecretBox
                .encrypt(
                        new byte[0],
                        password,
                        PasswordHash.interactiveOpsLimit(),
                        PasswordHash.interactiveMemLimit(),
                        PasswordHash.Algorithm.recommended());
        byte[] clearText = SecretBox
                .decrypt(
                        cipherText,
                        password,
                        PasswordHash.interactiveOpsLimit(),
                        PasswordHash.interactiveMemLimit(),
                        PasswordHash.Algorithm.recommended());

        assertNotNull(clearText);
        assertEquals(0, clearText.length);
    }

    @Test
    public void checkDetachedEncryptDecryptWithPassword() {
        String password = "a random password";

        byte[] message = "This is a test message".getBytes(UTF_8);

        DetachedEncryptionResult result = SecretBox
                .encryptDetached(
                        message,
                        password,
                        PasswordHash.interactiveOpsLimit(),
                        PasswordHash.interactiveMemLimit(),
                        PasswordHash.Algorithm.recommended());
        byte[] clearText = SecretBox
                .decryptDetached(
                        result.cipherTextArray(),
                        result.macArray(),
                        password,
                        PasswordHash.interactiveOpsLimit(),
                        PasswordHash.interactiveMemLimit(),
                        PasswordHash.Algorithm.recommended());

        assertNotNull(clearText);
        assertArrayEquals(message, clearText);

        String otherPassword = "a different password";
        assertNull(
                SecretBox
                        .decryptDetached(
                                result.cipherTextArray(),
                                result.macArray(),
                                otherPassword,
                                PasswordHash.interactiveOpsLimit(),
                                PasswordHash.interactiveMemLimit(),
                                PasswordHash.Algorithm.recommended()));
    }

    @Test
    public void checkDetachedEncryptDecryptEmptyMessageWithPassword() {
        String password = "a random password";

        DetachedEncryptionResult result = SecretBox
                .encryptDetached(
                        new byte[0],
                        password,
                        PasswordHash.interactiveOpsLimit(),
                        PasswordHash.interactiveMemLimit(),
                        PasswordHash.Algorithm.recommended());
        byte[] clearText = SecretBox
                .decryptDetached(
                        result.cipherTextArray(),
                        result.macArray(),
                        password,
                        PasswordHash.interactiveOpsLimit(),
                        PasswordHash.interactiveMemLimit(),
                        PasswordHash.Algorithm.recommended());

        assertNotNull(clearText);
        assertEquals(0, clearText.length);
    }
}