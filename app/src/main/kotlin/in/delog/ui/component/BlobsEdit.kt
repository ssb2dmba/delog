package `in`.delog.ui.component

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.delog.ui.theme.MyTheme
import `in`.delog.viewmodel.BlobItem
import kotlinx.coroutines.launch
import java.io.File


@Composable
fun BlobsEdit(blobs: Array<BlobItem>, action: (key: BlobItem) -> Unit, actionIcon: @Composable () -> Unit) {
    if (blobs.isEmpty()) return
    val listState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val showBefore by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }
    val showAfter by remember {
        derivedStateOf {
            listState.canScrollForward
        }
    }
    Row (modifier = Modifier.fillMaxWidth()){
        if (showBefore) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .width(32.dp)
            ) {
                IconButton(
                    modifier = Modifier
                        .size(30.dp)
                        .padding(end = 5.dp),
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(index = 0)
                        }
                    },
                ) {
                    ArrowBackIcon()
                }
            }
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            if (blobs.size == 1) {
                BlobView(blobs[0], action, cta = actionIcon)
            } else {
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(if (blobs.size >= 6) 2 else 1),
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier.height(300.dp),
                    contentPadding = PaddingValues(all = 0.dp)
                ) {
                    items(blobs) { blobItem ->
                        //Text("BlobEdit: ${blobItem.key}")
                        BlobView(blobItem, action, cta = actionIcon)
                    }
                }
            }

        }

        if (showAfter) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .width(32.dp)
            ) {
                IconButton(
                    modifier = Modifier
                        .size(30.dp)
                        .padding(end = 5.dp),
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(blobs.size - 1)
                        }
                    },
                ) {
                    ArrowAfterIcon()
                }
            }
        }
    }
}




@Preview
@Composable
fun BlobEditPreview() {
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
    //blobs = blobs.plus(b2)
    MyTheme(
        darkTheme = false,
        dynamicColor = false
    ) {
        Row(modifier=Modifier.fillMaxWidth()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                BlobsEdit(
                    blobs,
                    action = { },
                    actionIcon = { CancelIcon() }
                )
            }
        }
    }
}