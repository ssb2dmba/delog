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

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import `in`.delog.di.modules.dataBaseModule
import `in`.delog.di.modules.mainViewModel
import `in`.delog.di.modules.ssbModule
import `in`.delog.libsodium.NaCl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MainApplication : Application() {

    val applicationScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    val context = this
    override fun onCreate() {
        super.onCreate()
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

        fun getApplicationScope():  CoroutineScope {
            return instance!!.applicationScope
        }

        fun toastify(message: String) {

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(instance!!.context, message, Toast.LENGTH_LONG).show()
            }
        }

    }



}







