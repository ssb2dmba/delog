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
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

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

    ) {
}

/*
WITH RECURSIVE tree_view AS (
select  m1.*  from message m1
union all (select * from  message)  m2 on m2.`key`=m1.root
where  m1.author = "@Edxyzw78VTaOeBDZE7Mf3tY4RnTDB7uscGQwjaWqXa8=.ed25519" or m1.author IN (select follow from contact where author = "@Edxyzw78VTaOeBDZE7Mf3tY4RnTDB7uscGQwjaWqXa8=.ed25519"  and value = 1)
order by min(coalesce(m2.timestamp ,9223372036854775807),m1.timestamp) desc, m2.timestamp asc
)
select * from tree_view

WITH RECURSIVE tree_view AS (
    SELECT
         0 AS level,
         author as pauthor,
         CAST(message.timestamp AS varchar(50)) AS order_sequence,
         message.timestamp as ts,
         (select count(*) from message x where x.root=message.`key`) as ct,
         message.*
    FROM message, about a1
    WHERE message.author=a1.about and message.root IS NULL
UNION ALL
    SELECT
    level + 1 AS level,
    cast(pauthor  as varchar(255)) pauthor,
    CAST(order_sequence || '_' || CAST(m2.timestamp AS VARCHAR (50)) AS VARCHAR(50)) AS order_sequence,
    min(ts,m2.timestamp) as ts,
             (select count(*) from message x where x.root=x.`key`) as ct,
    m2. *
    FROM message m2, about a2
    JOIN tree_view tv
      ON m2.root = tv.`key`
            WHERE a2.about=m2.author
)
select * from tree_view order by ts desc, order_sequence desc;
 */
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

    fun getNetworkIdentifier(): String {
        if (this.about?.name == null || this.about?.name!!.isEmpty()) {
            return this.ident.publicKey.subSequence(0, 5).toString()
        }
        val server = if (this.ident.server.isNullOrEmpty()) "" else "@" + this.ident.server
        val name = this.about?.name + server
        return name
    }

    companion object {
        fun empty(about: String): Ident {
            return Ident(
                -1,
                publicKey = about,
                "",
                -1,
                "",
                false,
                -1,
                "",
                null
            )
        }
    }
}

data class ContactAndAbout(
    @Embedded val contact: Contact,
    @Relation(
        parentColumn = "follow",
        entityColumn = "about"
    )
    var about: About?
)