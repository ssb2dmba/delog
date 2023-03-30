/**
 * Delog
 * Copyright (C) 2023 dmba.info
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package `in`.delog.ssb

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.sodium.Signature
import java.nio.ByteOrder
import kotlin.math.log2


private fun shr256(x: IntArray, n: Int) {
    var carry = 0
    val mask = (1 shl n) - 1
    for (i in 8 downTo 0) {
        val newCarry = (x[i] and mask) shl (32 - n)
        x[i] = carry or (x[i] ushr n)
        carry = newCarry
    }
}


private fun shl256(x: IntArray, n: Int) {
    var carry = 0
    val mask = ((1 shl n) - 1) shl (32 - n)
    for (i in 0 until 9) {
        val newCarry = (x[i] and mask) ushr (32 - n)
        x[i] = carry or (x[i] shl n)
        carry = newCarry
    }
}


private fun parity(_sum: Int, v: Int, n: Int): Int {
    var sum = _sum
    for (i in 0 until 32 step n) {
        sum = sum xor (v ushr i)
    }
    return sum
}


class Dict(val words: Array<String>) {
    val bits = log2(words.size.toDouble()).toInt()
    val mask = words.size - 1
    val nword = Math.floor(256.0 / bits).toInt() + 1

    init {
        if (1 shl bits != words.size) {
            throw Error("dict must be exactly power of 2 words, got ${words.size}")
        }
    }

    val checkbits = nword * bits - 256
    val checkmask = (1 shl checkbits) - 1
}


fun secretKeyToMnemonic(sk: ByteArray, dict: Dict): List<String> {
    val sk = sk.sliceArray(0..31)
    val t = IntArray(9)
    var sum = 0
    val dv = Bytes.wrap(sk)
    for (i in 0 until 8) {
        val v = dv.getInt(i * 4, ByteOrder.LITTLE_ENDIAN)
        sum = parity(sum, v, dict.checkbits)
        t[i] = v
    }
    t[8] = 0
    shl256(t, dict.checkbits)
    t[0] = t[0] or (sum and dict.checkmask)
    val phrase = mutableListOf<String>()
    for (i in 0 until dict.nword) {
        val idx = t[0] and dict.mask
        val word = dict.words[idx]
        phrase.add(word)
        shr256(t, dict.bits)
    }
    return phrase
}


fun mnemonicToSignature(phrase: List<String>, dict: Dict): Signature.KeyPair? {
    if (phrase.size != dict.nword) {
        throw IllegalArgumentException("phrase must be exactly ${dict.nword} words")
    }
    val t = IntArray(9)
    for (i in dict.nword - 1 downTo 0) {
        val idx = dict.words.indexOf(phrase[i])
        if (idx < 0) return null
        shl256(t, dict.bits)
        t[0] = t[0] or idx
    }

    var sum = 0
    val sum2 = t[0] and dict.checkmask
    shr256(t, dict.checkbits)
    var buffer = ByteArray(32);
    for (i in 0 until 8) {
        sum = parity(sum, t[i], dict.checkbits)
        write8BytesToBuffer(buffer, i * 4, t[i])
    }
    if (sum and dict.checkmask != sum2) return null
    val seedValue = Signature.Seed.fromBytes(buffer)
    return Signature.KeyPair.fromSeed(seedValue)
}


private fun write8BytesToBuffer(buffer: ByteArray, offset: Int, data: Int) {
    buffer[offset + 0] = (data shr 0).toByte()
    buffer[offset + 1] = (data shr 8).toByte()
    buffer[offset + 2] = (data shr 16).toByte()
    buffer[offset + 3] = (data shr 24).toByte()
}
