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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import `in`.delog.db.model.About
import `in`.delog.db.model.Ident
import `in`.delog.db.model.IdentAndAbout
import `in`.delog.ui.theme.MyTheme
import `in`.delog.ui.theme.keySmall

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun IdentityBox(
    identAndAbout: IdentAndAbout,
    short: Boolean = false,
    onClick: ((IdentAndAbout) -> Unit)? = null,
    onLongClick: ((IdentAndAbout) -> Unit)? = null,
    onDblClick: ((IdentAndAbout) -> Unit)? = null
) {

    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    if (identAndAbout.about == null) {
        identAndAbout.about = About(identAndAbout.ident.publicKey)
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
        // private Key
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = identAndAbout.ident.publicKey,
                modifier = Modifier
                    .padding(start = 56.dp)
                    .clickable {
                        clipboardManager.setText(buildAnnotatedString { append(identAndAbout.ident.publicKey) })
                        Toast
                            .makeText(
                                context,
                                String.format("%s copied!", identAndAbout.ident.publicKey),
                                Toast.LENGTH_LONG
                            )
                            .show()
                    },
                style = keySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // alias
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onLongClick = {
                        onLongClick?.let { it(identAndAbout) }
                    },
                    onClick = {
                        onClick?.let { it(identAndAbout) }
                    },
                    onDoubleClick = {
                        onDblClick?.let { it(identAndAbout) }
                    }
                )
        ) {
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
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = identAndAbout.about?.name ?: identAndAbout.ident.publicKey,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 16.dp)
                    )
                    Spacer(modifier = Modifier.weight(0.2f))

                    if (identAndAbout.ident.defaultIdent) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(top = 18.dp)
                        ) {
                            Text(
                                "default",
                                modifier = Modifier
                                    .semantics {
                                        contentDescription = "default"
                                    }

                            )
                        }
                    }
                    FilledTonalIconButton(
                        modifier = Modifier.padding(16.dp),
                        onClick = {
                            onDblClick?.invoke(identAndAbout)
                        }
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "",
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                }

                if (short && identAndAbout.about?.description != null)  {
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
}


@Preview
@Composable
fun IdentityCardPreview() {
    val navController = rememberNavController()
    var identAndAbout = IdentAndAbout(
        ident = Ident(
            1,
            "YpSbE5/7oWuf7k6zhU/wwbm28EffUggYEwVpDkOAdIg=.ed25519",
            "localhost",
            1234,
            "priv",
            false,
            1,
            ""
        ),
        about = About(
            about = "@YpSbE5/7oWuf7k6zhU/wwbm28EffUggYEwVpDkOAdIg=.ed25519",
            name = "Oreo Cookie",
            description = "we made healthy  \uD83D\uDD25  drinks and we are rewilding community across #Britain with the @Orchad project. Find out more at https://www.voila.co.uk",
            image = "image",
            dirty = false
        )
    )
    MyTheme(
        darkTheme = true,
        dynamicColor = false
    ) {
        Column() {
            IdentityBox(
                identAndAbout = identAndAbout,
                short = false,
            )
        }
    }
}

