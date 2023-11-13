package `in`.delog.ui.component.preview.images

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import `in`.delog.R
import `in`.delog.ui.component.richtext.ClickableUrl
import `in`.delog.ui.component.LoadingAnimation
import `in`.delog.ui.component.preview.videos.VideoView
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import java.io.File

val imageExtensions = listOf("png", "jpg", "gif", "bmp", "jpeg", "webp", "svg")
val videoExtensions = listOf("mp4", "avi", "wmv", "mpg", "amv", "webm", "mov", "mp3", "m3u8")
@Immutable
abstract class ZoomableContent(
    val description: String? = null,
    val dim: String? = null
)

@Immutable
abstract class ZoomableUrlContent(
    val url: String,
    description: String? = null,
    val hash: String? = null,
    dim: String? = null,
    val uri: String? = null
) : ZoomableContent(description, dim)

@Immutable
class ZoomableUrlImage(
    url: String,
    description: String? = null,
    hash: String? = null,
    val blurhash: String? = null,
    dim: String? = null,
    uri: String? = null
) : ZoomableUrlContent(url, description, hash, dim, uri)

@Immutable
class ZoomableUrlVideo(
    url: String,
    description: String? = null,
    hash: String? = null,
    dim: String? = null,
    uri: String? = null,
    val artworkUri: String? = null,
    val authorName: String? = null
) : ZoomableUrlContent(url, description, hash, dim, uri)

@Immutable
abstract class ZoomablePreloadedContent(
    val localFile: File?,
    description: String? = null,
    val mimeType: String? = null,
    val isVerified: Boolean? = null,
    dim: String? = null,
    val uri: String
) : ZoomableContent(description, dim)

@Immutable
class ZoomableLocalImage(
    localFile: File?,
    mimeType: String? = null,
    description: String? = null,
    val blurhash: String? = null,
    dim: String? = null,
    isVerified: Boolean? = null,
    uri: String
) : ZoomablePreloadedContent(localFile, description, mimeType, isVerified, dim, uri)

@Immutable
class ZoomableLocalVideo(
    localFile: File?,
    mimeType: String? = null,
    description: String? = null,
    dim: String? = null,
    isVerified: Boolean? = null,
    uri: String,
    val artworkUri: String? = null,
    val authorName: String? = null
) : ZoomablePreloadedContent(localFile, description, mimeType, isVerified, dim, uri)


