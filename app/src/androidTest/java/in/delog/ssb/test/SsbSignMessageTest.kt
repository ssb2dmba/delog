package `in`.delog.ssb.test

import `in`.delog.db.model.Ident
import `in`.delog.model.SsbMessageContent
import `in`.delog.model.SsbSignableMessage
import `in`.delog.model.SsbSignedMessage
import `in`.delog.ssb.*
import org.junit.Assert.assertEquals
import org.junit.Test


class SsbSignMessageTest {

    val feed = Ident(
        privateKey = "8CcQUI27IE+Rjj7sZ4Q9njjqcB0vizqstNYGVux/ehJilJsTn/uha5/uTrOFT/DBubbwR99SCBgTBWkOQ4B0iA==",
        oid = -1, port = 8080,
        publicKey = "",
        defaultIdent = true,
        server = "",
        sortOrder = -1,
        invite = null,
        lastPush = null
    )

    val feed2 = Ident(
        privateKey = "ZUCJ2dS2+Wn7ByTYNenQUXxK8zxrpvg07doDoenRs232FW2bkAh9hcWnijbmw1huRxqWs6Oi+e4hqBKRzobCCQ==",
        oid = -1, port = 8080,
        publicKey = "",
        defaultIdent = true,
        server = "",
        sortOrder = -1,
        invite = null,
        lastPush = null
    )

    @Test
    fun ssbSignMessage() {
        /*
        {
          "key": "%fH6ZETSgkMAvxbMO8aAz1h8rNLO4lKoWMTtmxZZag/A=.sha256",
          "value": {
            "previous": "%11J4JcYTzJy6a5Tlk9ZKxiCMQEupNuNs747Ktemo2d0=.sha256",
            "sequence": 3,
            "author": "@YpSbE5/7oWuf7k6zhU/wwbm28EffUggYEwVpDkOAdIg=.ed25519",
            "timestamp": 1673170497023,
            "hash": "sha256",
            "content": {
              "text": "NEWTEST 2",
              "type": "post"
            },
            "signature": "KlEVtD4E221mJibhXuZCQ15BrsnNNHruepucHqvYnJVvw8UJgl5sL1QPGMATnP7KlkzM3SirUf4/19DkT+4sDQ==.sig.ed25519"
          },
          "timestamp": 1673170497024
        }
        */
        val ssbMessageContent = SsbMessageContent("NEWTEST 2", "post")
        val ssbSignableMessage = SsbSignableMessage(
            previous = "%11J4JcYTzJy6a5Tlk9ZKxiCMQEupNuNs747Ktemo2d0=.sha256",
            sequence = 3,
            timestamp = 1673170497023,
            author = "@YpSbE5/7oWuf7k6zhU/wwbm28EffUggYEwVpDkOAdIg=.ed25519",
            hash = "sha256",
            content = ssbMessageContent
        )
        var sig = ssbSignableMessage.signMessage(feed)
        assertEquals(
            "KlEVtD4E221mJibhXuZCQ15BrsnNNHruepucHqvYnJVvw8UJgl5sL1QPGMATnP7KlkzM3SirUf4/19DkT+4sDQ==",
            sig.toBase64String()
        )
    }

    @Test
    fun ssbSignMessage2() {
        /*
{
  "previous": null,
  "author": "@9hVtm5AIfYXFp4o25sNYbkcalrOjovnuIagSkc6Gwgk=.ed25519",
  "sequence": 1,
  "timestamp": 1674143818478,
  "hash": "sha256",
  "content": {
    "text": "Hgjhf",
    "type": "post"
  }
}
        */
        val ssbMessageContent = SsbMessageContent("Hgjhf", "post")
        val ssbSignableMessage = SsbSignableMessage(
            previous = null,
            sequence = 1,
            timestamp = 1674143818478,
            author = "@9hVtm5AIfYXFp4o25sNYbkcalrOjovnuIagSkc6Gwgk=.ed25519",
            hash = "sha256",
            content = ssbMessageContent
        )
        var sig = ssbSignableMessage.signMessage(feed2)
        assertEquals(
            "frSesk4GvaLhlx22eViXs9KN5BjD6pU6Q90zVOPJ7NHaMAqcncl2zGHuOyaVoUQ/V2uYFak4HvGo5zuZ5TWICg==",
            sig.toBase64String()
        )
    }


    @Test
    fun ssbSignMessageFirst() {
        /*
        {
          "key": "%nSG0pVsSiEerGIWa2r/weVp0JKlvH7EFwbJIsag2T5E=.sha256",
          "value": {
            "previous": null,
            "sequence": 1,
            "author": "@YpSbE5/7oWuf7k6zhU/wwbm28EffUggYEwVpDkOAdIg=.ed25519",
            "timestamp": 1673170496679,
            "hash": "sha256",
            "content": {
              "text": "NEWTEST 0",
              "type": "post"
            },
            "signature": "EcTSm0ybUqibXSBTvqeji2dMr7OhVS30h+0wvqnR3tU7CaJBpk3xDtKvgvqi2/qmg+dwmeYFzwO/29yzglBJAA==.sig.ed25519"
          },
          "timestamp": 1673170496693
        }
         */
        val ssbMessageContent = SsbMessageContent("NEWTEST 0", "post")
        val ssbSignableMessage = SsbSignableMessage(
            previous = null,
            sequence = 1,
            timestamp = 1673170496679,
            author = "@YpSbE5/7oWuf7k6zhU/wwbm28EffUggYEwVpDkOAdIg=.ed25519",
            hash = "sha256",
            content = ssbMessageContent
        )
        var sig = ssbSignableMessage.signMessage(feed)
        val ssbSignedMessage = SsbSignedMessage(ssbSignableMessage, sig)
        assertEquals(
            "EcTSm0ybUqibXSBTvqeji2dMr7OhVS30h+0wvqnR3tU7CaJBpk3xDtKvgvqi2/qmg+dwmeYFzwO/29yzglBJAA==",
            sig.toBase64String()
        )
        val hash = ssbSignedMessage.makeHash()
        println(hash)
        assertEquals(
            "y6lKs9xDNxy55VLqucCBND/S5h4aZXnwb+RS2YAwlC4=",
            hash!!.bytes().toBase64String()
        )
    }

}
