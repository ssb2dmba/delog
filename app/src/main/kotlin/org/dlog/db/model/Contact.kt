package org.dlog.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.io.Base64

@Entity
data class Contact(

    @PrimaryKey(autoGenerate = false)
    val key: String,

    @ColumnInfo(name = "author")
    val timestamp: Int,

    @ColumnInfo(name = "author")
    val author: String,

    @ColumnInfo(name = "content_text")
    val contentText: String,

    @ColumnInfo(name = "content_type")
    val contentType: String,

)