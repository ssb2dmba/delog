/**
 * Delog
 * Copyright (C) 2023 dmba.info
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package in.delog.libsodium;

import android.os.Build;

import androidx.annotation.RequiresApi;

import org.apache.tuweni.crypto.sodium.SodiumException;

import java.util.function.BiFunction;

public class SodiumUtils {

    public static byte[] dup(byte[] pointer, int length) {
        return pointer.clone();
    }

    public static byte[] reify(SWIGTYPE_p_void ptr, int length) {
        byte[] bytes = new byte[length];
        //ptr.get(0, bytes, 0, bytes.length);
        return bytes;

    }

    static byte[] dup(byte[] bytes) {
        return bytes.clone();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static <T> T dup(byte[] bytes, BiFunction<byte[], Integer, T> ctr) {
        byte[] ptr = bytes.clone();
        try {
            return ctr.apply(ptr, bytes.length);
        } catch (Throwable e) {
            ptr = null;
            throw e;
        }
    }


    public static int hashCode(byte[] ptr, int length) {
        int result = 1;
        for (int i = 0; i < length; ++i) {
            result = 31 * result + ((int) ptr[i]);
        }
        return result;
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    public static <T> T scalarMultBase(byte[] n, long nlen, BiFunction<byte[], Long, T> ctr) {
        if (nlen != Sodium.crypto_scalarmult_scalarbytes()) {
            throw new IllegalArgumentException(
                    "secret key length is " + nlen + " but required " + Sodium.crypto_scalarmult_scalarbytes());
        }
        long qbytes = Sodium.crypto_scalarmult_bytes();
        byte[] dst = new byte[(int) qbytes];
        try {
            int rc = Sodium.crypto_scalarmult_base(dst, n);
            if (rc != 0) {
                throw new SodiumException("crypto_scalarmult_base: failed with result " + rc);
            }
            return ctr.apply(dst, qbytes);
        } catch (Throwable e) {
            dst = null;
            throw e;
        }
    }


    static byte[] dupAndIncrement(byte[] src, int length) {
        byte[] ptr = dup(src, length);
        try {
            Sodium.sodium_increment(ptr, length);
            return ptr;
        } catch (Throwable e) {
            ptr = null;
            throw e;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static <T> T dupAndIncrement(byte[] src, int length, BiFunction<byte[], Integer, T> ctr) {
        byte[] ptr = SodiumUtils.dupAndIncrement(src, length);
        try {
            return ctr.apply(ptr, length);
        } catch (Throwable e) {
            ptr = null;
            throw e;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    public static <T> T randomBytes(int length, BiFunction<byte[], Integer, T> ctr) {
        byte[] r = new byte[length];
        Sodium.randombytes(r, length);
        try {
            return ctr.apply(r, length);
        } catch (Throwable e) {
            r = null;
            throw e;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    public static <T> T scalarMult(byte[] n, long nlen, byte[] p, long plen, BiFunction<byte[], Long, T> ctr) {
        if (nlen != Sodium.crypto_scalarmult_scalarbytes()) {
            throw new IllegalArgumentException(
                    "secret key length is " + nlen + " but required " + Sodium.crypto_scalarmult_scalarbytes());
        }
        if (plen != Sodium.crypto_scalarmult_bytes()) {
            throw new IllegalArgumentException(
                    "public key length is " + plen + " but required " + Sodium.crypto_scalarmult_bytes());
        }
        long qbytes = Sodium.crypto_scalarmult_bytes();
        byte[] dst = new byte[(int) qbytes];
        try {
            int rc = Sodium.crypto_scalarmult(dst, n, p);
            if (rc != 0) {
                throw new SodiumException("crypto_scalarmult_base: failed with result " + rc);
            }
            return ctr.apply(dst, qbytes);
        } catch (Throwable e) {
            dst = null;
            throw e;
        }
    }

}
