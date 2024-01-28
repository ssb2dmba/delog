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
@Entity
data class Draft(

    @PrimaryKey(autoGenerate = true)
    val oid: Long,

    @ColumnInfo(name = "author")
    val author: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "type")
    var type: String,

    @ColumnInfo(name = "contentAsText")
    var contentAsText: String,

    @ColumnInfo(name = "root")
    var root: String?,

    @ColumnInfo(name = "branch")
    var branch: String?

) {
    companion object {
        fun empty(author: String): Draft =  Draft(
            oid = 0,
            author = author,
            timestamp = 0L,
            type = "",
            contentAsText = "",
            root = null,
            branch = null
        )
    }
}




