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
package `in`.delog.ui.component

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import `in`.delog.db.model.About
import `in`.delog.db.model.Ident
import `in`.delog.db.model.IdentAndAbout
import `in`.delog.ui.scene.ExportPublickKeyDialog
import `in`.delog.ui.theme.MyTheme
import `in`.delog.ui.theme.keySmall

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IdentityBox(
    identAndAbout: IdentAndAbout,
    short: Boolean = false,
    onClick: ((IdentAndAbout) -> Unit)? = null,
) {

    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }

    if (showExportDialog) {
        ExportPublickKeyDialog(identAndAbout = identAndAbout, onDismissRequest = {
            showExportDialog = false
        })
        return
    }

    if (identAndAbout.about == null) {
        identAndAbout.about = About(identAndAbout.ident.publicKey)
    }
    Row(modifier = Modifier.padding(16.dp)) {
        // column image
        Column(modifier = Modifier.width(56.dp)) {
            AsyncImage(
                model = "https://robohash.org/${identAndAbout.about!!.about}.png",
                placeholder = rememberAsyncImagePainter("https://robohash.org/${identAndAbout.about!!.about}.png"),
                contentDescription = "Profile Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size = 48.dp)
                    .clip(shape = CircleShape)
                    .background(MaterialTheme.colorScheme.outline),
            )
        }
        // spacer
        Column(modifier = Modifier.width(8.dp)) {}
        // column text
        Column() {
            // private Key
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = identAndAbout.ident.publicKey,
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                clipboardManager.setText(buildAnnotatedString { append(identAndAbout.ident.publicKey) })
                                Toast
                                    .makeText(
                                        context,
                                        String.format("%s copied!", identAndAbout.ident.publicKey),
                                        Toast.LENGTH_LONG
                                    )
                                    .show()
                            },
                            onDoubleClick = {
                                showExportDialog = true
                            }
                        ),
                    style = keySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // alias + button
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = identAndAbout.getNetworkIdentifier(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (identAndAbout.ident.defaultIdent && onClick != null) {
                    FilledTonalIconButton(
                        onClick = {
                            onClick.invoke(identAndAbout)
                        }
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "",
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                }
            }

            // biography
            if (identAndAbout.about?.description != null && identAndAbout.about?.description != "") {
                Text(
                    text = identAndAbout.about!!.description!!,
                    style = MaterialTheme.typography.bodySmall,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = if (short) 2 else Int.MAX_VALUE,
                )
            }
        }
    }
}


@Preview
@Composable
fun IdentityCardPreview() {
    val identAndAbout = IdentAndAbout(
        ident = Ident(
            1,
            "YpSbE5/7oWuf7k6zhU/wwbm28EffUggYEwVpDkOAdIg=.ed25519",
            "localhost",
            1234,
            "priv",
            false,
            1,
            "",
            null
        ),
        about = About(
            about = "@YpSbE5/7oWuf7k6zhU/wwbm28EffUggYEwVpDkOAdIg=.ed25519",
            name = "Oreo Cookie",
            //description = "we made healthy  \uD83D\uDD25  drinks and we are rewilding community across #Britain with the @Orchad project. Find out more at https://www.voila.co.uk",
            image = "image",
            description = "",
            dirty = false
        )
    )
    MyTheme(
        darkTheme = false,
        dynamicColor = false
    ) {
        Column {
            IdentityBox(
                identAndAbout = identAndAbout,
                short = true,
            )
            ListSpacer()
        }
    }
}

