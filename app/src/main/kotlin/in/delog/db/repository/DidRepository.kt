package `in`.delog.repository

import `in`.delog.db.model.IdentAndAbout
import `in`.delog.db.model.getHttpScheme
import `in`.delog.db.model.isOnion
import `in`.delog.service.HttpClient
import `in`.delog.service.ssb.TorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

interface DidRepository {
    suspend fun checkIfValid(identAndAbout: IdentAndAbout?, name: String?): DidValid
}

data class DidValid(
    val valid: Boolean,
    val error: String?
)

class DidRepositoryImpl() : DidRepository {

    override suspend fun checkIfValid(identAndAbout: IdentAndAbout?, name: String?): DidValid {
        if (identAndAbout == null) return DidValid(false, null)
        if (name.isNullOrEmpty()) return DidValid(false, null)
        val httpScheme = identAndAbout.ident.getHttpScheme()
        val server = identAndAbout.ident.server
        val url = "${httpScheme}${server}/.well-known/ssb/about/${name}"
        val textResponse: String = withContext(Dispatchers.Default) {
            try {
                if (identAndAbout.ident.isOnion()) {
                    val conn = URL(url).openConnection(HttpClient.getTorProxy())
                    conn.getInputStream().buffered().reader().use { it.readText() }
                } else {
                        URL(url).readText()
                }
            } catch (e: Exception) {
                JSONObject().put("error", e.message).toString()
            }
        }
        val jsonObject = JSONObject(textResponse)
        if (jsonObject.has("error")) {
            return DidValid(false, jsonObject.getString("error"))
        }
        if (jsonObject.has("content") && jsonObject.getJSONObject("content").has("about")) {
            if (jsonObject.getJSONObject("content")
                    .getString("about") != identAndAbout.ident.publicKey
            ) {
                return DidValid(false, null)
            }
        }
        return DidValid(true, null)
    }
}
