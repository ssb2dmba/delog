package org.dlog.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PubServer(

    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name="author")
    val author: String,

    @ColumnInfo(name="description")
    val description: String?,

    @ColumnInfo(name="image")
    val image: String?,

    @ColumnInfo(name="value")
    val value: Boolean?, // true follow, false: block, null: unfollow
)