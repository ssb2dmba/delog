package `in`.delog.ui.scene

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.zachklipp.richtext.ui.printing.Printable
import com.zachklipp.richtext.ui.printing.rememberPrintableController
import `in`.delog.db.model.IdentAndAbout
import `in`.delog.ssb.BaseSsbService.Companion.TAG
import `in`.delog.ssb.Dict
import `in`.delog.ssb.WordList
import `in`.delog.ssb.secretKeyToMnemonic
import `in`.delog.viewmodel.IdentAndAboutViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.tuweni.io.Base64
import java.util.*


@Composable
fun rememberMnemonicText(pk: String): String? {
    var txt by remember(pk) {
        mutableStateOf<String?>(null)
    }
    LaunchedEffect(txt) {
        if (txt != null) return@LaunchedEffect
        launch(Dispatchers.IO) {
            val entropy: ByteArray =
                Base64.decode(pk).toArray()
            val arr: List<String> = WordList(Locale.ENGLISH).words
            val dict = Dict(arr.toTypedArray())
            val mnemonicCode = secretKeyToMnemonic(entropy, dict)
            val mnemonic = mnemonicCode.joinToString(" ")
            txt = mnemonic
        }
    }
    return txt
}


@Composable
fun rememberQrBitmapPainter(
    content: String,
    size: Dp = 300.dp,
    padding: Dp = 0.dp
): BitmapPainter {
    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx() }
    val paddingPx = with(density) { padding.roundToPx() }


    var bitmap by remember(content) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(bitmap) {
        if (bitmap != null) return@LaunchedEffect

        launch(Dispatchers.IO) {
            val qrCodeWriter = QRCodeWriter()

            val encodeHints = mutableMapOf<EncodeHintType, Any?>()
                .apply {
                    this[EncodeHintType.MARGIN] = paddingPx
                }

            val bitmapMatrix = try {
                qrCodeWriter.encode(
                    content, BarcodeFormat.QR_CODE,
                    sizePx, sizePx, encodeHints
                )
            } catch (ex: WriterException) {
                ex.message?.let { Log.e(TAG, it) }
                null
            }

            val matrixWidth = bitmapMatrix?.width ?: sizePx
            val matrixHeight = bitmapMatrix?.height ?: sizePx
            val newBitmap = Bitmap.createBitmap(
                bitmapMatrix?.width ?: sizePx,
                bitmapMatrix?.height ?: sizePx,
                Bitmap.Config.ARGB_8888,
            )

            for (x in 0 until matrixWidth) {
                for (y in 0 until matrixHeight) {
                    val shouldColorPixel = bitmapMatrix?.get(x, y) ?: false
                    val pixelColor = if (shouldColorPixel) Color.BLACK else Color.WHITE

                    newBitmap.setPixel(x, y, pixelColor)
                }
            }
            bitmap = newBitmap
        }
    }

    return remember(bitmap) {
        val currentBitmap = bitmap ?: Bitmap.createBitmap(
            sizePx, sizePx,
            Bitmap.Config.ARGB_8888,
        ).apply { eraseColor(Color.TRANSPARENT) }

        BitmapPainter(currentBitmap.asImageBitmap())
    }
}

@Composable
fun ExportMnemonicDialog(
    identAndAbout: IdentAndAbout,
    onDismissRequest: () -> Unit
) {
    val printController = rememberPrintableController()
    Dialog(
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        ),
        onDismissRequest = { onDismissRequest() }) {
        Card(
            Modifier
                .fillMaxSize()
                .padding(0.dp),
            colors = CardDefaults.cardColors(),
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Printable(printController) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = identAndAbout!!.about!!.name!!,
                            modifier = Modifier.padding(16.dp),
                        )
                        val pk = identAndAbout!!.ident.privateKey!!
                        //var atServer =""
                        val atServer = "@" +
                                    identAndAbout!!.ident.server +
                                    ":" +
                                    identAndAbout!!.ident.port

                        val configuration = LocalConfiguration.current

                        val screenWidth = configuration.screenWidthDp.dp
                        Image(
                            painter = rememberQrBitmapPainter(
                                content = if (identAndAbout!!.ident.server.isNotEmpty())
                                    (pk + atServer)
                                else
                                    pk,
                                size = screenWidth - 32.dp
                            ),
                            contentDescription = "QR Code"
                        )
                        if (identAndAbout!!.ident.server.isNotEmpty()) {
                            Text(
                                text = atServer,
                                modifier = Modifier.padding(32.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        rememberMnemonicText(pk = identAndAbout!!.ident.privateKey!!)?.let {
                            Text(
                                text = it,
                                modifier = Modifier.padding(32.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { onDismissRequest() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Dismiss")
                    }
                    TextButton(
                        onClick = {
                            printController.print(identAndAbout!!.about!!.name!!)
                        },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Print")
                    }
                }
            }
        }
    }
}
