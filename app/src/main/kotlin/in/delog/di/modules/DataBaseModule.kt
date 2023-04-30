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
package `in`.delog.di.modules

import android.app.Application
import android.os.AsyncTask
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import `in`.delog.db.dao.AboutDao
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.io.Base64
import org.apache.tuweni.scuttlebutt.Identity
import org.apache.tuweni.scuttlebutt.Invite.Companion.fromCanonicalForm
import `in`.delog.db.AppDatabase
import `in`.delog.db.dao.*
import `in`.delog.db.model.Contact
import `in`.delog.db.model.Ident
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module


val dataBaseModule = module(createdAtStart = true) {

    lateinit var appDatabase: AppDatabase

    fun provideDataBase(application: Application): AppDatabase {
        appDatabase = Room.databaseBuilder(application, AppDatabase::class.java, "dlog")
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    doAsync {
                        //initDefaultData(appDatabase.identDao(), appDatabase.contactDao())
                    }
                }
            })
            .fallbackToDestructiveMigration()
            .build()
        return appDatabase
    }

    fun provideFeedDao(dataBase: AppDatabase): IdentDao {
        return dataBase.identDao()
    }

    fun provideMessageDao(dataBase: AppDatabase): MessageDao {
        return dataBase.messageDao()
    }

    fun provideDraftDao(dataBase: AppDatabase): DraftDao {
        return dataBase.draftDao()
    }

    fun provideContactDao(dataBase: AppDatabase): ContactDao {
        return dataBase.contactDao()
    }

    fun provideAuthorInfoDao(dataBase: AppDatabase): AboutDao {
        return dataBase.authorInfoDao()
    }

    single { provideDataBase(androidApplication()) }
    single { provideFeedDao(get()) }
    single { provideMessageDao(get()) }
    single { provideDraftDao(get()) }
    single { provideContactDao(get()) }
    single { provideAuthorInfoDao(get()) }
}

class doAsync(val handler: () -> Unit) : AsyncTask<Void, Void, Void>() {
    init {
        execute()
    }

    override fun doInBackground(vararg params: Void?): Void? {
        handler()
        return null
    }
}

