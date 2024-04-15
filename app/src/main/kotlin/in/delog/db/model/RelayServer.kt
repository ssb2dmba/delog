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
data class RelayServer(

    @PrimaryKey(autoGenerate = true)
    val oid: Int,

    @ColumnInfo(name = "url")
    var url: String,

    @ColumnInfo(name = "port", defaultValue = "8008")
    var port: Int = 8008,

    @ColumnInfo(name = "invite")
    val invite: String? = null,

    @ColumnInfo(name = "last",defaultValue = "0")
    val last: Int=0,

    @ColumnInfo(name = "fail_count",defaultValue = "0")
    val failCount: Int = 0,

)