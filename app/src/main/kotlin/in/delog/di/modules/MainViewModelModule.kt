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

import `in`.delog.db.AppDatabase
import `in`.delog.repository.AboutRepository
import `in`.delog.repository.AboutRepositoryImpl
import `in`.delog.repository.ContactRepository
import `in`.delog.repository.ContactRepositoryImpl
import `in`.delog.repository.DidRepository
import `in`.delog.repository.DidRepositoryImpl
import `in`.delog.repository.DraftRepository
import `in`.delog.repository.DraftRepositoryImpl
import `in`.delog.repository.FeedRepositoryImpl
import `in`.delog.repository.IdentRepository
import `in`.delog.repository.MessageRepository
import `in`.delog.repository.MessageRepositoryImpl
import `in`.delog.repository.MessageTreeRepository
import `in`.delog.repository.MessageTreeRepositoryImpl
import `in`.delog.viewmodel.BottomBarViewModel
import `in`.delog.viewmodel.ContactListViewModel
import `in`.delog.viewmodel.DraftListViewModel
import `in`.delog.viewmodel.DraftViewModel
import `in`.delog.viewmodel.IdentAndAboutViewModel
import `in`.delog.viewmodel.IdentListViewModel
import `in`.delog.viewmodel.MessageListViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val mainViewModel = module {

    single {
        val database = get<AppDatabase>()
        database.identDao()
    }

    single { FeedRepositoryImpl(get(), get()) }
    factory<IdentRepository> { (FeedRepositoryImpl(get(), get())) }
    single { IdentListViewModel(get(), get()) }
    viewModel { IdentAndAboutViewModel(get(), get(), get(), get(), get()) }
    single { MessageRepositoryImpl(get()) }
    factory<MessageRepository> { (MessageRepositoryImpl(get())) }
    factory<MessageTreeRepository> { (MessageTreeRepositoryImpl(get())) }
    viewModel { MessageListViewModel(get(), get(), get(), get()) }

    factory<DraftRepository> { (DraftRepositoryImpl(get())) }
    viewModel { DraftListViewModel(get(), get()) }
    viewModel { ContactListViewModel(get(), get()) }

    single { ContactRepositoryImpl(get()) }
    factory<ContactRepository> { (ContactRepositoryImpl(get())) }

    single { AboutRepositoryImpl(get()) }
    factory<AboutRepository> { (AboutRepositoryImpl(get())) }

    single { DidRepositoryImpl() }
    factory<DidRepository> { (DidRepositoryImpl()) }

    viewModel { DraftViewModel(get(), get(), get(), get(), get(), get()) }
    single { BottomBarViewModel() }
}
