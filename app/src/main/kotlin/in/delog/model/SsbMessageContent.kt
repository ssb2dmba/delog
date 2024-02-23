package `in`.delog.model

import `in`.delog.service.ssb.BaseSsbService
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Mention(
    val link: String,
    val name: String,
    val type: String?=null,
    val size: Long?=null
)


@Serializable
data class SsbMessageContent(
    var text: String? = null,
    var type: String,
    val contentWarning: String? = null,
    val about: String? = null,
    var image: String? = null,
    val name: String? = null,
    var root: String? = null,
    var branch: String? = null,
    var description: String? = null,
    var mentions: Array<Mention>? = null
) {
    companion object {
        fun  empty(): SsbMessageContent {
            return SsbMessageContent (type = "post")
        }

        fun serialize(str: String): SsbMessageContent {
            if (str.isNullOrBlank()) {
                return SsbMessageContent(type = "post")
            }
            return Json.decodeFromString(
                serializer(),
                str
            )
        }

    }

    fun deserialize(): String {
        return BaseSsbService.format.encodeToString(
            serializer(),
            this
        )
    }

}