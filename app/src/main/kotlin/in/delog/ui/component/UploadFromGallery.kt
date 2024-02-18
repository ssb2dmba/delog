package `in`.delog.ui.component

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import `in`.delog.GetMediaActivityResultContract
import `in`.delog.R
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun UploadFromGallery(
        isUploading: Boolean,
        tint: Color,
        modifier: Modifier,
        onImageChosen: (Uri) -> Unit,
) {
    val cameraPermissionState =
            rememberPermissionState(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        android.Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    },
            )

    if (cameraPermissionState.status.isGranted) {
        var showGallerySelect by remember { mutableStateOf(false) }
        if (showGallerySelect) {
            GallerySelect(
                    onImageUri = { uri ->
                        showGallerySelect = false
                        if (uri != null) {
                            onImageChosen(uri)
                        }
                    },
            )
        }
        UploadBoxButton(isUploading, tint, modifier) { showGallerySelect = true }
    } else {
        UploadBoxButton(isUploading, tint, modifier) { cameraPermissionState.launchPermissionRequest() }
    }
}

@Composable
private fun UploadBoxButton(
        isUploading: Boolean,
        tint: Color,
        modifier: Modifier,
        onClick: () -> Unit,
) {
    Box {
        IconButton(
                modifier = modifier.align(Alignment.Center),
                enabled = !isUploading,
                onClick = { onClick() },
        ) {
            if (!isUploading) {
                Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = stringResource(id = R.string.upload_image),
                        modifier = Modifier.height(25.dp),
                        tint = tint,
                )
            } else {
                LoadingAnimation()
            }
        }
    }
}


@Composable
fun GallerySelect(onImageUri: (Uri?) -> Unit = {}) {
    var hasLaunched by remember { mutableStateOf(AtomicBoolean(false)) }
    val launcher =
            rememberLauncherForActivityResult(
                    contract = GetMediaActivityResultContract(),
                    onResult = { uri: Uri? ->
                        onImageUri(uri)
                        hasLaunched.set(false)
                    },
            )

    @Composable
    fun LaunchGallery() {
        SideEffect {
            if (!hasLaunched.getAndSet(true)) {
                launcher.launch("*/*")
            }
        }
    }

    LaunchGallery()
}
