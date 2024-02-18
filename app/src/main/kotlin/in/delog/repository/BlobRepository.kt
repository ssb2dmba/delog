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
package `in`.delog.repository

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.net.toUri
import `in`.delog.MainApplication
import `in`.delog.db.dao.BlobDao
import `in`.delog.db.model.Blob
import `in`.delog.service.ssb.BaseSsbService.Companion.TAG
import `in`.delog.viewmodel.BlobItem
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.sodium.SHA256Hash
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream


interface BlobRepository {
    suspend fun insert(author: String, uri: Uri): BlobItem?
    suspend fun delete(hash: String)
    suspend fun deleteIfKeyUnused(key: String)
    suspend fun getAsBlobItem(b64hash: String): BlobItem
}

class BlobRepositoryImpl(
    private val messageRepository: MessageRepository, private val blobDao: BlobDao
) : BlobRepository {

    val context = MainApplication.applicationContext()
    val contentResolver = this.context.contentResolver

    fun getSize(uri: Uri): Long {
        var size: Long = -1L
        contentResolver.query(uri, null, null, null, null).use {
            if (it !== null && it?.moveToFirst() == true) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != null && sizeIndex > -1) size = it.getLong(sizeIndex)
            }
        }
        return size
    }

    override suspend fun insert(author: String, uri: Uri): BlobItem? {

        val mimeType = contentResolver.getType(uri)
        val size = getSize(uri)
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        if (inputStream == null || mimeType == null) {
            Log.e(TAG, "unable to open file input stream is null:" + uri)
            return null
        }
        val hash = SHA256Hash.hash(SHA256Hash.Input.fromBytes(getBytes(inputStream)));
        inputStream.close()

        val b64hash = hash.bytes().toBase64String()
        val hexHash = hash.bytes().toHexString().substring(2)
        val subdir = hexHash.substring(0, 2)
        val fileName = hexHash.substring(2)
        val externalFileDir = File(context.getExternalFilesDir("blobs"), subdir)
        externalFileDir.mkdirs()
        var outputFile = File(externalFileDir, fileName)
        val input: InputStream? = contentResolver.openInputStream(uri)
        input!!.copyTo(outputFile.outputStream())
        input.close()
        val key = "&" + b64hash + ".sha256"
        val blob = Blob(
            oid = 0,
            author = author,
            key = key,
            type = mimeType,
            size = size,
            own = true,
            want = false,
            contentWarning = null
        )
        blobDao.insert(blob)
        return BlobItem(key = key, size = size!!, type = mimeType!!, uri = outputFile.toUri())
    }

    @Throws(IOException::class)
    fun getBytes(inputStream: InputStream): ByteArray? {
        val byteBuffer = ByteArrayOutputStream()
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)
        var len = 0
        while (inputStream.read(buffer).also { len = it } != -1) {
            byteBuffer.write(buffer, 0, len)
        }
        return byteBuffer.toByteArray()
    }

    override suspend fun delete(hash: String) {

    }

    fun getUri(b64hash: String): Uri {
        var simpleHash = b64hash
        if (b64hash.startsWith("&")) {
            simpleHash = b64hash.subSequence(1, b64hash.length - 7).toString()
        }
        val hexHash = Bytes.fromBase64String(simpleHash).toHexString().substring(2)
        val subdir = hexHash.substring(0, 2)
        val fileName = hexHash.substring(2)
        val externalFileDir= File (context.getExternalFilesDir("blobs"), subdir)
        return File(externalFileDir, fileName).toUri()
    }


    override suspend fun getAsBlobItem(b64hash: String): BlobItem {
        val uri = getUri(b64hash)
        val blob = blobDao.get(b64hash)
        if (blob == null) {
            Log.e(TAG, "blob not found in db:" + b64hash)
            return BlobItem(key = b64hash, size = -1, type = "/", uri = Uri.EMPTY)
        }
        return BlobItem(key = b64hash, size = blob.size, type = blob.type, uri = uri)
    }

    override suspend fun deleteIfKeyUnused(key: String) {
        messageRepository.blobIsUsefull(key)
    }

}
