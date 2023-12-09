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
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Destroyable;

import in.delog.libsodium.SodiumUtils;

/**
 * Allocated objects track allocation of memory using Sodium.
 *
 * @see <a href="https://libsodium.gitbook.io/doc/memory_management">Secure memory</a>
 */
public final class Allocated implements Destroyable {

    private final int length;
    @Nullable
    private byte[] ptr;

    Allocated(byte[] ptr, int length) {
        this.ptr = ptr;
        this.length = length;
    }

    /**
     * Assign bytes using Sodium memory allocation
     *
     * @param bytes the bytes to assign
     * @return a new allocated value filled with the bytes
     */
    public static Allocated fromBytes(Bytes bytes) {
        Allocated allocated = Allocated.allocate(bytes.size());
        allocated.ptr = bytes.toArrayUnsafe().clone();
        return allocated;
    }

    /**
     * Allocate bytes using Sodium memory allocation
     *
     * @param length the length of the memory allocation, in bytes
     * @return a new allocated value
     */
    static Allocated allocate(long length) {
//    SWIGTYPE_p_void ptr = Sodium.sodium_malloc((int) length);
        return new Allocated(new byte[(int) length], (int) length);
    }

    int length() {
        return length;
    }


    /**
     * Provides the bytes of this key.
     *
     * @return The bytes of this key.
     */
    public Bytes bytes() {
        return Bytes.wrap(bytesArray());
    }

    /**
     * Provides the bytes of this key.
     *
     * @return The bytes of this key.
     */
    public byte[] bytesArray() {
        return ptr;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (isDestroyed()) {
            throw new IllegalStateException("allocated value has been destroyed");
        }
        return SodiumUtils.hashCode(ptr, length);
    }
}
