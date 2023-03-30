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

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import dev.jeziellago.compose.markdowntext.MarkdownText
import `in`.delog.R
import `in`.delog.ssb.BaseSsbService.Companion.format
import `in`.delog.ui.navigation.Scenes
import `in`.delog.ui.theme.MyTheme
import java.net.URLEncoder
import java.nio.charset.Charset
import java.sql.Timestamp
import java.util.*


@Composable
fun MessageItem(
    navController: NavController,
    message: MessageViewData,
    showToolbar: Boolean = false,
    onClickCallBack: () -> Unit,
) {

    var expand by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier
            .wrapContentHeight()
            .padding(vertical = 2.dp, horizontal = 4.dp)
            .clickable {
                onClickCallBack()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(16.dp)
                    .clickable() {
                        navController.popBackStack();
                    }
            ) {
                if (showToolbar) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = "",
                        modifier = Modifier
                            .size(ButtonDefaults.IconSize)
                            .padding(4.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(2.dp)) {

                // Message head
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 6.dp, end = 6.dp)
                        .clickable {
                            onClickCallBack();
                        }
                ) {
                    Text(
                        modifier = Modifier
                            .weight(0.9f),
                        //.padding(2.dp),
                        text = message.authorName ?: message.author,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleSmall,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )

                    val strTimeAgo: String = DateUtils.getRelativeTimeSpanString(
                        Date(Timestamp(message.timestamp).time).getTime(),
                        Calendar.getInstance().getTimeInMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    ).toString()

                    Text(
                        text = strTimeAgo,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                    if (expand) {
                        IconButton(onClick = { expand = false }) {
                            Icon(
                                Icons.Default.ExpandLess,
                                contentDescription = "",
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                        }
                    } else {
                        IconButton(onClick = { expand = true }) {
                            Icon(
                                Icons.Default.ExpandMore,
                                contentDescription = "",
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                        }
                    }
                }

                // Message content
                Row(
                    modifier = Modifier
                        .padding(end = 6.dp, top = 12.dp)
                        .fillMaxWidth()
                        .clickable {
                            onClickCallBack();
                        }
                ) {
                    if (expand) {
                        MarkdownText(
                            onClick = onClickCallBack,
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            markdown = message.content(format).text.toString(),
                            maxLines = Int.MAX_VALUE,
                            textAlign = TextAlign.Companion.Justify
                        )
                    } else {
                        MarkdownText(
                            onClick = onClickCallBack,
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            markdown = message.content(format).text.toString(),
                            maxLines = 4,
                            textAlign = TextAlign.Companion.Justify
                        )
                    }
                }
            }
        }

        val currentRoute = navController.currentBackStackEntry?.arguments?.getString("id")
        if (currentRoute?.contains(message.key) == true) {
            expand = true
        }
        if (expand) {
            // toolbar
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 18.dp)
                    .fillMaxWidth()
            ) {
                // favorite
                FilledTonalButton(
                    onClick = {}
                )
                {
                    Icon(
                        Icons.Filled.FavoriteBorder,
                        contentDescription = "",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text("react")
                }

                val hasReply = true // todo stub
                if (hasReply && currentRoute?.contains(message.key) != true) {
                    OutlinedButton(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp, end = 12.dp),
                        onClick = {
                            var argUri =
                                URLEncoder.encode(message.key, Charset.defaultCharset().toString())
                            navController.navigate("${Scenes.MainFeed.route}/${argUri}")
                        },
                    )
                    {
                        Text(
                            style = MaterialTheme.typography.labelSmall,
                            text = stringResource(R.string.replies)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                // reply
                FilledTonalButton(
                    onClick = {},
                )
                {
                    Icon(
                        Icons.Filled.Reply,
                        contentDescription = "",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Text(text = stringResource(R.string.reply))
                }


            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp)
        ) {
            // favorite

        }


    }


}


@Preview
@Composable
fun MessageItemPreview() {
    val navController = rememberNavController()
    var messageViewData = MessageViewData(
        key = "@1234",
        timestamp = 1234, author = "",
        contentAsText = "#title \n we made healthy  \uD83D\uDD25  Wikipedia[note 3] is a #multilingual free online encyclopedia written and maintained by a community of volunteers, known as @Wikipedians, through open collaboration and using a wiki-based editing system called MediaWiki. Wikipedia is the largest and most-read reference work in history.[3] It is consistently one of the 10 most popular websites ranked by Similarweb and formerly Alexa; as of 2022, Wikipedia was ranked the 5th most popular site in the world.[4] It is hosted by the Wikimedia Foundation, an American non-profit organization funded mainly through donations.[5]",
        authorImage = "",
        authorName = "Cookie Jar"
    )
    MyTheme(
        darkTheme = true,
        dynamicColor = false
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column() {
                LazyVerticalGrid(columns = GridCells.Fixed(1)) {
                    item {
                        MessageItem(
                            message = messageViewData,
                            navController = navController,
                            showToolbar = false,
                            onClickCallBack = { }
                        )
                    }
                }
            }
        }
    }
}