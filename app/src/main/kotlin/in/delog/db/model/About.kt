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

import androidx.room.*
import `in`.delog.db.model.Contact

@Entity
data class About(

    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "about")
    val about: String,

    @ColumnInfo(name = "name")
    var name: String? = null,

    @ColumnInfo(name = "description")
    var description: String? = null,

    @ColumnInfo(name = "image")
    var image: String? = null,

    @ColumnInfo(name = "dirty")
    var dirty: Boolean = false,
)

data class MessageAndAbout(
    @Embedded val message: Message,
    @Relation(
        parentColumn = "author",
        entityColumn = "about"
    )
    val about: About?
)

data class IdentAndAbout(
    @Embedded val ident: Ident,
    @Relation(
        parentColumn = "public_key",
        entityColumn = "about"
    )
    var about: About?
) {
    companion object
}

data class ContactAndAbout(
    @Embedded val contact: Contact,
    @Relation(
        parentColumn = "follow",
        entityColumn = "about"
    )
    var about: About?
)