fun figureOutMimeType(fullUrl: String): ZoomableContent {
    val removedParamsFromUrl = fullUrl.split("?")[0].lowercase()
    val isImage = imageExtensions.any { removedParamsFromUrl.endsWith(it) }
    val isVideo = videoExtensions.any { removedParamsFromUrl.endsWith(it) }

    return if (isImage) {
        ZoomableUrlImage(fullUrl)
    } else if (isVideo) {
        ZoomableUrlVideo(fullUrl)
    } else {
        ZoomableUrlImage(fullUrl)
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ZoomableContentView(
    content: ZoomableContent,
    images: ImmutableList<ZoomableContent> = listOf(content).toImmutableList(),
    roundedCorner: Boolean,
) {
    // store the dialog open or close state
    var dialogOpen by remember {
        mutableStateOf(false)
    }

    // store the dialog open or close state
    val shareOpen = remember {
        mutableStateOf(false)
    }

    var mainImageModifier = if (roundedCorner) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth()
    }

    if (content is ZoomableUrlContent) {
        mainImageModifier = mainImageModifier.combinedClickable(
            onClick = { dialogOpen = true },
            onLongClick = { shareOpen.value = true }
        )
    } else if (content is ZoomablePreloadedContent) {
        mainImageModifier = mainImageModifier.combinedClickable(
            onClick = { dialogOpen = true },
            onLongClick = { shareOpen.value = true }
        )
    } else {
        mainImageModifier = mainImageModifier.clickable {
            dialogOpen = true
        }
    }

    when (content) {
        is ZoomableUrlImage -> UrlImageView(content, mainImageModifier)
        is ZoomableUrlVideo -> VideoView(
            videoUri = content.url,
            title = content.description,
            artworkUri = content.artworkUri,
            authorName = content.authorName,
            roundedCorner = roundedCorner,
            nostrUriCallback = content.uri,
            onDialog = { dialogOpen = true },
        )

        is ZoomableLocalImage -> LocalImageView(content, mainImageModifier)
        is ZoomableLocalVideo ->
            content.localFile?.let {
                VideoView(
                    videoUri = it.toUri().toString(),
                    title = content.description,
                    artworkUri = content.artworkUri,
                    authorName = content.authorName,
                    roundedCorner = roundedCorner,
                    nostrUriCallback = content.uri,
                    onDialog = { dialogOpen = true },
                )
            }
    }

    if (dialogOpen) {
        //ZoomableImageDialog(content, images, onDismiss = { dialogOpen = false })
    }
}

@Composable
private fun LocalImageView(
    content: ZoomableLocalImage,
    mainImageModifier: Modifier,
    topPaddingForControllers: Dp = Dp.Unspecified,
    alwayShowImage: Boolean = false
) {
    if (content.localFile != null && content.localFile.exists()) {
        BoxWithConstraints(contentAlignment = Alignment.Center) {
            val showImage = remember {
                mutableStateOf(true)
            }

            val myModifier = remember {
                mainImageModifier
                    .widthIn(max = maxWidth)
                    .heightIn(max = maxHeight)
                    .run {
                        aspectRatio(content.dim)?.let { ratio ->
                            this.aspectRatio(ratio, false)
                        } ?: this
                    }
            }

            val contentScale = remember {
                if (maxHeight.isFinite) ContentScale.Fit else ContentScale.FillWidth
            }

            val verifierModifier = if (topPaddingForControllers.isSpecified) {
                Modifier
                    .padding(top = topPaddingForControllers)
                    .align(Alignment.TopEnd)
            } else {
                Modifier.align(Alignment.TopEnd)
            }

            val painterState = remember {
                mutableStateOf<AsyncImagePainter.State?>(null)
            }

            if (showImage.value) {
                AsyncImage(
                    model = content.localFile,
                    contentDescription = content.description,
                    contentScale = contentScale,
                    modifier = myModifier,
                    onState = {
                        painterState.value = it
                    }
                )
            }

            AddedImageFeatures(
                painterState,
                content,
                contentScale,
                myModifier,
                verifierModifier,
                showImage
            )
        }
    } else {
        Text("-")
    }
}

@Composable
private fun UrlImageView(
    content: ZoomableUrlImage,
    mainImageModifier: Modifier,
    topPaddingForControllers: Dp = Dp.Unspecified,
    alwayShowImage: Boolean = false
) {
    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val showImage = remember {
            mutableStateOf<Boolean>(
                true
            )
        }

        val myModifier = remember {
            mainImageModifier
                .widthIn(max = maxWidth)
                .heightIn(max = maxHeight)
            /* Is this necessary? It makes images bleed into other pages
            .run {
                aspectRatio(content.dim)?.let { ratio ->
                    this.aspectRatio(ratio, false)
                } ?: this
            }
            */
        }

        val contentScale = remember {
            if (maxHeight.isFinite) ContentScale.Fit else ContentScale.FillWidth
        }

        val verifierModifier = if (topPaddingForControllers.isSpecified) {
            Modifier
                .padding(top = topPaddingForControllers)
                .align(Alignment.TopEnd)
        } else {
            Modifier.align(Alignment.TopEnd)
        }

        val painterState = remember {
            mutableStateOf<AsyncImagePainter.State?>(null)
        }

        if (showImage.value) {
            AsyncImage(
                model = content.url,
                contentDescription = content.description,
                contentScale = contentScale,
                modifier = myModifier,
                onState = {
                    painterState.value = it
                }
            )
        }

        AddedImageFeatures(
            painterState,
            content,
            contentScale,
            myModifier,
            verifierModifier,
            showImage
        )
    }
}

