package `in`.delog.ui.component

import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import `in`.delog.ui.theme.MyTheme
import `in`.delog.viewmodel.BlobItem

@Composable
fun BlobView(blobItem: BlobItem, action: (key: BlobItem) -> Unit, cta: @Composable() () -> Unit) {
    val mediaType = blobItem.type
    Box(modifier = Modifier
        .border(1.dp, MaterialTheme.colorScheme.background)
        .fillMaxWidth()
        ) {
        if (mediaType.startsWith("image") || mediaType.startsWith("video")) {
            var model: ImageRequest? = null
            if (mediaType.startsWith("image")) {
                model = ImageRequest.Builder(LocalContext.current)
                    .data(blobItem.uri)
                    .build()
            } else if (mediaType.startsWith("video")) {
                model = ImageRequest.Builder(LocalContext.current)
                    .data(blobItem.uri)
                    .videoFrameMillis(10000)
                    .decoderFactory { result, options, _ ->
                        VideoFrameDecoder(
                            result.source,
                            options
                        )
                    }.build()
            }
            AsyncImage(
                model = model,
                contentDescription = blobItem.uri.toString(),
                //contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setData(blobItem.uri)
            intent.setType(blobItem.type)
            val matches: List<ResolveInfo> =
                LocalContext.current.packageManager.queryIntentActivities(intent, 0)

            if (!matches.isEmpty()) {
                val icon: Drawable = matches.first().loadIcon(LocalContext.current.packageManager)
                Image(
                    modifier = Modifier
                        .size(96.dp)
                        .padding(16.dp)
                        .align(Alignment.Center),
                    painter = BitmapPainter(icon.toBitmap().asImageBitmap()),
                    contentDescription = blobItem.type
                )
            } else {
                Text(text = blobItem.type,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(8.dp).align(Alignment.TopStart))
                Icon(
                    tint= MaterialTheme.colorScheme.primary,
                    imageVector = Icons.Default.BrokenImage,
                    modifier = Modifier
                        .size(96.dp)
                        .align(Alignment.Center),
                    contentDescription = blobItem.type
                )
            }

        }
        Text(
            blobItem.key,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .width(128.dp)
                .align(Alignment.BottomCenter)
                .padding(8.dp),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        IconButton(
            modifier = Modifier
                .size(30.dp)
                .padding(end = 5.dp)
                .align(Alignment.TopEnd),
            onClick = { action(blobItem) },
        ) {
            cta()
        }
    }
}


@Preview
@Composable
fun previewBlobView() {
    val b1 = BlobItem(
        key = "&YsGsrC3iYbfU9ZS1qw0XTPGGLxxpapUreC/fo0xICNA=.sha256",
        size = 100,
        type = "application/pdf",
        uri = Uri.EMPTY
    )
    MyTheme(
        darkTheme = false,
        dynamicColor = false
    ) {
        Row {
            Column(modifier = Modifier.fillMaxWidth()) {
                BlobView(b1, {}) { CancelIcon() }
            }
        }
    }
}