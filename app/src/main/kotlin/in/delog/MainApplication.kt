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
package `in`.delog

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import `in`.delog.di.modules.dataBaseModule
import `in`.delog.di.modules.mainViewModel
import `in`.delog.di.modules.ssbModule
import `in`.delog.libsodium.NaCl
import `in`.delog.service.ssb.TorService
import `in`.delog.ui.component.preview.videos.VideoCache
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MainApplication : Application() {

    lateinit var torService: TorService
    val context = this
    override fun onCreate() {
        super.onCreate()
        torService = TorService(this)
        NaCl.sodium()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MainApplication)
            modules(
                dataBaseModule,
                ssbModule,
                mainViewModel
            )
        }
    }


    init {
        instance = this
    }

    companion object {
        private var instance: MainApplication? = null

        fun applicationContext(): Context {
            return instance!!.applicationContext
        }


        fun getTorService(): TorService {
            return instance!!.torService
        }

        fun toastify(message: String) {

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(instance!!.context, message, Toast.LENGTH_LONG).show()
            }
        }

    }

    val videoCache: VideoCache by lazy {
        val newCache = VideoCache()
        newCache.initFileCache(this)
        newCache
    }


}

class GetMediaActivityResultContract : ActivityResultContracts.GetMultipleContents() {
    @SuppressLint("MissingSuperCall")
    override fun createIntent(
            context: Context,
            input: String,
    ): Intent {
        // Force OPEN Document because of the resulting URI must be passed to the
        // Playback service and the picker's permissions only allow the activity to read the URI
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            // Force only images and videos to be selectable
            type = input
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(input))
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
    }
}





