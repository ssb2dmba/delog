package `in`.delog.db

import androidx.room.DatabaseView
import `in`.delog.ui.component.MessageContent
import `in`.delog.ui.component.MessageViewData
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class AppDatabaseView {
    @DatabaseView("WITH RECURSIVE tree_view AS (" +
            "       SELECT " +
            "           0 AS level, " +
            "           author as pauthor, " +
            "           a1.name, a1.image, " +
            "           CAST(message.key AS varchar(50)) AS seq, " +
            "           message.timestamp as ts, " +
            "           (select count(*) from message x where x.branch=message.key) as ct, " +
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
            "           CAST(seq|| '_' || CAST(m2.key AS VARCHAR (50)) AS VARCHAR(50)) AS seq, " +
            "           min(ts,m2.timestamp) as ts, " +
            "           (select count(*) from message x where x.branch=x.key) as ct," +
            "           m2.key," +
            "           m2.author, " +
            "           m2.timestamp, " +
            "           m2.contentAsText, " +
            "           m2.root," +
            "           m2.branch " +
            "       FROM message m2, about a2 " +
            "       JOIN tree_view tv ON m2.branch = tv.key and m2.type=\"post\" " +
            "       WHERE a2.about=m2.author) " +
            "select * from tree_view order by ts desc, seq asc",
        "MessageTree"
    )
    data class MessageInTree(
        val level: Long,
        val pauthor: String,
        val seq: String,
        val ts: Long,
        val ct: Long,
        val key: String,
        val author: String,
        val timestamp: Long,
        val contentAsText: String,
        val root: String?,
        val branch: String?
    )
}

fun AppDatabaseView.MessageInTree.content(format: Json): MessageContent {
    try {
        return format.decodeFromString<MessageContent>(
            MessageContent.serializer(),
            this.contentAsText
        )
    } catch (e: SerializationException) {
        // TODO incomming .box message here
        // at insert in db with type text that should not happen
        return MessageContent(this.contentAsText, "error");
    }
}

fun AppDatabaseView.MessageInTree.toMessageViewData() = MessageViewData(
    key = key,
    timestamp = timestamp,
    author = author,
    contentAsText = contentAsText,
    root = root,
    branch = branch
)
