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
import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import `in`.delog.ssb.BaseSsbService.Companion.format
import `in`.delog.ui.component.preview.url.UrlPreview
import `in`.delog.ui.component.richtext.RichTextViewer

import `in`.delog.ui.navigation.Scenes
import `in`.delog.ui.theme.MyTheme
import java.net.URLEncoder
import java.nio.charset.Charset
import java.sql.Timestamp
import java.util.*


@Composable
fun msgToolbar(
    navController: NavController,
    message: MessageViewData,
    onClickCallBack: () -> Unit,
    truncated: Boolean,
    ) {
    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.weight(0.25F)
        ) {
            if (truncated) {
                IconButton(
                    onClick = {
                        onClickCallBack.invoke()
                    }
                )
                {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "show more",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )// reply
                }
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.weight(0.25F)
        ) {
            // Repost
            IconButton(
                onClick = {
                    repost(message.key, navController)
                }
            )
            {
                Icon(
                    Icons.Filled.Autorenew,
                    contentDescription = "",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )// reply
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.weight(0.25F)
        ) {
            // Like
            IconButton(
                onClick = {
                    vote(message.key, navController)
                }
            )
            {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )// reply
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.weight(0.25F)
        ) {
            // Reply
            IconButton(
                onClick = {
                    reply(message.key, navController)
                },
            )
            {
                Icon(
                    Icons.Filled.Reply,
                    contentDescription = "",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
            }
        }
    }
}

@Composable
fun MessageItem(
    navController: NavController,
    message: MessageViewData,
    showToolbar: Boolean = false,
    hasDivider: Boolean = false,
    onClickCallBack: () -> Unit,
    truncate: Boolean = false
) {

    fun firstUrl(search: String): String? {
        val matcher = Patterns.WEB_URL.matcher(search)
        if (matcher.find()) {
            return matcher.group()
        }
        return null
    }

    val content: String by remember {
        mutableStateOf(message.content(format).text.toString())
    }

    val SHORT_TEXT_LENGTH = 240
    val SHORTEN_AFTER_LINES = 6

    val whereToCut = remember(content) {
        // Cuts the text in the first space or new line after SHORT_TEXT_LENGTH characters
        val firstSpaceAfterCut = content.indexOf(' ', SHORT_TEXT_LENGTH).let { if (it < 0) content.length else it }
        val firstNewLineAfterCut = content.indexOf('\n', SHORT_TEXT_LENGTH).let { if (it < 0) content.length else it }

        // or after SHORTEN_AFTER_LINES lines
        val numberOfLines = content.count { it == '\n' }

        var charactersInLines = minOf(firstSpaceAfterCut, firstNewLineAfterCut)

        if (numberOfLines > SHORTEN_AFTER_LINES) {
            val shortContent = content.lines().take(SHORTEN_AFTER_LINES)
            charactersInLines = 0
            for (line in shortContent) {
                // +1 because new line character is omitted from .lines
                charactersInLines += (line.length + 1)
            }
        }

        minOf(firstSpaceAfterCut, firstNewLineAfterCut, charactersInLines)
    }


    var truncated by remember {
        mutableStateOf(false)
    }


    val text:String by remember(content) {
        derivedStateOf {
            if (!truncate) {
                content
            } else {
                truncated = whereToCut < content.length
                content.take(whereToCut)
            }
        }
    }





    Card(
        colors = CardDefaults.cardColors(),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier
            .wrapContentHeight()
            .clickable {
                onClickCallBack()
            }
    ) {
        Row(
            modifier = Modifier
                //.height(IntrinsicSize.Max)
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(44.dp)
            ) {
                if (hasDivider) {
                    Divider(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxHeight()
                            .padding(top = 48.dp)
                            .width(width = 1.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                AsyncImage(
                    model = "https://robohash.org/${message.author}.png",
                    placeholder = rememberAsyncImagePainter("https://robohash.org/${message.author}.png"),
                    contentDescription = "Profile Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .size(size = 36.dp)
                        .clip(shape = CircleShape)
                        .background(MaterialTheme.colorScheme.outline),
                )

            }
            // spacer
            Column(modifier = Modifier.width(8.dp)) {}
            // Message head
            Column {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {

                    Row {
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
                            Date(Timestamp(message.timestamp).time).time,
                            Calendar.getInstance().timeInMillis,
                            DateUtils.MINUTE_IN_MILLIS
                        ).toString()
                        Text(
                            text = strTimeAgo,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }

                }
                // row message
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                ) {
                    val maxLines = if (truncate) 6 else Int.MAX_VALUE
                    RichTextViewer(text, { onClickCallBack.invoke() }, maxLines)
                }
//                // row preview
//                if (truncated || text.length <= shortLength) {
//                    if (url != null) {
//                        Row(modifier = Modifier
//                            .padding(16.dp)
//                            .padding(bottom = 0.dp)
//                            .height(200.dp)) {
//                            UrlPreview(url = url, urlText = url)
//                        }
//                    }
//                }
                //Spacer(modifier = Modifier.height(8.dp))
                // row toolbar
                if (showToolbar) {
                    msgToolbar(navController = navController, message = message, truncated=truncated, onClickCallBack = onClickCallBack)
                }
            }
        }
    }
}

fun makeArgUri(key: String): Any {
    return URLEncoder.encode(key, Charset.defaultCharset().toString())
}

fun reply(key: String, navController: NavController) {
    val argUri = makeArgUri(key)
    navController.navigate("${Scenes.DraftNew.route}/reply/${argUri}")
}

fun vote(key: String, navController: NavController) {
    val argUri = makeArgUri(key)
    navController.navigate("${Scenes.DraftNew.route}/vote/${argUri}")
}

fun repost(key: String, navController: NavController) {
    val argUri = makeArgUri(key)
    navController.navigate("${Scenes.DraftNew.route}/repost/${argUri}")
}

@Preview
@Composable
fun MessageItemPreview() {
    val navController = rememberNavController()
    val messageViewData = MessageViewData(
        key = "@1234",
        timestamp = 1234, author = "",
        contentAsText = "#title \n we made healthy  \uD83D\uDD25  Wikipedia[note 3] is a #multilingual free online encyclopedia written and maintained by a community of volunteers, known as @Wikipedians, through open collaboration and using a wiki-based editing system called MediaWiki. Wikipedia is the largest and most-read reference work in history.[3] It is consistently one of the 10 most popular websites ranked by Similarweb and formerly Alexa; as of 2022, Wikipedia was ranked the 5th most popular site in the world.[4] It is hosted by the Wikimedia Foundation, an American non-profit organization funded mainly through donations.[5]",
        authorImage = "",
        authorName = "Cookie Jar"
    )
    MyTheme(
        darkTheme = false,
        dynamicColor = false
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                LazyVerticalGrid(columns = GridCells.Fixed(1)) {
                    item {
                        MessageItem(
                            navController = navController,
                            message = messageViewData,
                            showToolbar = true,
                            onClickCallBack = { },
                            truncate = true
                        )
                    }
                }
            }
        }
    }
}