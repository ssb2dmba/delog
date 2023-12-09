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
package `in`.delog.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.io.Base64

@Entity
data class Ident(
    @PrimaryKey(autoGenerate = true)
    var oid: Long,

    @ColumnInfo(name = "public_key")
    val publicKey: String,

    @ColumnInfo(name = "server")
    var server: String,

    @ColumnInfo(name = "port")
    var port: Int,

    @ColumnInfo(name = "private_key")
    val privateKey: String?,

    @ColumnInfo(name = "default_ident")
    var defaultIdent: Boolean,

    @ColumnInfo(name = "sort_order")
    var sortOrder: Int,

    @ColumnInfo(name = "invite")
    var invite: String?,

    @ColumnInfo(name = "last_push")
    var lastPush: Int?,

    ) {
    companion object {
        fun getHttpScheme(server: String): String {
            var httpScheme = "https://"
            if (server.endsWith(".onion")
                || server.endsWith(".bit")
                || server.startsWith("192.168")
            ) {
                httpScheme = "http://"
            }
            return httpScheme
        }

        fun getInviteUrl(server: String): String {
            val httpScheme = getHttpScheme(server)
            return "${httpScheme}${server}/invite/"
        }


    }
}

fun Ident.isOnion(): Boolean {
    return this.server.endsWith("onion")
}

fun Ident.getHttpScheme(): String {
    return Ident.getHttpScheme(this.server)
}

fun Ident.getInviteURl(): String {
    return Ident.getInviteUrl(this.server)
}

fun Ident.asKeyPair(): Signature.KeyPair? {
    val privateKey = this.privateKey?.replace(".ed25519", "")
    val privKeyBytes = Base64.decode(privateKey)
    val secretKey = Signature.SecretKey.fromBytes(privKeyBytes)
    return Signature.KeyPair.forSecretKey(secretKey)
}