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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Drafts
import androidx.compose.material.icons.rounded.DynamicFeed
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import `in`.delog.R
import `in`.delog.ui.LocalActiveFeed
import `in`.delog.ui.navigation.Scenes

@Composable
fun AppDrawer(
    itemClick: (String?) -> Unit
) {

    val itemsList = prepareNavigationDrawerItems()
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .clickable {
                itemClick(null)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 36.dp),

        ) {

        item {
            val feed = LocalActiveFeed.current
            if (feed != null) {
                // user's image

                AsyncImage(
                    model = "https://robohash.org/${feed.ident.publicKey}.png",
                    placeholder = rememberAsyncImagePainter("https://robohash.org/${feed.ident.publicKey}.png"),
                    contentDescription = feed.about?.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(size = 120.dp)
                        .clip(shape = CircleShape)
                        .background(MaterialTheme.colorScheme.outline),
                )
                // user's name
                Text(
                    modifier = Modifier
                        .padding(top = 12.dp),
                    text = if (feed.about!!.name != null)
                        feed.about!!.name!!
                    else
                        feed.ident.publicKey.subSequence(0, 5).toString() + "error",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        for (i in itemsList) {
            item {
                NavigationListItem(item = i) {
                    itemClick(i.route)
                }
            }
        }
    }

}

@Composable
private fun NavigationListItem(
    item: NavigationDrawerItem,
    itemClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                itemClick()
            }
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        // icon
        Box {

            Icon(
                modifier = Modifier
                    .padding(all = 2.dp)
                    .size(size = 28.dp),
                painter = item.image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        // label
        Text(
            modifier = Modifier.padding(start = 16.dp),
            text = item.label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun prepareNavigationDrawerItems(): List<NavigationDrawerItem> {
    val itemsList = arrayListOf<NavigationDrawerItem>()

    itemsList.add(
        NavigationDrawerItem(
            image = rememberVectorPainter(Icons.Rounded.DynamicFeed),
            label = stringResource(R.string.main_feed),
            route = Scenes.MainFeed.route
        )
    )

    itemsList.add(
        NavigationDrawerItem(
            image = rememberVectorPainter(Icons.Rounded.Drafts),
            label = stringResource(R.string.drafts),
            route = Scenes.DraftList.route
        )
    )

    itemsList.add(
        NavigationDrawerItem(
            image = rememberVectorPainter(Icons.Rounded.Fingerprint),
            label = stringResource(R.string.identifiers),
            route = Scenes.FeedList.route
        )
    )
    itemsList.add(
        NavigationDrawerItem(
            image = rememberVectorPainter(Icons.Rounded.People),
            label = stringResource(R.string.contacts),
            route = Scenes.ContactList.route
        )
    )
    return itemsList
}

data class NavigationDrawerItem(
    val image: Painter,
    val label: String,
    val route: String
)