package org.dlog.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import org.dlog.db.model.About
import org.dlog.db.model.Ident
import org.dlog.db.model.IdentAndAbout
import org.dlog.ui.navigation.Scenes
import org.dlog.ui.theme.MyTheme
import org.dlog.ui.theme.keySmall

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityCard(
    identAndAbout: IdentAndAbout,
    isMine: Boolean = true,
    canExpand: Boolean = false,
    navController: NavController
) {
    val ident = identAndAbout.ident
    val about = identAndAbout.about

    Box(
        modifier = Modifier
            .padding(6.dp)
            .clickable {
                if (isMine) {
                    navController.navigate("${Scenes.AboutEdit.route}/${ident.oid}")
                }
            }
    ) {
        // private Key
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = ident.publicKey,
                modifier = Modifier.padding(start=54.dp),
                style = keySmall,
            )
        }
        // alias
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // TODO about.image
            AsyncImage(
                model = "https://robohash.org/${ident.publicKey}.png",
                placeholder = rememberAsyncImagePainter("https://robohash.org/${ident.publicKey}.png"),
                contentDescription = "Profile Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size = 48.dp)
                    .clip(shape = CircleShape)
                    .background(MaterialTheme.colorScheme.outline),

            )
            Column(Modifier.fillMaxWidth().padding(start=6.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        text = about?.name ?: ident.alias,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f).padding(top=12.dp)
                    )
                    Spacer(modifier = Modifier.weight(0.2f))
                    if (true) {
//                if (ident.defaultIdent) {
                        Badge(
                            modifier = Modifier.padding(top=18.dp) // same padding as alias
                        ) {
                            Text(
                                "default",
                                modifier = Modifier.padding(2.dp).semantics {
                                    contentDescription = "default"
                                }

                            )
                        }
                    }
                }
                Text(
                    text = if (about?.description != null) about.description!! else "",
                    style = MaterialTheme.typography.bodySmall,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                )

            }
        }

//        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgeDefault() {

}


@Preview
@Composable
fun IdentityCardPreview() {
    val navController = rememberNavController()
    var identAndAbout = IdentAndAbout(
        ident = Ident(
            1,
            "YpSbE5/7oWuf7k6zhU/wwbm28EffUggYEwVpDkOAdIg=.ed25519",
            "mega.dlog.org",
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
            IdentityCard(
                identAndAbout = identAndAbout,
                isMine = true,
                canExpand = false,
                navController = navController
            )
        }
    }
}

