package `in`.delog.ui.scene

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zachklipp.richtext.ui.printing.Printable
import com.zachklipp.richtext.ui.printing.rememberPrintableController
import `in`.delog.db.model.IdentAndAbout


@Composable
fun ExportPublickKeyDialog(
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
                            text = identAndAbout.about!!.name!!,
                            modifier = Modifier.padding(16.dp),
                        )
                        val pubKey = identAndAbout.ident.publicKey
                        val atServer = "@" +
                                identAndAbout.ident.server +
                                ":" +
                                identAndAbout.ident.port
                        val content = if (identAndAbout.ident.server.isNotEmpty())
                            (pubKey + atServer)
                        else
                            pubKey
                        val configuration = LocalConfiguration.current
                        val screenWidth = configuration.screenWidthDp.dp
                        Image(
                            painter = rememberQrBitmapPainter(
                                content = content,
                                size = screenWidth - 32.dp
                            ),
                            contentDescription = "QR Code"
                        )
                        if (identAndAbout.ident.server.isNotEmpty()) {
                            Text(
                                text = content,
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
                    Button(
                        onClick = { onDismissRequest() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Dismiss")
                    }
                    Button(
                        onClick = {
                            printController.print(identAndAbout.about!!.name!!)
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


