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
package `in`.delog.db.repository

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.net.toUri
import `in`.delog.MainApplication
import `in`.delog.db.dao.BlobDao
import `in`.delog.db.model.Blob
import `in`.delog.model.Mention
import `in`.delog.service.ssb.SsbService.Companion.TAG
import `in`.delog.viewmodel.BlobItem
import org.apache.tika.Tika
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.sodium.SHA256Hash
import org.apache.tuweni.scuttlebutt.lib.BlobService.Companion.MAX_BLOB_SIZE
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream


interface BlobRepository {
    suspend fun deleteIfKeyUnused(key: String)
    suspend fun getAsBlobItem(b64hash: String): BlobItem
    suspend fun getBlobItem(b64hash: String): BlobItem?
    suspend fun getWants(author: String): HashMap<String, Long>
    suspend fun createWant(author: String, blob: Mention)
    suspend fun insertOwnBlob(author: String, uri: Uri): BlobItem?
    suspend fun update(author: String, uri: Uri): BlobItem?
    fun getTempFile(hash: String): File
}

class BlobRepositoryImpl(
    private val messageRepository: MessageRepository, private val blobDao: BlobDao
) : BlobRepository {

    val context = MainApplication.applicationContext()
    private val contentResolver: ContentResolver = this.context.contentResolver

    private fun getSize(uri: Uri): Long {
        var size: Long = -1L
        contentResolver.query(uri, null, null, null, null).use {
            if (it !== null && it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex > -1) size = it.getLong(sizeIndex)
            }
        }
        return size
    }

    private fun detectMime(inputStream: InputStream): String {
        //return "image/jpeg"
        val tika = Tika()
        return tika.detect(inputStream)
    }

    private fun ingestBlob(uri:Uri): BlobItem? {
        var mimeType = contentResolver.getType(uri)
        if (mimeType==null) {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream!=null) {
                mimeType = detectMime(inputStream)
                inputStream.close()
            }
        }
        val size = getSize(uri)
        if (size>MAX_BLOB_SIZE) {
            throw Exception("file is bigger than network ${MAX_BLOB_SIZE / 1024 /1024} Mo limit: (${size / 1024 / 1024} Mo)")
        }
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        if (inputStream == null || mimeType == null) {
            Log.e(TAG, "unable to open file input stream is null:$uri")
            return null
        }

        val hash = SHA256Hash.hash(SHA256Hash.Input.fromBytes(getBytes(inputStream)))
        inputStream.close()
        val b64hash = hash.bytes().toBase64String()
        val key = "&$b64hash.sha256"

        val hexHash = hash.bytes().toHexString().substring(2)
        val subdir = hexHash.substring(0, 2)
        val fileName = hexHash.substring(2)
        val externalFileDir = File(context.getExternalFilesDir("blobs"), subdir)
        externalFileDir.mkdirs()
        val outputFile = File(externalFileDir, fileName)
        val input: InputStream? = contentResolver.openInputStream(uri)
        input!!.copyTo(outputFile.outputStream())
        input.close()

        return BlobItem(key = key, size = size, type = mimeType, uri = uri)
    }
    override suspend fun insertOwnBlob(author: String, uri: Uri): BlobItem? {

        val blobItem = ingestBlob(uri) ?: return null
        if (blobDao.get(blobItem.key) != null) {
            // blob already exists in db
            return blobItem
        }
        val blob = Blob(
            oid = 0,
            author = author,
            key = blobItem.key,
            type = blobItem.type,
            size = blobItem.size,
            own = true,
            has = true,
            contentWarning = null
        )
        blobDao.insert(blob)
        return blobItem
    }


    override suspend fun update(author: String, uri: Uri): BlobItem? {
        val blobItem = ingestBlob(uri) ?: return null
        val existing = blobDao.get(blobItem.key)
        if (existing == null) {
            Log.w(TAG, "ingesting not wanted blob ? :${blobItem.key}")
            return null
        }
        existing.key = blobItem.key
        existing.has = true
        existing.size = blobItem.size
        existing.type = blobItem.type
        blobDao.update(existing)
        return blobItem
    }

    @Throws(IOException::class)
    fun getBytes(inputStream: InputStream): ByteArray? {
        val byteBuffer = ByteArrayOutputStream()
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)
        var len: Int
        while (inputStream.read(buffer).also { len = it } != -1) {
            byteBuffer.write(buffer, 0, len)
        }
        return byteBuffer.toByteArray()
    }



    private fun getUri(b64hash: String): Uri {
        return try {
            var simpleHash = b64hash
            if (b64hash.startsWith("&")) {
                simpleHash = b64hash.subSequence(1, b64hash.length - 7).toString()
            }
            val hexHash = Bytes.fromBase64String(simpleHash).toHexString().substring(2)
            val subdir = hexHash.substring(0, 2)
            val fileName = hexHash.substring(2)
            val externalFileDir = File(context.getExternalFilesDir("blobs"), subdir)
            File(externalFileDir, fileName).toUri()
        } catch (e: Exception) {
            Log.e(TAG, "unable to get uri for hash:$b64hash")
            Uri.EMPTY
        }
    }


    override suspend fun getAsBlobItem(b64hash: String): BlobItem {
        val uri = getUri(b64hash)
        val blob = blobDao.get(b64hash)
        if (blob == null) {
            Log.e(TAG, "blob not found in db:$b64hash")
            return BlobItem(key = b64hash, size = -1, type = "/", uri = Uri.EMPTY)
        }
        return BlobItem(key = b64hash, size = blob.size, type = blob.type ?: "", uri = uri)
    }

    override suspend fun getBlobItem(b64hash: String): BlobItem? {
        val uri = getUri(b64hash)
        val blob = blobDao.get(b64hash) ?: return null
        val blobItem = BlobItem(key = b64hash, size = blob.size, type = blob.type ?: "", uri = uri)
        val file = File(blobItem.uri.path!!)
        if (!file.exists()) return null
        return blobItem
    }


    override suspend fun deleteIfKeyUnused(key: String) {
        messageRepository.blobIsUsefull(key)
    }

    override suspend fun getWants(author: String): HashMap<String, Long> {
        val blobs = blobDao.getWants(author)
        val wants = HashMap<String, Long>()
        for (blob in blobs) {
            val blobItem = getAsBlobItem(blob.key)
            wants[blobItem.key] = -1
        }
        return wants

    }

    override suspend fun createWant(author: String, blob: Mention) {
        // TODO check if exists
        blobDao.get(blob.link)?.let {
            return
        }
        val want = Blob(
            oid = 0,
            author = author,
            key = blob.link,
            type = null,
            size = 0,
            own = false,
            has = false,
            contentWarning = null
        )
        blobDao.insert(want)
    }


    override fun getTempFile(hash: String): File {
        val hashAsBytes = Bytes.fromBase64String(hash.subSequence(1, hash.length - 7).toString())
        val hashed = SHA256Hash.Input.fromBytes(hashAsBytes)
        val hexhash = hashed.bytes().toHexString()
        val tmpFile =  File(context.cacheDir, hexhash)
        if (!tmpFile.exists()) tmpFile.createNewFile()
        return tmpFile
    }

}
