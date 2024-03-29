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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import `in`.delog.MainApplication
import `in`.delog.model.MessageViewData
import `in`.delog.model.serializeMessageContent
import `in`.delog.service.ssb.SsbService.Companion.format
import `in`.delog.ui.component.richtext.RichTextViewer
import `in`.delog.ui.navigation.Scenes
import `in`.delog.ui.theme.MyTheme
import `in`.delog.viewmodel.BlobItem
import java.io.File
import java.net.URLEncoder
import java.nio.charset.Charset
import java.sql.Timestamp
import java.util.Calendar
import java.util.Date


@Composable
fun MsgToolbar(
    navController: NavController,
    message: MessageViewData,
    onClickCallBack: () -> Unit,
    truncated: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
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
//        Column(
//            horizontalAlignment = Alignment.End,
//            modifier = Modifier.weight(0.25F)
//        ) {
//            // Repost
//            IconButton(
//                onClick = {
//                    repost(message.key, navController)
//                }
//            )
//            {
//                Icon(
//                    Icons.Filled.Autorenew,
//                    contentDescription = "",
//                    modifier = Modifier.size(ButtonDefaults.IconSize)
//                )// reply
//            }
//        }
//        Column(
//            horizontalAlignment = Alignment.End,
//            modifier = Modifier.weight(0.25F)
//        ) {
//            // Like
//            IconButton(
//                onClick = {
//                    vote(message.key, navController)
//                }
//            )
//            {
//                Icon(
//                    Icons.Filled.Favorite,
//                    contentDescription = "",
//                    modifier = Modifier.size(ButtonDefaults.IconSize)
//                )// reply
//            }
//        }
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
    messageViewData: MessageViewData,
    showToolbar: Boolean = false,
    hasDivider: Boolean = false,
    onClickCallBack: () -> Unit,
    truncate: Boolean = false
) {

    val messageText: String = messageViewData.serializeMessageContent(format).text ?: ""

    val SHORT_TEXT_LENGTH = 240
    val SHORTEN_AFTER_LINES = 6

    val whereToCut = remember(messageText) {
        // Cuts the text in the first space or new line after SHORT_TEXT_LENGTH characters
        val firstSpaceAfterCut =
            messageText.indexOf(' ', SHORT_TEXT_LENGTH).let { if (it < 0) messageText.length else it }
        val firstNewLineAfterCut =
            messageText.indexOf('\n', SHORT_TEXT_LENGTH).let { if (it < 0) messageText.length else it }

        // or after SHORTEN_AFTER_LINES lines
        val numberOfLines = messageText.count { it == '\n' }

        var charactersInLines = minOf(firstSpaceAfterCut, firstNewLineAfterCut)

        if (numberOfLines > SHORTEN_AFTER_LINES) {
            val shortContent = messageText.lines().take(SHORTEN_AFTER_LINES)
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

    val text: String by remember(messageText) {
        derivedStateOf {
            if (!truncate) {
                messageText
            } else {
                truncated = whereToCut < messageText.length
                messageText.take(whereToCut)
            }
        }
    }
    /**************/
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
            modifier = Modifier.threadIndicator(messageViewData.level.toInt())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(64.dp)
            ) {
                if( messageViewData.authorImage.isNullOrEmpty()) {
                    ProfileImage(
                        identAndAboutWithBlob = null,
                        authorImage = null,
                        pk=messageViewData.author
                    )
                } else {
                    ProfileImage(
                        identAndAboutWithBlob = null,
                        authorImage = messageViewData.authorImage
                    )
                }
            }
            // spacer
            Column(modifier = Modifier.width(8.dp)) {}
            // Message head
            Column {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Row {
                        Text(
                            modifier = Modifier
                                .weight(0.9f),
                            //.padding(2.dp),
                            text = messageViewData.authorName ?: messageViewData.author,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleSmall,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                        val strTimeAgo: String = DateUtils.getRelativeTimeSpanString(
                            Date(Timestamp(messageViewData.timestamp).time).time,
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


            }
        }
        if (messageViewData.blobs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .threadIndicator(messageViewData.level.toInt())
            ) {
                Row(modifier=Modifier.padding(start = (messageViewData.level * 40).toInt().dp)) {
                    BlobsEdit(
                        blobs = messageViewData.blobs,
                        action = { openView(it) }, actionIcon = { PageViewIcon() })
                }

            }
        }
        if (showToolbar) {
            Row(modifier=Modifier.threadIndicator(messageViewData.level.toInt())) {
                MsgToolbar(
                    navController = navController,
                    message = messageViewData,
                    truncated = truncated,
                    onClickCallBack = onClickCallBack
                )
            }
        }
    }
}



fun openView( it: BlobItem) {
    val context = MainApplication.applicationContext()
    val type = it.type
    try {
        val intent = Intent()
        intent.setAction(Intent.ACTION_VIEW)

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            val contentUri =
                FileProvider.getUriForFile(context, "in.delog.provider", File(it.uri.path!!))
            intent.setDataAndType(contentUri, type)

        context.startActivity(intent)
    } catch (anfe: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "No activity found to open this attachment.",
            Toast.LENGTH_LONG
        ).show()
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
    var blobs = arrayOf<BlobItem>()
    val b1 = BlobItem(
        key = "&YsGsrC3iYbfU9ZS1qw0XTPGGLxxpapUreC/fo0xICNA=.sha256",
        size = 100,
        type = "application/pdf",
        uri = Uri.EMPTY
    )
    val b2 = BlobItem(
        key = "key2",
        size = 100,
        type = "image/png",
        uri = Uri.fromFile(File("https://picsum.photos/300/300"))
    )
    blobs = blobs.plus(b1)
    blobs = blobs.plus(b2)
    val txt =  "#title \n we made healthy  \uD83D\uDD25  Wikipedia[note 3] is a #multilingual free online encyclopedia written and maintained by a community of volunteers, known as @Wikipedians, through open collaboration and using a wiki-based editing system called MediaWiki. Wikipedia is the largest and most-read reference work in history.[3] It is consistently one of the 10 most popular websites ranked by Similarweb and formerly Alexa; as of 2022, Wikipedia was ranked the 5th most popular site in the world.[4] It is hosted by the Wikimedia Foundation, an American non-profit organization funded mainly through donations.[5]"

    val messageViewData = MessageViewData(
        key = "@1234",
        timestamp = 1234, author = "",
        contentAsText = "{ \"type\": \"post\", \"text\": \"${txt}\", \"mentions\": [ { \"name\": \"Wikipedians\", \"link\": \"@Wikipedians\" } ] }",
        authorImage = "",
        authorName = "Cookie Jar",
        blobs = blobs
    )

    val messageViewData2 = messageViewData.copy(
        key="@1235",
        contentAsText = "{ \"type\": \"post\", \"text\":  \"plop\" }",
        level = 1
    )

    MyTheme(
        darkTheme = false,
        dynamicColor = false
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier=Modifier.padding(8.dp),
                    content = {
                        item {
                            MessageItem(
                                navController = navController,
                                messageViewData = messageViewData,
                                showToolbar = true,
                                onClickCallBack = { },
                                truncate = true
                            )
                        }
                        item {
                            MessageItem(
                                navController = navController,
                                messageViewData = messageViewData2,
                                showToolbar = true,
                                onClickCallBack = { },
                                truncate = true
                            )
                        }
                    }
                )

            }
        }
    }
}

fun Modifier.threadIndicator(level: Int) = composed(
    factory = {
        if (level == 0) return@composed this
        val density = LocalDensity.current
        val strokeWidth = 1.dp
        val color = MaterialTheme.colorScheme.onSurfaceVariant
        val strokeWidthPx = density.run { strokeWidth.toPx() }

        Modifier.drawBehind {
            val height = size.height - strokeWidthPx / 2
            for (i in 1..level) {
                drawLine(
                    color = color,
                    start = Offset(x = 64f * (level), y = 0f),
                    end = Offset(x = 64f * (level), y = height),
                    strokeWidth = strokeWidthPx
                )
            }
        }
    }
)