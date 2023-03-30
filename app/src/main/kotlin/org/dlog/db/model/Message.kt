package org.dlog.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.io.Base64

@Entity
data class Message(
    @PrimaryKey(autoGenerate = true) val oid: Int,
    val publicKey: String,

    @ColumnInfo(name = "server")
    var server: String,

    @ColumnInfo(name = "port")
    var port: Int,

    @ColumnInfo(name = "private_key")
    val privateKey: String?,

    @ColumnInfo(name = "default_feed")
    val defaultFeed: Boolean,

    @ColumnInfo(name = "alias")
    var alias: String,

    @ColumnInfo(name = "sort_order")
    var sortOrder: Int



)