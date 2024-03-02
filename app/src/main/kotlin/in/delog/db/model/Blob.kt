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
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity( indices = [Index(value = ["key", "author"], unique = true)])
data class Blob(

    @PrimaryKey(autoGenerate = true)
    val oid: Int,

    @ColumnInfo(name = "author")
    val author: String,

    @ColumnInfo(name = "key")
    var key: String,

    @ColumnInfo(name = "type")
    var type: String?,

    @ColumnInfo(name = "size")
    var size: Long,

    @ColumnInfo(name = "own")
    var own: Boolean,

    @ColumnInfo(name = "has")
    var has: Boolean,

    @ColumnInfo(name = "contentWarning")
    val contentWarning: String?,
)