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
package `in`.delog.ui.component.qrcode

import android.graphics.ImageFormat.YUV_420_888
import android.graphics.ImageFormat.YUV_422_888
import android.graphics.ImageFormat.YUV_444_888
import android.os.Build
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer


private fun ByteBuffer.toByteArray(): ByteArray {
    rewind()
    val data = ByteArray(remaining())
    get(data)
    return data
}

class QrCodeAnalyzer(
    private val onQrCodesDetected: (qrCode: Result) -> Unit
) : ImageAnalysis.Analyzer {

    private val yuvFormats = mutableListOf(YUV_420_888)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            yuvFormats.addAll(listOf(YUV_422_888, YUV_444_888))
        }
    }

    private val reader = MultiFormatReader().apply {
        val map = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to arrayListOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to true
        )
        setHints(map)

    }

    override fun analyze(image: ImageProxy) {
        // We are using YUV format because, ImageProxy internally uses ImageReader to get the image
        // by default ImageReader uses YUV format unless changed.
        if (image.format !in yuvFormats) {
            Log.e("QRCodeAnalyzer", "Expected YUV, now = ${image.format}")
            return
        }

        val data = image.planes[0].buffer.toByteArray()

        val source = PlanarYUVLuminanceSource(
            data,
            image.width,
            image.height,
            0,
            0,
            image.width,
            image.height,
            false
        )

        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        try {
            // Whenever reader fails to detect a QR code in image
            // it throws NotFoundException
            val result = reader.decode(binaryBitmap)
            onQrCodesDetected(result)
        } catch (e: NotFoundException) {
            // pass
        }
        image.close()
    }
}