@Composable
fun ImageUrlWithDownloadButton(url: String, showImage: MutableState<Boolean>) {
    val uri = LocalUriHandler.current

    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.onBackground

    val regularText = remember { SpanStyle(color = background) }
    val clickableTextStyle = remember { SpanStyle(color = primary) }

    val annotatedTermsString = remember {
        buildAnnotatedString {
            withStyle(clickableTextStyle) {
                pushStringAnnotation("routeToImage", "")
                append("$url ")
            }

            withStyle(clickableTextStyle) {
                pushStringAnnotation("routeToImage", "")
                appendInlineContent("inlineContent", "[icon]")
            }

            withStyle(regularText) {
                append(" ")
            }
        }
    }

    val inlineContent = mapOf("inlineContent" to InlineDownloadIcon(showImage))

    val pressIndicator = remember {
        Modifier.clickable {
            runCatching { uri.openUri(url) }
        }
    }

    Text(
        text = annotatedTermsString,
        modifier = pressIndicator,
        inlineContent = inlineContent
    )
}

@Composable
private fun InlineDownloadIcon(showImage: MutableState<Boolean>) =
    InlineTextContent(
        Placeholder(
            width =  17.sp,
            height =  17.sp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
        )
    ) {
        IconButton(
            modifier = Modifier.size(20.dp),
            onClick = { showImage.value = true }
        ) {
            Icon(Icons.Default.DownloadForOffline,contentDescription = "download for offline")
        }
    }

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AddedImageFeatures(
    painter: MutableState<AsyncImagePainter.State?>,
    content: ZoomableLocalImage,
    contentScale: ContentScale,
    myModifier: Modifier,
    verifiedModifier: Modifier,
    showImage: MutableState<Boolean>
) {
    if (!showImage.value) {
        ImageUrlWithDownloadButton(content.uri, showImage)
    } else {
        when (painter.value) {
            null, is AsyncImagePainter.State.Loading -> {
                if (content.blurhash != null) {
                    DisplayBlurHash(content.blurhash, content.description, contentScale, myModifier)
                } else {
                    FlowRow() {
                        DisplayUrlWithLoadingSymbol(content)
                    }
                }
            }

            is AsyncImagePainter.State.Error -> {
                Text("error")
            }


            else -> {
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AddedImageFeatures(
    painter: MutableState<AsyncImagePainter.State?>,
    content: ZoomableUrlImage,
    contentScale: ContentScale,
    myModifier: Modifier,
    verifiedModifier: Modifier,
    showImage: MutableState<Boolean>
) {
    if (!showImage.value) {
        ImageUrlWithDownloadButton(content.url, showImage)
    } else {
        var verifiedHash by remember {
            mutableStateOf<Boolean?>(null)
        }

        when (painter.value) {
            null, is AsyncImagePainter.State.Loading -> {
                if (content.blurhash != null) {
                    DisplayBlurHash(content.blurhash, content.description, contentScale, myModifier)
                } else {
                    FlowRow(Modifier.fillMaxWidth()) {
                        DisplayUrlWithLoadingSymbol(content)
                    }
                }
            }

            is AsyncImagePainter.State.Error -> {
                FlowRow(Modifier.fillMaxWidth()) {
                    ClickableUrl(urlText = "${content.url} ", url = content.url)
                }
            }

            is AsyncImagePainter.State.Success -> {

            }

            else -> {
            }
        }
    }
}

private fun aspectRatio(dim: String?): Float? {
    if (dim == null) return null

    val parts = dim.split("x")
    if (parts.size != 2) return null

    return try {
        val width = parts[0].toFloat()
        val height = parts[1].toFloat()
        width / height
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun DisplayUrlWithLoadingSymbol(content: ZoomableContent) {
    var cnt by remember { mutableStateOf<ZoomableContent?>(null) }

    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            delay(200)
            cnt = content
        }
    }

    cnt?.let { DisplayUrlWithLoadingSymbolWait(it) }
}

@Composable
private fun DisplayUrlWithLoadingSymbolWait(content: ZoomableContent) {
    val uri = LocalUriHandler.current

    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.onBackground

    val regularText = remember { SpanStyle(color = background) }
    val clickableTextStyle = remember { SpanStyle(color = primary) }

    val annotatedTermsString = remember {
        buildAnnotatedString {
            if (content is ZoomableUrlContent) {
                withStyle(clickableTextStyle) {
                    pushStringAnnotation("routeToImage", "")
                    append(content.url + " ")
                }
            } else {
                withStyle(regularText) {
                    append("Loading content...")
                }
            }

            withStyle(clickableTextStyle) {
                pushStringAnnotation("routeToImage", "")
                appendInlineContent("inlineContent", "[icon]")
            }

            withStyle(regularText) {
                append(" ")
            }
        }
    }

    val inlineContent = mapOf("inlineContent" to InlineLoadingIcon())

    val pressIndicator = remember {
        if (content is ZoomableUrlContent) {
            Modifier.clickable {
                runCatching { uri.openUri(content.url) }
            }
        } else {
            Modifier
        }
    }

    Text(
        text = annotatedTermsString,
        modifier = pressIndicator,
        inlineContent = inlineContent
    )
}

@Composable
private fun InlineLoadingIcon() =
    InlineTextContent(
        Placeholder(
            width = 17.sp,
            height = 17.sp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
        )
    ) {
        LoadingAnimation()
    }

@Composable
private fun DisplayBlurHash(
    blurhash: String?,
    description: String?,
    contentScale: ContentScale,
    modifier: Modifier
) {
    if (blurhash == null) return

    val context = LocalContext.current
    AsyncImage(
        model = remember {
            BlurHashRequester.imageRequest(
                context,
                blurhash
            )
        },
        contentDescription = description,
        contentScale = contentScale,
        modifier = modifier
    )
}

@Composable
fun ZoomableImageDialog(
    imageUrl: ZoomableContent,
    allImages: ImmutableList<ZoomableContent> = listOf(imageUrl).toImmutableList(),
    onDismiss: () -> Unit
) {
    val orientation = LocalConfiguration.current.orientation
    println("This Log only exists to force orientation listener $orientation")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        val orientation = LocalConfiguration.current.orientation
        println("This Log only exists to force orientation listener $orientation")

        val activityWindow = getActivityWindow()
        val dialogWindow = getDialogWindow()
        val parentView = LocalView.current.parent as View
        SideEffect {
            if (activityWindow != null && dialogWindow != null) {
                val attributes = WindowManager.LayoutParams()
                attributes.copyFrom(activityWindow.attributes)
                attributes.type = dialogWindow.attributes.type
                dialogWindow.attributes = attributes
                parentView.layoutParams = FrameLayout.LayoutParams(activityWindow.decorView.width, activityWindow.decorView.height)
                view.layoutParams = FrameLayout.LayoutParams(activityWindow.decorView.width, activityWindow.decorView.height)
            }
        }

        DisposableEffect(key1 = Unit) {
            if (Build.VERSION.SDK_INT >= 30) {
                view.windowInsetsController?.hide(
                    android.view.WindowInsets.Type.systemBars()
                )
            }

            onDispose {
                if (Build.VERSION.SDK_INT >= 30) {
                    view.windowInsetsController?.show(
                        android.view.WindowInsets.Type.systemBars()
                    )
                }
            }
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                DialogContent(allImages, imageUrl, onDismiss)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DialogContent(
    allImages: ImmutableList<ZoomableContent>,
    imageUrl: ZoomableContent,
    onDismiss: () -> Unit
) {
    val pagerState: PagerState = rememberPagerState() { allImages.size }
    val controllerVisible = remember { mutableStateOf(false) }
    val holdOn = remember { mutableStateOf<Boolean>(true) }

    LaunchedEffect(key1 = pagerState, key2 = imageUrl) {
        launch {
            val page = allImages.indexOf(imageUrl)
            if (page > -1) {
                pagerState.scrollToPage(page)
            }
        }
        launch(Dispatchers.Default) {
            delay(2000)
            holdOn.value = false
        }
    }

    if (allImages.size > 1) {
        SlidingCarousel(
            pagerState = pagerState
        ) { index ->
            RenderImageOrVideo(
                content = allImages[index],
                roundedCorner = false,
                topPaddingForControllers = 55.dp,
                onControllerVisibilityChanged = {
                    controllerVisible.value = it
                },
                onToggleControllerVisibility = {
                    controllerVisible.value = !controllerVisible.value
                }
            )
        }
    } else {
        RenderImageOrVideo(
            content = imageUrl,
            roundedCorner = false,
            topPaddingForControllers = 55.dp,
            onControllerVisibilityChanged = {
                controllerVisible.value = it
            },
            onToggleControllerVisibility = {
                controllerVisible.value = !controllerVisible.value
            }
        )
    }
}

@Composable
private fun CopyToClipboard(
    content: ZoomableContent
) {
    val popupExpanded = remember { mutableStateOf(false) }

    OutlinedButton(
        modifier = Modifier.padding(horizontal = 5.dp),
        onClick = { popupExpanded.value = true }
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            modifier = Modifier.size(20.dp),
            contentDescription = stringResource(R.string.copy_url_to_clipboard)
        )

    }
}



@Composable
private fun RenderImageOrVideo(
    content: ZoomableContent,
    roundedCorner: Boolean,
    topPaddingForControllers: Dp = Dp.Unspecified,
    onControllerVisibilityChanged: ((Boolean) -> Unit)? = null,
    onToggleControllerVisibility: (() -> Unit)? = null
) {
    if (content is ZoomableUrlImage) {
        val mainModifier = Modifier
            .fillMaxSize()
            .zoomable(
                rememberZoomState(),
                onTap = {
                    if (onToggleControllerVisibility != null) {
                        onToggleControllerVisibility()
                    }
                }
            )

        UrlImageView(
            content = content,
            mainImageModifier = mainModifier,
            topPaddingForControllers = topPaddingForControllers,
            alwayShowImage = true
        )
    } else if (content is ZoomableUrlVideo) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize(1f)) {
            VideoView(
                videoUri = content.url,
                title = content.description,
                artworkUri = content.artworkUri,
                authorName = content.authorName,
                roundedCorner = roundedCorner,
                topPaddingForControllers = topPaddingForControllers,
                onControllerVisibilityChanged = onControllerVisibilityChanged,
                alwaysShowVideo = true
            )
        }
    } else if (content is ZoomableLocalImage) {
        val mainModifier = Modifier
            .fillMaxSize()
            .zoomable(
                rememberZoomState(),
                onTap = {
                    if (onToggleControllerVisibility != null) {
                        onToggleControllerVisibility()
                    }
                }
            )

        LocalImageView(
            content = content,
            mainImageModifier = mainModifier,
            topPaddingForControllers = topPaddingForControllers,
            alwayShowImage = true
        )
    } else if (content is ZoomableLocalVideo) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize(1f)) {
            content.localFile?.let {
                VideoView(
                    videoUri = it.toUri().toString(),
                    title = content.description,
                    artworkUri = content.artworkUri,
                    authorName = content.authorName,
                    roundedCorner = roundedCorner,
                    topPaddingForControllers = topPaddingForControllers,
                    onControllerVisibilityChanged = onControllerVisibilityChanged,
                    alwaysShowVideo = true
                )
            }
        }
    }
}





// Window utils
@Composable
fun getDialogWindow(): Window? = (LocalView.current.parent as? DialogWindowProvider)?.window

@Composable
fun getActivityWindow(): Window? = LocalView.current.context.getActivityWindow()

private tailrec fun Context.getActivityWindow(): Window? =
    when (this) {
        is Activity -> window
        is ContextWrapper -> baseContext.getActivityWindow()
        else -> null
    }

@Composable
fun getActivity(): Activity? = LocalView.current.context.getActivity()

private tailrec fun Context.getActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.getActivity()
        else -> null
    }