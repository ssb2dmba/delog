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
import `in`.delog.db.AppDatabase
import `in`.delog.db.dao.*
import `in`.delog.db.model.Contact
import `in`.delog.db.model.Ident
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.io.Base64
import org.apache.tuweni.scuttlebutt.Identity
import org.apache.tuweni.scuttlebutt.Invite.Companion.fromCanonicalForm
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



fun initDefaultData(myIdentDao: IdentDao, contactDao: ContactDao) {
    if (myIdentDao.count() > 0) {
        return
    }
    println("creating default identity...")
    val b64key =
        "8CcQUI27IE+Rjj7sZ4Q9njjqcB0vizqstNYGVux/ehJilJsTn/uha5/uTrOFT/DBubbwR99SCBgTBWkOQ4B0iA==.ed25519"
    insertIdent(myIdentDao, "ssbpub", "192.168.0.40", "8008", b64key)

    val b64key2 =
        "ZUCJ2dS2+Wn7ByTYNenQUXxK8zxrpvg07doDoenRs232FW2bkAh9hcWnijbmw1huRxqWs6Oi+e4hqBKRzobCCQ==.ed25519"
    val ident = insertIdent(myIdentDao, "remote1", "192.168.0.40", "8008", b64key2)

    val b64key3 =
        "aE49ri2GSz67A1jS6tVA97kBRP9PdZNOhJPD5rYuSLQVRMHScr3yJwKLj6O/uJERYLMl6n1fdl2vEZcq4Qgpgg==.ed25519"
    val ident2 = insertIdent(myIdentDao, "remote2", "192.168.0.40", "8008", b64key3)

    val testInvite =
        "192.168.0.40:8008:@YpSbE5/7oWuf7k6zhU/wwbm28EffUggYEwVpDkOAdIg=.ed25519~QYdBi54tluAzqICLT03aOWwmfydVVjZGLW8lkt3QY64="
    val invite = fromCanonicalForm(testInvite)

    myIdentDao.setDefault(2)
    // add contact
    contactDao.insert(Contact(1, ident.publicKey, ident2.publicKey, true))

    contactDao.insert(Contact(2, ident2.publicKey, ident.publicKey, true))

    contactDao.insert(
        Contact(
            3,
            ident.publicKey,
            "@XVm2XGanhTuBPawgvjjAmbmAi3ENdJk4vGNFd+euT80=.ed25519",
            true
        )
    )
    contactDao.insert(
        Contact(
            4,
            ident2.publicKey,
            "@XVm2XGanhTuBPawgvjjAmbmAi3ENdJk4vGNFd+euT80=.ed25519",
            true
        )
    )
}

fun insertIdent(
    myIdentDao: IdentDao,
    name: String,
    server: String,
    port: String,
    b64key: String
): Ident {
    val privateKey = b64key.replace(".ed25519", "")
    val privKeyBytes = Base64.decode(privateKey)
    val secretKey = Signature.SecretKey.fromBytes(privKeyBytes)
    val ssbIdentity: Identity = Identity.fromSecretKey(secretKey)
    val feed = Ident(
        0,
        ssbIdentity.toCanonicalForm(),
        server,
        port.toInt(),
        ssbIdentity.privateKeyAsBase64String(),
        true,
        name,
        1,
        null
    );
    println("adding: " + ssbIdentity.toCanonicalForm())
    myIdentDao.insert(feed)
    return feed;
}


