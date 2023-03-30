package `in`.delog.ssb

import org.apache.tuweni.io.Base64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class MnemonicsTest {


    @Test
    fun privateToMnemonic() {
        val privKeyBytes =
            Base64.decode("8CcQUI27IE+Rjj7sZ4Q9njjqcB0vizqstNYGVux/ehJilJsTn/uha5/uTrOFT/DBubbwR99SCBgTBWkOQ4B0iA==")
        var memoText =
            "abstract panther amount steak jacket sight media model march panic season toilet illness dentist twist congress fabric rail fog hope project wild stadium bar".trim()
        val arr: List<String> = WordList(Locale.ENGLISH).words
        val dict = Dict(arr.toTypedArray())
        val p = secretKeyToMnemonic(privKeyBytes.toArray(), dict)
        assertEquals(24, p.size)
        assertEquals(p.joinToString(" ").trim(), memoText)
    }


    @Test
    fun mnemonicToPrivate() {
        val b64secret =
            "8CcQUI27IE+Rjj7sZ4Q9njjqcB0vizqstNYGVux/ehJilJsTn/uha5/uTrOFT/DBubbwR99SCBgTBWkOQ4B0iA=="
        var memoText =
            "abstract panther amount steak jacket sight media model march panic season toilet illness dentist twist congress fabric rail fog hope project wild stadium bar".trim()
        val arr: List<String> = WordList(Locale.ENGLISH).words
        val dict = Dict(arr.toTypedArray())
        val signature = mnemonicToSignature(memoText.split(" "), dict)
        assertEquals(
            b64secret,
            signature?.secretKey()?.bytes()?.toBase64String() ?: "no value returned",
        )
    }


}