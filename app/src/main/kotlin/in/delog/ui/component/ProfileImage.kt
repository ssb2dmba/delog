package `in`.delog.ui.component

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import `in`.delog.db.model.IdentAndAboutWithBlob


@Composable
fun ProfileImage(identAndAboutWithBlob: IdentAndAboutWithBlob?, authorImage: String?=null) {
    var model: Any

    if (identAndAboutWithBlob?.profileImage != null && identAndAboutWithBlob?.profileImage != Uri.EMPTY) {
        model = ImageRequest.Builder(LocalContext.current)
            .data(identAndAboutWithBlob.profileImage)
            .build()
    } else if (authorImage != null) {
        model = ImageRequest.Builder(LocalContext.current)
            .data(authorImage)
            .build()
    } else {
        model = "https://robohash.org/${identAndAboutWithBlob?.about}.png"
    }

    AsyncImage(
        model = model,
        placeholder = rememberAsyncImagePainter(model),
        contentDescription = identAndAboutWithBlob?.about?.name ?: "",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .padding(top = 12.dp, end = 8.dp)
            .size(size = 48.dp)
            .clip(shape = CircleShape)
            .background(MaterialTheme.colorScheme.outline),
    )

}