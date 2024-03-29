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
package `in`.delog.db

import androidx.room.Database
import androidx.room.RoomDatabase
import `in`.delog.db.dao.AboutDao
import `in`.delog.db.dao.BlobDao
import `in`.delog.db.dao.ContactDao
import `in`.delog.db.dao.DraftDao
import `in`.delog.db.dao.IdentDao
import `in`.delog.db.dao.MessageDao
import `in`.delog.db.dao.MessageTreeDao
import `in`.delog.db.dao.RelayDao
import `in`.delog.db.model.About
import `in`.delog.db.model.Blob
import `in`.delog.db.model.Contact
import `in`.delog.db.model.Draft
import `in`.delog.db.model.Ident
import `in`.delog.db.model.Message
import `in`.delog.db.model.RelayServer

@Database(
    entities = [
        Ident::class,
        Message::class,
        Draft::class,
        Contact::class,
        About::class,
        RelayServer::class,
        Blob::class,
    ],
    views = [
        AppDatabaseView.MessageInTree::class
    ],
    version = 12
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun identDao(): IdentDao
    abstract fun messageDao(): MessageDao
    abstract fun messageTreeDao(): MessageTreeDao
    abstract fun draftDao(): DraftDao
    abstract fun contactDao(): ContactDao
    abstract fun authorInfoDao(): AboutDao
    abstract fun pubServerDao(): RelayDao
    abstract fun wantDao(): BlobDao
}