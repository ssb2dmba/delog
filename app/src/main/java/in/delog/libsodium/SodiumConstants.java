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

public abstract class SodiumConstants {
    public static final int SHA256BYTES = 32;
    public static final int SHA512BYTES = 64;
    public static final int BLAKE2B_OUTBYTES = 64;
    public static final int PUBLICKEY_BYTES = 32;
    public static final int SECRETKEY_BYTES = 32;
    public static final int NONCE_BYTES = 24;
    public static final int ZERO_BYTES = 32;
    public static final int BOXZERO_BYTES = 16;
    public static final int SCALAR_BYTES = 32;
    public static final int XSALSA20_POLY1305_SECRETBOX_KEYBYTES = 32;
    public static final int XSALSA20_POLY1305_SECRETBOX_NONCEBYTES = 24;
    public static final int SIGNATURE_BYTES = 64;
    public static final int AEAD_CHACHA20_POLY1305_KEYBYTES = 32;
    public static final int AEAD_CHACHA20_POLY1305_NPUBBYTES = 8;
    public static final int AEAD_CHACHA20_POLY1305_ABYTES = 8;
    public static final int SESSIONKEYBYTES = 32;
    public static final int MAC_BYTES = 16;
    public static final int SEAL_BYTES = PUBLICKEY_BYTES + MAC_BYTES;
    public static final int RANDOMBYTES_SEEDBYTES = 32;
}
