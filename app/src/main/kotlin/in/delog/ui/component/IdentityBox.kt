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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import `in`.delog.R
import `in`.delog.db.model.About
import `in`.delog.db.model.Ident
import `in`.delog.db.model.IdentAndAbout
import `in`.delog.ui.navigation.Scenes
import `in`.delog.ui.theme.MyTheme
import `in`.delog.ui.theme.keySmall
import java.net.URLEncoder
import java.nio.charset.Charset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityBox(
    about: About,
    short: Boolean = false,
    navController: NavController,
    mine: Boolean = false,
    following: Boolean = false,
    defaultIdent: Boolean = false
) {

    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

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
                text = about.about,
                modifier = Modifier.padding(start = 56.dp)
                    .clickable {
                    clipboardManager.setText(buildAnnotatedString { append(about.about) })
                    Toast
                        .makeText(
                            context,
                            String.format("%s copied!", about.about),
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
            modifier = Modifier.fillMaxWidth()
                .clickable {
                    var argUri = URLEncoder.encode(
                        about.about,
                        Charset
                            .defaultCharset()
                            .toString()
                    )
                    if (mine) {
                        navController.navigate("${Scenes.AboutEdit.route}/${argUri}")
                    } else {
                        navController.navigate("${Scenes.MainFeed.route}/${argUri}")
                    }
                }
        ) {
            AsyncImage(
                model = "https://robohash.org/${about.about}.png",
                placeholder = rememberAsyncImagePainter("https://robohash.org/${about.about}.png"),
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
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        text = about?.name ?: about.about,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 12.dp)
                    )
                    Spacer(modifier = Modifier.weight(0.2f))

                    if (defaultIdent) {
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
                }

                if (short) {
                    Text(
                        text = if (about?.description != null) about.description!! else "",
                        style = MaterialTheme.typography.bodySmall,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = if (short) 2 else Int.MAX_VALUE,
                    )
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                    ClickableTextField(
                        text = if (about?.description != null) about.description!! else "",
                        onClick = {})
                }

            }

        }
        if (!short) {

        }

    }

    ListSpacer()

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
            "Oreo Cookie",
            1,
            null
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
                about = identAndAbout.about!!,
                short = false,
                navController = navController
            )
        }
    }
}

