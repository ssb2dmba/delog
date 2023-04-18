package `in`.delog.ssb.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.delog.db.model.Draft
import `in`.delog.db.model.Message
import `in`.delog.ssb.BaseSsbService.Companion.format
import `in`.delog.ui.component.MessageViewData
import `in`.delog.ui.component.content
import `in`.delog.ui.component.toMessageViewData
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageViewDataKtTest {


    @Test
    fun draftToMessageViewData() {
        var draft = Draft(1, "me", 1, "content")
        val mvd = draft.toMessageViewData()
        assertEquals(draft.author, mvd.author)
        assertEquals(draft.contentAsText, mvd.contentAsText)
        assertEquals(draft.timestamp, mvd.timestamp)
    }


    @Test
    fun messageToMessageViewData() {
        val msg = Message("#key", "#previous", 1, "@author", 1, "content", "post", "sig.ed25")
        val mvd = msg.toMessageViewData()
        assertEquals(msg.key, mvd.key)
        assertEquals(msg.author, mvd.author)
        assertEquals(msg.contentAsText, mvd.contentAsText)
        assertEquals(msg.timestamp, mvd.timestamp)
    }

    @Test
    fun contentMin() {
        val contentAsText = "{\"text\": \"lores ipsum ...\",\"type\": \"post\" }"
        val mvd = MessageViewData("%deadbeef", 1, "@deadbeef", contentAsText)
        val c = mvd.content(format)
        assertEquals("lores ipsum ...", c.text)
        assertEquals("post", c.type)
    }

    @Test
    fun contentMax() {
        var contentAsText =
            "{\"text\": \"lores ipsum ...\",\"type\": \"post\", \"branch\": \"#deadbeef\" , \"root\": \"#beefdead\" }"
        var mvd = MessageViewData("%deadbeef", 1, "@deadbeef", contentAsText)
        var c = mvd.content(format)
        assertEquals("lores ipsum ...", c.text)
        assertEquals("post", c.type)
        assertEquals("#beefdead", c.root)
        assertEquals("#deadbeef", c.branch)
    }

}