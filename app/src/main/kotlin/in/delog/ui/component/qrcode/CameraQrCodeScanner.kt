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
package `in`.delog.ui

import android.Manifest
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.common.util.concurrent.ListenableFuture
import `in`.delog.ui.component.qrcode.QrCodeAnalyzer
import java.util.concurrent.Executors

private val REQUIRED_PERMISSIONS = listOf(Manifest.permission.CAMERA)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraQrCodeScanner(callback: (String) -> Unit) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionState = rememberMultiplePermissionsState(REQUIRED_PERMISSIONS)

    lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    val cameraExecutor = Executors.newSingleThreadExecutor()

    val previewView: PreviewView = remember { PreviewView(context) }

    fun startCamera(cameraProvider: ProcessCameraProvider) {
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val preview = Preview.Builder().apply {
            setTargetResolution(Size(previewView.width, previewView.height))
        }.build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(previewView.width, previewView.height))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, QrCodeAnalyzer { qrResult ->
                    cameraExecutor.shutdown()
                    callback(qrResult.text)
                })
            }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }


    LaunchedEffect(Unit) {

        permissionState.launchMultiplePermissionRequest()
    }


    LaunchedEffect(previewView) {
        cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            startCamera(cameraProvider)
        }, ContextCompat.getMainExecutor(context))
    }


    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
    }

}
