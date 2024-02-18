package `in`.delog.db

import androidx.room.DatabaseView

class AppDatabaseView {
    @DatabaseView(
        "WITH RECURSIVE tree_view AS (" +
                "       SELECT " +
                "           0 AS level, " +
                "           author as pauthor, " +
                "           a1.name, " +
                "           a1.image, " +
                "           name as pname," +
                "           CAST(message.key AS varchar(50)) AS parents, " +
                "           message.timestamp as ts, " +
                "           (select count(*) from message x where x.branch=message.key and x.type='post') as replies, " +
                "           (select count(*) from message x where x.branch=message.key and x.type='vote') as votes, " +
                "           message.key, " +
                "           message.author, " +
                "           message.timestamp, " +
                "           message.contentAsText, " +
                "           message.root, " +
                "           message.branch " +
                "       FROM " +
                "           message, about a1 " +
                "       WHERE " +
                "           message.author=a1.about and message.root IS NULL and message.type ='post' " +
                "       UNION ALL SELECT " +
                "           level + 1 AS level, " +
                "           cast(pauthor  as varchar(255)) pauthor," +
                "           a2.name, a2.image, " +
                "           CAST(pname  as varchar(50)) as pname," +
                "           CAST(parents|| '_' || CAST(m2.key AS VARCHAR (50)) AS VARCHAR(50)) AS parents, " +
                "           min(ts,m2.timestamp) as ts, " +
                "           (select count(*) from message x where x.branch=x.key and x.type='post') as replies," +
                "           (select count(*) from message x where x.branch=x.key and x.type='vote') as votes," +
                "           m2.key," +
                "           m2.author, " +
                "           m2.timestamp, " +
                "           m2.contentAsText, " +
                "           m2.root," +
                "           m2.branch " +
                "       FROM message m2, about a2 " +
                "       JOIN tree_view tv ON m2.branch = tv.key and m2.type=\"post\" " +
                "       WHERE a2.about=m2.author) " +
                "select * from tree_view order by ts desc, parents asc",
        "MessageTree"
    )
    data class MessageInTree(
        val level: Long,
        val pauthor: String,
        val name: String?,
        val image: String?,
        val pName: String?,
        val parents: String,
        val ts: Long,
        val replies: Long,
        val votes: Long,
        val key: String,
        val author: String,
        val timestamp: Long,
        val contentAsText: String,
        val root: String?,
        val branch: String?
    )
}




